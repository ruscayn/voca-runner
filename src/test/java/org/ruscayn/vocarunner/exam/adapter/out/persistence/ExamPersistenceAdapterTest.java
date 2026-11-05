package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.answerOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.incorrectOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.questions;
import static org.ruscayn.vocarunner.support.TestWords.selected;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.adapter.out.persistence.JpaConfig;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 시험 저장소를 실제 PostgreSQL 로 확인한다 (U21).
 * 테스트 동안만 Docker 로 임시 PostgreSQL 을 띄워 표를 만든다 (db/schema.sql, 단어 데이터 없음. 개발 DB 는 건드리지 않음).
 *
 * <p>앱의 서비스처럼 저장소 호출마다 따로 커밋되게 테스트 트랜잭션을 끈다. 그래서 테스트마다 표를 비운다.
 */
@DataJpaTest
@Testcontainers
@Import({ExamPersistenceAdapter.class, JpaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("ExamPersistenceAdapter : 시험 저장 (PostgreSQL)")
class ExamPersistenceAdapterTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withInitScript("db/schema.sql");

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final Duration GRACE = Duration.ofMinutes(3);
    private static final Duration RETENTION = Duration.ofHours(1);
    private static final Duration UNDELIVERED = Duration.ofMinutes(2);

    @Autowired
    private ExamPersistenceAdapter repository;

    @Autowired
    private SpringDataExamRepository springDataRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clear() {
        springDataRepository.deleteAllInBatch();
    }

    /** 문항 4개(마감 = 시작 + 4분), 진행 중 */
    private static Exam exam(String deviceId, String ip, Instant startedAt) {
        return Exam.start(Level.A1, questions(4), new Examinee(deviceId, ip), startedAt, GRACE);
    }

    private Exam saved(Exam exam) {
        repository.save(exam);
        return exam;
    }

    private Exam load(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    @Nested
    @DisplayName("저장·불러오기")
    class SaveAndLoad {

        @Test
        @DisplayName("진행 중 시험을 저장했다 불러오면 보기·정답·기기·마감이 그대로다")
        void inProgress() {
            Exam exam = saved(exam("device-1", "10.0.0.1", NOW));

            Exam loaded = load(exam.getId());

            assertThat(loaded.getStatus()).isEqualTo(ExamStatus.IN_PROGRESS);
            assertThat(loaded.getLevel()).isEqualTo(Level.A1);
            assertThat(loaded.getQuestionCount()).isEqualTo(4);
            assertThat(loaded.getTime().startedAt()).isEqualTo(NOW);
            assertThat(loaded.getTime().deadline()).isEqualTo(exam.getTime().deadline());
            assertThat(loaded.getExaminee()).isEqualTo(new Examinee("device-1", "10.0.0.1"));
            assertThat(jdbcTemplate.queryForObject("select created_at is not null from test_session"
                    + " where test_session_id = ?", Boolean.class, exam.getId())).isTrue();
            for (int no = 1; no <= 4; no++) {
                assertThat(loaded.getQuestions().optionIds(no)).isEqualTo(exam.getQuestions().optionIds(no));
                assertThat(loaded.getQuestions().answerOptionId(no)).isEqualTo(exam.getQuestions().answerOptionId(no));
            }
        }

        @Test
        @DisplayName("제출한 시험을 다시 불러오면 선택한 보기·점수가 그대로다 (시간 초과 = 선택한 보기 없음)")
        void completed() {
            Exam exam = saved(exam("device-1", "10.0.0.1", NOW));
            Exam inProgress = load(exam.getId());
            inProgress.submit(selected(Map.of(1, answerOptionNo(1), 2, incorrectOptionNo(2), 3, answerOptionNo(3))),
                    NOW.plusSeconds(30));
            repository.save(inProgress);

            Exam loaded = load(exam.getId());

            assertThat(loaded.getStatus()).isEqualTo(ExamStatus.COMPLETED);
            assertThat(loaded.getGradingResult().correctCount()).isEqualTo(2);
            assertThat(loaded.getGradingResult().score()).isEqualTo(50);
            assertThat(loaded.getTime().submittedAt()).isEqualTo(NOW.plusSeconds(30));
            assertThat(loaded.getWrongQuestionNos()).containsExactly(2, 4);
            assertThat(loaded.selectedOptionId(2)).isEqualTo(
                    exam.getQuestions().optionIds(2).get(incorrectOptionNo(2) - 1));
            assertThat(loaded.selectedOptionId(4)).isNull();
        }

        @Test
        @DisplayName("없는 시험은 비어 있다")
        void notFound() {
            assertThat(repository.findById(UUID.randomUUID())).isEmpty();
        }

    }

    @Test
    @DisplayName("같은 시험을 동시에 두 번 제출하면 먼저 저장한 것만 반영되고 나머지는 거절된다")
    void submitTwiceAtOnce() {
        Exam exam = saved(exam("device-1", "10.0.0.1", NOW));
        // 두 요청이 같은 시점에 같은 시험을 읽음
        Exam first = load(exam.getId());
        Exam second = load(exam.getId());

        first.submit(selected(Map.of(1, answerOptionNo(1))), NOW.plusSeconds(10));
        repository.save(first);
        second.submit(selected(Map.of(1, answerOptionNo(1), 2, answerOptionNo(2))), NOW.plusSeconds(11));

        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        Exam loaded = load(exam.getId());
        assertThat(loaded.getGradingResult().correctCount()).isEqualTo(1);
        assertThat(loaded.getTime().submittedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Nested
    @DisplayName("진행 중 시험 세기 (마감 시각까지 진행 중)")
    class InProgress {

        @Test
        @DisplayName("진행 중이고 마감 전인 시험만 센다 (제출·마감 지남 제외)")
        void count() {
            saved(exam("device-1", "10.0.0.1", NOW));
            Exam completed = exam("device-2", "10.0.0.1", NOW);
            completed.submit(List.of(), NOW.plusSeconds(10));
            saved(completed);
            saved(exam("device-4", "10.0.0.1", NOW.minus(Duration.ofHours(1))));   // 마감 지남

            assertThat(repository.countInProgress(NOW.plusSeconds(20))).isEqualTo(1);
        }

        @Test
        @DisplayName("마감 시각 정각까지는 진행 중, 1초 지나면 아니다 (Exam.isInProgress 와 같음)")
        void deadlineBoundary() {
            Exam exam = saved(exam("device-1", "10.0.0.1", NOW));
            Instant deadline = exam.getTime().deadline();

            assertThat(repository.countInProgress(deadline)).isEqualTo(1);
            assertThat(exam.isInProgress(deadline)).isTrue();
            assertThat(repository.countInProgress(deadline.plusSeconds(1))).isZero();
            assertThat(exam.isInProgress(deadline.plusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("기기별 진행 중 시험은 그 기기 것만 찾는다")
        void byDevice() {
            Exam mine = saved(exam("device-1", "10.0.0.1", NOW));
            saved(exam("device-2", "10.0.0.1", NOW));

            List<Exam> found = repository.findInProgressByDevice("device-1", NOW);

            assertThat(found).extracting(Exam::getId).containsExactly(mine.getId());
        }

        @Test
        @DisplayName("IP별 진행 중 시험은 그 IP 것만 센다")
        void byIp() {
            saved(exam("device-1", "10.0.0.1", NOW));
            saved(exam("device-2", "10.0.0.1", NOW));
            saved(exam("device-3", "10.0.0.2", NOW));

            assertThat(repository.countInProgressByIp("10.0.0.1", NOW)).isEqualTo(2);
            assertThat(repository.countInProgressByIp("10.0.0.3", NOW)).isZero();
        }

    }

    @Test
    @DisplayName("정리 작업은 마감이 지난 시험 · 문제를 받아 가지 않은 시험 · 보관이 끝난 결과만 지운다")
    void deleteDisposable() {
        Instant now = NOW.plus(Duration.ofHours(3));
        // 남아야 하는 시험
        Exam delivered = exam("d1", "10.0.0.1", now.minus(Duration.ofMinutes(3)));
        delivered.deliverQuestions(now);
        Exam recentResult = exam("d3", "10.0.0.1", now.minus(Duration.ofMinutes(10)));
        recentResult.submit(List.of(), now.minus(Duration.ofMinutes(9)));                            // 결과 보관 중
        // CHECKSTYLE.SUPPRESS: VariableDeclarationUsageDistance — 남을 시험 · 지울 시험을 묶어 만들고 마지막에 한꺼번에 저장
        Exam justStarted = exam("d2", "10.0.0.1", now.minus(Duration.ofMinutes(1)));     // 아직 문제를 받기 전
        // 지워야 하는 시험
        Exam pastDeadline = exam("d4", "10.0.0.1", now.minus(Duration.ofMinutes(30)));
        pastDeadline.deliverQuestions(pastDeadline.getTime().startedAt());
        Exam undelivered = exam("d5", "10.0.0.1", now.minus(Duration.ofMinutes(3)));      // 2분 넘게 문제를 안 받아 감
        Exam oldResult = exam("d6", "10.0.0.1", now.minus(Duration.ofHours(3)));
        oldResult.submit(List.of(), now.minus(Duration.ofHours(3)).plusSeconds(30));                 // 보관 1시간 지남

        List<Exam> all = List.of(delivered, justStarted, recentResult, pastDeadline, undelivered, oldResult);
        all.forEach(repository::save);

        int deleted = repository.deleteDisposable(now, RETENTION, UNDELIVERED);

        assertThat(deleted).isEqualTo(3);
        assertThat(springDataRepository.findAll()).extracting(ExamJpaEntity::getId)
                .containsExactlyInAnyOrder(delivered.getId(), justStarted.getId(), recentResult.getId());
    }

    @Test
    @DisplayName("시작 잠금을 잡은 동안 다른 연결은 잠금을 못 잡고, 트랜잭션이 끝나면 풀린다")
    void lockForStart() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            repository.lockForStart();
            // 다른 스레드 = 다른 DB 연결 (같은 연결은 같은 잠금을 다시 잡을 수 있어 확인이 안 됨)
            boolean otherGot = CompletableFuture.supplyAsync(this::tryStartLock).join();
            assertThat(otherGot).isFalse();
        });

        assertThat(tryStartLock()).isTrue();
    }

    /** 새 트랜잭션에서 시작 잠금을 기다리지 않고 잡아 본다 */
    private boolean tryStartLock() {
        return Boolean.TRUE.equals(new TransactionTemplate(transactionManager).execute(status ->
                jdbcTemplate.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class,
                        SpringDataExamRepository.START_LOCK_KEY)));
    }

}

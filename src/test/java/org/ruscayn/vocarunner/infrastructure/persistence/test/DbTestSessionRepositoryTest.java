package org.ruscayn.vocarunner.infrastructure.persistence.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.test.Direction;
import org.ruscayn.vocarunner.domain.model.test.TestClient;
import org.ruscayn.vocarunner.domain.model.test.TestSession;
import org.ruscayn.vocarunner.domain.model.test.TestStatus;
import org.ruscayn.vocarunner.infrastructure.config.JpaConfig;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.correctOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.questions;
import static org.ruscayn.vocarunner.support.TestWords.wrongOptionNo;

/**
 * 시험 저장소를 실제 PostgreSQL 로 확인한다 (U21).
 * 테스트 동안만 Docker 로 임시 PostgreSQL 을 띄워 표를 만든다 (db/schema.sql, 단어 데이터 없음. 개발 DB 는 건드리지 않음).
 *
 * <p>앱의 서비스처럼 저장소 호출마다 따로 커밋되게 테스트 트랜잭션을 끈다. 그래서 테스트마다 표를 비운다.
 */
@DataJpaTest
@Testcontainers
@Import({DbTestSessionRepository.class, JpaConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("DbTestSessionRepository : 시험 저장 (PostgreSQL)")
class DbTestSessionRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18")
            .withInitScript("db/schema.sql");

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final Duration GRACE = Duration.ofMinutes(3);
    private static final Duration RETENTION = Duration.ofHours(1);
    private static final Duration UNDELIVERED = Duration.ofMinutes(5);

    @Autowired
    private DbTestSessionRepository repository;

    @Autowired
    private JpaTestSessionRepository jpa;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clear() {
        jpa.deleteAllInBatch();
    }

    /** 문항 4개(마감 = 시작 + 4분), 진행 중 */
    private static TestSession session(String deviceId, String ip, Instant startedAt) {
        return TestSession.start(Level.A1, Direction.EN_KO, 5, questions(4),
                new TestClient(deviceId, ip), startedAt, GRACE);
    }

    private TestSession saved(TestSession session) {
        repository.save(session);
        return session;
    }

    private TestSession load(UUID id) {
        return repository.findById(id).orElseThrow();
    }

    @Nested
    @DisplayName("저장·불러오기")
    class SaveAndLoad {

        @Test
        @DisplayName("진행 중 시험을 저장했다 불러오면 보기·정답·기기·마감이 그대로다")
        void inProgress() {
            TestSession session = saved(session("device-1", "10.0.0.1", NOW));

            TestSession loaded = load(session.getId());

            assertThat(loaded.getStatus()).isEqualTo(TestStatus.IN_PROGRESS);
            assertThat(loaded.getLevel()).isEqualTo(Level.A1);
            assertThat(loaded.getQuestionCount()).isEqualTo(4);
            assertThat(loaded.getStartedAt()).isEqualTo(NOW);
            assertThat(loaded.getDeadline()).isEqualTo(session.getDeadline());
            assertThat(loaded.getClient()).isEqualTo(new TestClient("device-1", "10.0.0.1"));
            assertThat(loaded.getCreatedAt()).isNotNull();
            for (int no = 1; no <= 4; no++) {
                assertThat(loaded.optionLevelWordIds(no)).isEqualTo(session.optionLevelWordIds(no));
                assertThat(loaded.answerLevelWordId(no)).isEqualTo(session.answerLevelWordId(no));
            }
        }

        @Test
        @DisplayName("제출한 시험을 다시 불러오면 고른 답·점수가 그대로다 (시간 초과 = 고른 답 없음)")
        void completed() {
            TestSession session = saved(session("device-1", "10.0.0.1", NOW));
            TestSession inProgress = load(session.getId());
            inProgress.submit(Map.of(1, correctOptionNo(1), 2, wrongOptionNo(2), 3, correctOptionNo(3)),
                    NOW.plusSeconds(30));
            repository.save(inProgress);

            TestSession loaded = load(session.getId());

            assertThat(loaded.getStatus()).isEqualTo(TestStatus.COMPLETED);
            assertThat(loaded.getCorrectCount()).isEqualTo(2);
            assertThat(loaded.getScore()).isEqualTo(50);
            assertThat(loaded.getSubmittedAt()).isEqualTo(NOW.plusSeconds(30));
            assertThat(loaded.isCorrect(1)).isTrue();
            assertThat(loaded.isCorrect(2)).isFalse();
            assertThat(loaded.selectedLevelWordId(2)).isEqualTo(session.optionLevelWordIds(2).get(wrongOptionNo(2) - 1));
            assertThat(loaded.selectedLevelWordId(4)).isNull();
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
        TestSession session = saved(session("device-1", "10.0.0.1", NOW));
        // 두 요청이 같은 시점에 같은 시험을 읽음
        TestSession first = load(session.getId());
        TestSession second = load(session.getId());

        first.submit(Map.of(1, correctOptionNo(1)), NOW.plusSeconds(10));
        repository.save(first);
        second.submit(Map.of(1, correctOptionNo(1), 2, correctOptionNo(2)), NOW.plusSeconds(11));

        assertThatThrownBy(() -> repository.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        TestSession loaded = load(session.getId());
        assertThat(loaded.getCorrectCount()).isEqualTo(1);
        assertThat(loaded.getSubmittedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Nested
    @DisplayName("진행 중 시험 세기 (마감 시각까지 진행 중)")
    class InProgress {

        @Test
        @DisplayName("진행 중이고 마감 전인 시험만 센다 (제출·무효·마감 지남 제외)")
        void count() {
            saved(session("device-1", "10.0.0.1", NOW));
            TestSession completed = session("device-2", "10.0.0.1", NOW);
            completed.submit(Map.of(), NOW.plusSeconds(10));
            saved(completed);
            TestSession expired = session("device-3", "10.0.0.1", NOW);
            expired.expire();
            saved(expired);
            saved(session("device-4", "10.0.0.1", NOW.minus(Duration.ofHours(1))));   // 마감 지남

            assertThat(repository.countInProgress(NOW.plusSeconds(20))).isEqualTo(1);
        }

        @Test
        @DisplayName("마감 시각 정각까지는 진행 중, 1초 지나면 아니다 (TestSession.isInProgress 와 같음)")
        void deadlineBoundary() {
            TestSession session = saved(session("device-1", "10.0.0.1", NOW));
            Instant deadline = session.getDeadline();

            assertThat(repository.countInProgress(deadline)).isEqualTo(1);
            assertThat(session.isInProgress(deadline)).isTrue();
            assertThat(repository.countInProgress(deadline.plusSeconds(1))).isZero();
            assertThat(session.isInProgress(deadline.plusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("기기별 진행 중 시험은 그 기기 것만 찾는다")
        void byDevice() {
            TestSession mine = saved(session("device-1", "10.0.0.1", NOW));
            saved(session("device-2", "10.0.0.1", NOW));

            List<TestSession> found = repository.findInProgressByDevice("device-1", NOW);

            assertThat(found).extracting(TestSession::getId).containsExactly(mine.getId());
        }

        @Test
        @DisplayName("IP별 진행 중 시험은 그 IP 것만 센다")
        void byIp() {
            saved(session("device-1", "10.0.0.1", NOW));
            saved(session("device-2", "10.0.0.1", NOW));
            saved(session("device-3", "10.0.0.2", NOW));

            assertThat(repository.countInProgressByIp("10.0.0.1", NOW)).isEqualTo(2);
            assertThat(repository.countInProgressByIp("10.0.0.3", NOW)).isZero();
        }
    }

    @Test
    @DisplayName("정리 작업은 지울 시험만 지운다 (TestSession.isDisposable 과 같은 조건)")
    void deleteDisposable() {
        Instant now = NOW.plus(Duration.ofHours(3));
        // 남아야 하는 시험
        TestSession delivered = session("d1", "10.0.0.1", now.minus(Duration.ofMinutes(3)));
        delivered.markQuestionsDelivered();
        TestSession justStarted = session("d2", "10.0.0.1", now.minus(Duration.ofMinutes(1)));     // 아직 문제를 받기 전
        TestSession recentResult = session("d3", "10.0.0.1", now.minus(Duration.ofMinutes(10)));
        recentResult.submit(Map.of(), now.minus(Duration.ofMinutes(9)));                            // 결과 보관 중
        // 지워야 하는 시험
        TestSession pastDeadline = session("d4", "10.0.0.1", now.minus(Duration.ofMinutes(30)));
        pastDeadline.markQuestionsDelivered();
        TestSession undelivered = session("d5", "10.0.0.1", now.minus(Duration.ofMinutes(6)));      // 5분 넘게 문제를 안 받아 감
        TestSession oldResult = session("d6", "10.0.0.1", now.minus(Duration.ofHours(3)));
        oldResult.submit(Map.of(), now.minus(Duration.ofHours(3)).plusSeconds(30));                 // 보관 1시간 지남
        TestSession expired = session("d7", "10.0.0.1", now.minus(Duration.ofMinutes(1)));
        expired.expire();

        List<TestSession> all = List.of(delivered, justStarted, recentResult, pastDeadline, undelivered, oldResult, expired);
        all.forEach(repository::save);

        int deleted = repository.deleteDisposable(now, RETENTION, UNDELIVERED);

        assertThat(deleted).isEqualTo(4);
        assertThat(jpa.findAll()).extracting(TestSession::getId)
                .containsExactlyInAnyOrder(delivered.getId(), justStarted.getId(), recentResult.getId());
        // 화면·서비스가 쓰는 판정과 DB 삭제 조건이 어긋나지 않는지
        List<UUID> disposable = all.stream()
                .filter(s -> s.isDisposable(now, RETENTION, UNDELIVERED))
                .map(TestSession::getId)
                .toList();
        assertThat(disposable).containsExactlyInAnyOrder(
                pastDeadline.getId(), undelivered.getId(), oldResult.getId(), expired.getId());
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
                        JpaTestSessionRepository.START_LOCK_KEY)));
    }

}

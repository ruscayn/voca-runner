package org.ruscayn.vocarunner.exam.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.common.adapter.out.persistence.JpaConfig;
import org.ruscayn.vocarunner.exam.adapter.out.persistence.ExamPersistenceAdapter;
import org.ruscayn.vocarunner.exam.application.port.in.CleanUpExamsUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamCapacity;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.ruscayn.vocarunner.support.TestPostgres;
import org.ruscayn.vocarunner.support.TestWords;
import org.ruscayn.vocarunner.word.application.port.in.WordCatalogQuery;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 안전망: 유스케이스마다 DB(test_session)에 무엇이 남는지 실제 PostgreSQL 로 확인한다.
 * 시험 도메인과 DB 저장용 클래스를 나누는 작업(저장을 코드에서 직접 부르게 됨) 전후로 같은 결과가 나와야 한다.
 *
 * <p>테스트에 트랜잭션을 걸지 않아 서비스 트랜잭션이 실제로 커밋된다. 확인은 JPA 가 아닌 JdbcTemplate 으로
 * 표를 직접 읽는다 (같은 영속성 컨텍스트로 다시 읽으면 저장 누락이 가려짐). 그래서 테스트마다 표를 비운다.
 */
@DataJpaTest
@ImportTestcontainers(TestPostgres.class)
@Import({StartExamService.class, ExamService.class, CleanUpExamsService.class, ExamPersistenceAdapter.class,
        JpaConfig.class, ExamServicePersistenceTest.Config.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("시험 서비스 : 유스케이스별 DB 상태 (PostgreSQL)")
class ExamServicePersistenceTest {

    private static final int QUESTION_COUNT = 4;
    private static final int TIME_PER_QUESTION_SEC = Exam.DEFAULT_TIME_PER_QUESTION_SEC;
    private static final Duration GRACE = Duration.ofMinutes(3);
    private static final int MAX_IN_PROGRESS_PER_IP = 2;
    private static final String DEVICE_ID = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER_DEVICE_ID = "22222222-2222-2222-2222-222222222222";
    private static final String IP = "10.0.0.1";

    @TestConfiguration
    static class Config {

        /** A1 단어 20개 (뜻이 모두 다름) */
        @Bean
        WordCatalogQuery wordCatalogQuery() {
            WordCatalog wordCatalog = new WordCatalog(TestWords.words(Level.A1, 1, 20), List.of());
            return () -> wordCatalog;
        }

        @Bean
        QuestionGenerator questionGenerator() {
            return new QuestionGenerator();
        }

        /** 문항 4개, IP당 최대 진행 중 2개. 나머지는 기본값 */
        @Bean
        @Primary
        ExamProperties examProperties() {
            return new ExamProperties(QUESTION_COUNT, GRACE, Duration.ofHours(1),
                    new ExamCapacity(20000, MAX_IN_PROGRESS_PER_IP), Duration.ofMinutes(2));
        }

    }

    @Autowired
    private StartExamUseCase startExamUseCase;

    @Autowired
    private ExamUseCase examUseCase;

    @Autowired
    private CleanUpExamsUseCase cleanUpExamsUseCase;

    @Autowired
    private ExamPersistencePort examPersistencePort;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clear() {
        jdbcTemplate.update("delete from test_session");
    }

    private UUID start(String deviceId, String ip) {
        return startExamUseCase.start(new StartExamCommand(Level.A1, new Examinee(deviceId, ip)));
    }

    /** 서비스를 거치지 않고 시작 시각을 정한 시험 (마감 지남 · 오래된 결과 등을 만들 때) */
    private static Exam exam(String deviceId, Instant startedAt) {
        return Exam.start(Level.A1, TestWords.questions(QUESTION_COUNT), new Examinee(deviceId, IP), startedAt, GRACE);
    }

    private Exam saved(Exam exam) {
        examPersistencePort.save(exam);
        return exam;
    }

    private Map<String, Object> row(UUID examId) {
        return jdbcTemplate.queryForMap("select * from test_session where test_session_id = ?", examId);
    }

    private int rowCount() {
        return jdbcTemplate.queryForObject("select count(*) from test_session", Integer.class);
    }

    private boolean exists(UUID examId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from test_session where test_session_id = ?", Integer.class, examId) == 1;
    }

    /** DB 에 저장된 문항 no 의 정답 보기 번호 */
    private int answerOptionNo(UUID examId, int no) {
        return ((byte[]) row(examId).get("correct_option_no"))[no - 1];
    }

    /** 정답이 아닌 보기 번호 하나 */
    private int incorrectOptionNo(UUID examId, int no) {
        return answerOptionNo(examId, no) % 4 + 1;
    }

    private static SubmitExamCommand.SelectedOptionItem selectedOption(int no, Integer selectedOptionNo) {
        return new SubmitExamCommand.SelectedOptionItem(no, selectedOptionNo);
    }

    private void submit(UUID examId, String deviceId, SubmitExamCommand.SelectedOptionItem... selectedOptions) {
        examUseCase.submit(new SubmitExamCommand(examId, deviceId, List.of(selectedOptions)));
    }

    private static Consumer<CustomException> errorCode(ErrorCode expected) {
        return e -> assertThat(e.getErrorCode()).isEqualTo(expected);
    }

    @Nested
    @DisplayName("시작")
    class Start {

        @Test
        @DisplayName("시작하면 진행 중 행이 하나 생긴다 (문항 전달 전, 선택한 보기는 모두 0, 버전 0)")
        void inserted() {
            UUID examId = start(DEVICE_ID, IP);

            Map<String, Object> row = row(examId);
            assertThat(row).containsEntry("status", "IN_PROGRESS")
                    .containsEntry("level_code", "A1")
                    .containsEntry("direction", "ENGLISH")
                    .containsEntry("time_limit_sec", TIME_PER_QUESTION_SEC)
                    .containsEntry("device_id", DEVICE_ID)
                    .containsEntry("client_ip", IP)
                    .containsEntry("questions_delivered", false)
                    .containsEntry("version", 0);
            assertThat(row.get("submitted_at")).isNull();
            assertThat(row.get("correct_count")).isNull();
            assertThat(row.get("score")).isNull();
            assertThat(row.get("created_at")).isNotNull();
            byte[] correctOptionNo = (byte[]) row.get("correct_option_no");
            assertThat(correctOptionNo).hasSize(QUESTION_COUNT);
            for (byte no : correctOptionNo) {
                assertThat(no).isBetween((byte) 1, (byte) 4);
            }
            assertThat((byte[]) row.get("selected_option_no")).containsExactly(0, 0, 0, 0);
            assertThat(jdbcTemplate.queryForObject(
                    "select cardinality(option_ids) from test_session", Integer.class)).isEqualTo(QUESTION_COUNT * 4);
            // 마감 = 시작 + (4문항 × 5초 + 3분)을 분 단위로 올림 = 4분
            assertThat(jdbcTemplate.queryForObject(
                    "select extract(epoch from deadline - started_at) from test_session", Double.class))
                    .isEqualTo(240.0);
        }

        @Test
        @DisplayName("같은 기기에서 다시 시작하면 이전 행은 지워지고 새 행만 남는다")
        void replacesSameDevice() {
            UUID first = start(DEVICE_ID, IP);

            UUID second = start(DEVICE_ID, IP);

            assertThat(exists(first)).isFalse();
            assertThat(exists(second)).isTrue();
            assertThat(rowCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("IP당 최대 진행 중에 걸리면 거절하고 행을 만들지 않는다")
        void rejectedPerIp() {
            start("device-1", IP);
            start("device-2", IP);

            assertThatThrownBy(() -> start("device-3", IP))
                    .isInstanceOfSatisfying(CustomException.class,
                            errorCode(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP));
            assertThat(rowCount()).isEqualTo(MAX_IN_PROGRESS_PER_IP);
        }

    }

    @Nested
    @DisplayName("문항 전달")
    class GetQuestions {

        @Test
        @DisplayName("문항을 전달하면 questions_delivered 가 true, 버전이 1 오르고 만든 시각은 그대로다")
        void markedDelivered() {
            UUID examId = start(DEVICE_ID, IP);
            Timestamp createdAt = (Timestamp) row(examId).get("created_at");

            examUseCase.deliverQuestions(examId, DEVICE_ID);

            Map<String, Object> row = row(examId);
            assertThat(row).containsEntry("questions_delivered", true)
                    .containsEntry("status", "IN_PROGRESS")
                    .containsEntry("version", 1)
                    .containsEntry("created_at", createdAt);
        }

        @Test
        @DisplayName("다시 받아도 바뀐 것이 없어 버전이 그대로다")
        void secondTimeNoUpdate() {
            UUID examId = start(DEVICE_ID, IP);
            examUseCase.deliverQuestions(examId, DEVICE_ID);

            examUseCase.deliverQuestions(examId, DEVICE_ID);

            assertThat(row(examId)).containsEntry("version", 1);
        }

        @Test
        @DisplayName("다른 기기가 받으려 하면 없는 시험으로 거절하고 행은 그대로다")
        void otherDevice() {
            UUID examId = start(DEVICE_ID, IP);

            assertThatThrownBy(() -> examUseCase.deliverQuestions(examId, OTHER_DEVICE_ID))
                    .isInstanceOfSatisfying(CustomException.class, errorCode(ErrorCode.ENTITY_NOT_FOUND));
            assertThat(row(examId)).containsEntry("questions_delivered", false).containsEntry("version", 0);
        }

    }

    @Nested
    @DisplayName("제출")
    class Submit {

        @Test
        @DisplayName("제출하면 완료로 바뀌고 선택한 보기 · 맞힌 수 · 점수 · 제출 시각이 남는다 (선택하지 못한 문항 = 0)")
        void completed() {
            UUID examId = start(DEVICE_ID, IP);
            examUseCase.deliverQuestions(examId, DEVICE_ID);
            Timestamp createdAt = (Timestamp) row(examId).get("created_at");
            int correct1 = answerOptionNo(examId, 1);
            int wrong2 = incorrectOptionNo(examId, 2);

            // 3번은 시간 초과(null), 4번은 항목 없음
            submit(examId, DEVICE_ID, selectedOption(1, correct1), selectedOption(2, wrong2), selectedOption(3, null));

            Map<String, Object> row = row(examId);
            assertThat(row).containsEntry("status", "COMPLETED")
                    .containsEntry("correct_count", 1)
                    .containsEntry("score", 25)
                    .containsEntry("version", 2)
                    .containsEntry("created_at", createdAt);
            assertThat(row.get("submitted_at")).isNotNull();
            assertThat((byte[]) row.get("selected_option_no")).containsExactly(correct1, wrong2, 0, 0);
        }

        @Test
        @DisplayName("마감이 지난 시험을 제출하면 거절하고 행을 지운다")
        void expiredDeleted() {
            Exam exam = saved(exam(DEVICE_ID, Instant.now().minus(Duration.ofHours(1))));

            assertThatThrownBy(() -> submit(exam.getId(), DEVICE_ID, selectedOption(1, 1)))
                    .isInstanceOfSatisfying(CustomException.class, errorCode(ErrorCode.EXAM_EXPIRED));
            assertThat(exists(exam.getId())).isFalse();
        }

        @Test
        @DisplayName("없는 문항 번호로 제출하면 거절하고 행은 그대로다")
        void invalidQuestionNo() {
            UUID examId = start(DEVICE_ID, IP);

            assertThatThrownBy(() -> submit(examId, DEVICE_ID, selectedOption(QUESTION_COUNT + 1, 1)))
                    .isInstanceOfSatisfying(CustomException.class, errorCode(ErrorCode.INVALID_INPUT_VALUE));
            assertThat(row(examId)).containsEntry("status", "IN_PROGRESS").containsEntry("version", 0);
        }

        @Test
        @DisplayName("이미 제출한 시험을 다시 제출하면 거절하고 처음 결과가 그대로다")
        void submitAgain() {
            UUID examId = start(DEVICE_ID, IP);
            submit(examId, DEVICE_ID, selectedOption(1, answerOptionNo(examId, 1)));

            assertThatThrownBy(() -> submit(examId, DEVICE_ID,
                    selectedOption(1, answerOptionNo(examId, 1)), selectedOption(2, answerOptionNo(examId, 2))))
                    .isInstanceOfSatisfying(CustomException.class, errorCode(ErrorCode.EXAM_NOT_IN_PROGRESS));
            assertThat(row(examId)).containsEntry("correct_count", 1).containsEntry("version", 1);
        }

        @Test
        @DisplayName("같은 시험을 동시에 두 번 제출하면 먼저 커밋한 것만 남고 늦은 쪽은 저장 충돌로 실패한다 (U39)")
        void submitTwiceAtOnce() {
            UUID examId = start(DEVICE_ID, IP);
            int correct1 = answerOptionNo(examId, 1);
            int correct2 = answerOptionNo(examId, 2);

            // 늦은 쪽: 시험을 읽고 채점까지 했지만 아직 커밋 전
            Overlap late = Overlap.begin(transactionManager,
                    () -> submit(examId, DEVICE_ID, selectedOption(1, correct1)));
            // 먼저 커밋하는 쪽
            submit(examId, DEVICE_ID, selectedOption(1, correct1), selectedOption(2, correct2));

            assertThatThrownBy(late::commit).isInstanceOf(OptimisticLockingFailureException.class);
            assertThat(row(examId)).containsEntry("status", "COMPLETED")
                    .containsEntry("correct_count", 2)
                    .containsEntry("version", 1);
        }

    }

    @Nested
    @DisplayName("중단")
    class Abort {

        @Test
        @DisplayName("진행 중 시험을 중단하면 행을 지운다")
        void deleted() {
            UUID examId = start(DEVICE_ID, IP);

            examUseCase.abortIfInProgress(examId, DEVICE_ID);

            assertThat(exists(examId)).isFalse();
        }

        @Test
        @DisplayName("다른 기기의 중단 요청은 무시한다")
        void otherDeviceIgnored() {
            UUID examId = start(DEVICE_ID, IP);

            examUseCase.abortIfInProgress(examId, OTHER_DEVICE_ID);

            assertThat(row(examId)).containsEntry("status", "IN_PROGRESS");
        }

        @Test
        @DisplayName("제출한 시험은 중단 요청이 와도 결과 행이 남는다")
        void completedKept() {
            UUID examId = start(DEVICE_ID, IP);
            submit(examId, DEVICE_ID, selectedOption(1, 1));

            examUseCase.abortIfInProgress(examId, DEVICE_ID);

            assertThat(row(examId)).containsEntry("status", "COMPLETED");
        }

    }

    @Test
    @DisplayName("정리 작업은 마감 지남 · 문제 안 받아 감 · 보관 지난 결과만 지운다")
    void cleanup() {
        Instant now = Instant.now();
        Exam delivered = exam("d1", now.minus(Duration.ofMinutes(1)));
        delivered.deliverQuestions(now);
        Exam justStarted = exam("d2", now.minus(Duration.ofMinutes(1)));        // 아직 문항 전달 전
        Exam pastDeadline = exam("d3", now.minus(Duration.ofMinutes(30)));
        Exam undelivered = exam("d4", now.minus(Duration.ofMinutes(3)));        // 2분 넘게 문제를 안 받아 감
        Exam oldResult = exam("d5", now.minus(Duration.ofHours(3)));
        oldResult.submit(List.of(), now.minus(Duration.ofHours(3)).plusSeconds(30));   // 보관 1시간 지남
        List.of(delivered, justStarted, pastDeadline, undelivered, oldResult).forEach(this::saved);

        cleanUpExamsUseCase.cleanUp();

        assertThat(jdbcTemplate.queryForList("select test_session_id from test_session", UUID.class))
                .containsExactlyInAnyOrder(delivered.getId(), justStarted.getId());
        assertThat(List.of(pastDeadline, undelivered, oldResult)).noneMatch(exam -> exists(exam.getId()));
    }

    /**
     * B11 재현 (11/03). 중단이 같은 시험의 다른 요청과 겹치면, 늦게 커밋하는 쪽이 저장 충돌(@Version)로 실패한다.
     * 예전에는 재입장(새로고침) 때 화면 쪽 중단이 겹쳐 500 오류 화면이 됐다. 11/05 그 화면 경로가 없어져(P13)
     * 사용자에게 보이는 일은 없고, 서비스의 이 동작은 그대로라 테스트로 고정한다
     */
    @Nested
    @DisplayName("B11 재현 : 중단이 같은 시험의 다른 요청과 겹침")
    class B11 {

        @Test
        @DisplayName("중단 두 개가 겹치면 늦게 커밋하는 쪽이 저장 충돌로 실패한다")
        void twoAborts() {
            UUID examId = start(DEVICE_ID, IP);

            Overlap late = Overlap.begin(transactionManager, () -> examUseCase.abortIfInProgress(examId, DEVICE_ID));
            examUseCase.abortIfInProgress(examId, DEVICE_ID);

            assertThatThrownBy(late::commit).isInstanceOf(OptimisticLockingFailureException.class);
            assertThat(exists(examId)).isFalse();
        }

        @Test
        @DisplayName("중단이 시험을 읽은 뒤 문항 전달이 먼저 커밋하면, 중단이 저장 충돌로 실패하고 시험은 진행 중으로 남는다")
        void abortAfterQuestionsDelivered() {
            UUID examId = start(DEVICE_ID, IP);

            Overlap late = Overlap.begin(transactionManager, () -> examUseCase.abortIfInProgress(examId, DEVICE_ID));
            examUseCase.deliverQuestions(examId, DEVICE_ID);

            assertThatThrownBy(late::commit).isInstanceOf(OptimisticLockingFailureException.class);
            assertThat(row(examId)).containsEntry("status", "IN_PROGRESS")
                    .containsEntry("questions_delivered", true);
        }

        @Test
        @DisplayName("문항 전달이 시험을 읽은 뒤 중단이 먼저 커밋하면, 문항 전달이 저장 충돌로 실패한다 (시험은 지워짐)")
        void questionsDeliveredAfterAbort() {
            UUID examId = start(DEVICE_ID, IP);

            Overlap late = Overlap.begin(transactionManager, () -> examUseCase.deliverQuestions(examId, DEVICE_ID));
            examUseCase.abortIfInProgress(examId, DEVICE_ID);

            assertThatThrownBy(late::commit).isInstanceOf(OptimisticLockingFailureException.class);
            assertThat(exists(examId)).isFalse();
        }

    }

    /**
     * 두 요청이 겹치는 순서를 정해 재현한다.
     * begin: 다른 스레드(다른 DB 연결)의 트랜잭션 안에서 유스케이스를 끝까지 부르고(서비스 트랜잭션이 여기에 합류) 커밋 직전에 멈춘다.
     * 그 사이 테스트 스레드가 다른 요청을 끝까지 처리한 뒤 commit 으로 늦은 쪽을 커밋한다
     */
    private static final class Overlap {

        private final CountDownLatch called = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private CompletableFuture<Void> future;

        static Overlap begin(PlatformTransactionManager transactionManager, Runnable useCase) {
            Overlap overlap = new Overlap();
            overlap.future = CompletableFuture.runAsync(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        try {
                            useCase.run();
                        } finally {
                            overlap.called.countDown();
                        }
                        await(overlap.release);
                    }));
            await(overlap.called);
            return overlap;
        }

        /** 늦은 쪽을 커밋한다. 실패하면 그 예외를 그대로 던진다 */
        void commit() {
            release.countDown();
            try {
                future.join();
            } catch (CompletionException e) {
                throw (RuntimeException) e.getCause();
            }
        }

        private static void await(CountDownLatch latch) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("기다리는 시간이 지남");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }

    }

}

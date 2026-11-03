package org.ruscayn.vocarunner.exam.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.Level;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.correctOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.questions;
import static org.ruscayn.vocarunner.support.TestWords.wrongOptionNo;

@DisplayName("TestSession : 시험 1회 (정답지·채점·마감)")
class TestSessionTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final Duration GRACE = Duration.ofMinutes(3);
    private static final Duration RETENTION = Duration.ofMinutes(10);
    private static final Duration UNDELIVERED = Duration.ofMinutes(5);
    private static final TestClient CLIENT = new TestClient("device-1", "127.0.0.1");

    private static TestSession start(int questionCount) {
        return TestSession.start(Level.B1, Direction.EN_KO, 5, questions(questionCount), CLIENT, NOW, GRACE);
    }

    @Nested
    @DisplayName("마감 시간 계산 (분 단위 올림)")
    class AllowedDuration {

        @ParameterizedTest(name = "{0}문항 × {1}초 + {2}초 → {3}분")
        @CsvSource({
                "500, 5, 180, 45",   // 41분 40초 + 3분 = 44분 40초 → 45분
                "5,   5, 180, 4",    // 25초 + 3분 = 3분 25초 → 4분
                "12,  5, 0,   1",    // 정확히 60초 → 1분 (올림 없음)
                "13,  5, 0,   2",    // 65초 → 2분
                "500, 10, 180, 87"   // 83분 20초 + 3분 = 86분 20초 → 87분
        })
        void roundsUpToMinutes(int count, int sec, int graceSec, long expectedMinutes) {
            Duration allowed = TestSession.allowedDuration(count, sec, Duration.ofSeconds(graceSec));

            assertThat(allowed).isEqualTo(Duration.ofMinutes(expectedMinutes));
        }

        @Test
        @DisplayName("마감 시각 = 시작 시각 + 분 단위 제한 시간")
        void deadline() {
            TestSession session = start(500);

            assertThat(session.getDeadline()).isEqualTo(NOW.plus(Duration.ofMinutes(45)));
        }
    }

    @Nested
    @DisplayName("시작")
    class Start {

        @Test
        @DisplayName("진행 중 상태로 만들어지고 문항 수·정답이 기록된다")
        void createsInProgress() {
            List<GeneratedQuestion> questions = questions(4);
            TestSession session = TestSession.start(Level.A1, Direction.EN_KO, 5, questions, CLIENT, NOW, GRACE);

            assertThat(session.getStatus()).isEqualTo(TestStatus.IN_PROGRESS);
            assertThat(session.getQuestionCount()).isEqualTo(4);
            assertThat(session.isInProgress(NOW)).isTrue();
            for (int no = 1; no <= 4; no++) {
                GeneratedQuestion q = questions.get(no - 1);
                assertThat(session.answerLevelWordId(no)).isEqualTo(q.answer().levelWordId());
                assertThat(session.optionLevelWordIds(no))
                        .containsExactlyElementsOf(q.options().stream().map(o -> o.levelWordId()).toList());
            }
        }

        @Test
        @DisplayName("시작한 기기만 일치한다 (다른 기기·쿠키 없음은 불일치)")
        void startedBy() {
            TestSession session = start(1);

            assertThat(session.isStartedBy("device-1")).isTrue();
            assertThat(session.isStartedBy("device-2")).isFalse();
            assertThat(session.isStartedBy(null)).isFalse();
        }

        @Test
        @DisplayName("문제가 없으면 시작할 수 없다")
        void emptyQuestions() {
            assertThatThrownBy(() -> TestSession.start(Level.A1, Direction.EN_KO, 5, List.of(), CLIENT, NOW, GRACE))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @ParameterizedTest(name = "제한 시간 {0}초")
        @CsvSource({"4", "11"})
        @DisplayName("문제당 제한 시간은 5~10초만 허용")
        void timeLimitRange(int sec) {
            assertThatThrownBy(() -> TestSession.start(Level.A1, Direction.EN_KO, sec, questions(1), CLIENT, NOW, GRACE))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
        }

        @Test
        @DisplayName("없는 문제 번호를 조회하면 예외")
        void questionNoRange() {
            TestSession session = start(3);

            assertThatThrownBy(() -> session.answerLevelWordId(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> session.answerLevelWordId(4)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("제출·채점")
    class Submit {

        @Test
        @DisplayName("정답·오답·시간 초과(null)·미응답을 구분해 채점한다")
        void grades() {
            TestSession session = start(4);
            Map<Integer, Integer> answers = new HashMap<>();
            answers.put(1, correctOptionNo(1));   // 정답
            answers.put(2, wrongOptionNo(2));     // 오답
            answers.put(3, null);                 // 시간 초과
            // 4번은 아예 없음 (미응답)

            session.submit(answers, NOW.plusSeconds(30));

            assertThat(session.getStatus()).isEqualTo(TestStatus.COMPLETED);
            assertThat(session.isCompleted()).isTrue();
            assertThat(session.getCorrectCount()).isEqualTo(1);
            assertThat(session.getScore()).isEqualTo(25);
            assertThat(session.getSubmittedAt()).isEqualTo(NOW.plusSeconds(30));

            assertThat(session.isCorrect(1)).isTrue();
            assertThat(session.isCorrect(2)).isFalse();
            assertThat(session.isCorrect(3)).isFalse();
            assertThat(session.isCorrect(4)).isFalse();

            assertThat(session.selectedLevelWordId(1)).isEqualTo(session.answerLevelWordId(1));
            assertThat(session.selectedLevelWordId(2))
                    .isEqualTo(session.optionLevelWordIds(2).get(wrongOptionNo(2) - 1));
            assertThat(session.selectedLevelWordId(3)).isNull();
            assertThat(session.selectedLevelWordId(4)).isNull();
        }

        @Test
        @DisplayName("점수는 버림 (3문항 중 2개 → 66점)")
        void scoreFloor() {
            TestSession session = start(3);

            session.submit(Map.of(1, correctOptionNo(1), 2, correctOptionNo(2)), NOW);

            assertThat(session.getScore()).isEqualTo(66);
        }

        @Test
        @DisplayName("하나라도 틀리면 100점이 아니다 (997문항 중 995개 → 99점, U1)")
        void notPerfectIsNot100() {
            TestSession session = start(997);
            Map<Integer, Integer> answers = new HashMap<>();
            for (int no = 1; no <= 995; no++) {
                answers.put(no, correctOptionNo(no));
            }

            session.submit(answers, NOW);

            assertThat(session.getCorrectCount()).isEqualTo(995);
            assertThat(session.getScore()).isEqualTo(99);
        }

        @Test
        @DisplayName("모두 맞히면 100점")
        void perfect() {
            TestSession session = start(5);
            Map<Integer, Integer> answers = new HashMap<>();
            for (int no = 1; no <= 5; no++) {
                answers.put(no, correctOptionNo(no));
            }

            session.submit(answers, NOW);

            assertThat(session.getCorrectCount()).isEqualTo(5);
            assertThat(session.getScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("마감 시각 정각까지는 제출할 수 있다")
        void atDeadline() {
            TestSession session = start(5);

            session.submit(Map.of(), session.getDeadline());

            assertThat(session.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("마감 시각이 지나면 무효(EXPIRED) 처리되고 TEST_TIME_OVER")
        void afterDeadline() {
            TestSession session = start(5);
            Instant late = session.getDeadline().plusMillis(1);

            assertThatThrownBy(() -> session.submit(Map.of(1, 1), late))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TEST_TIME_OVER);
            assertThat(session.getStatus()).isEqualTo(TestStatus.EXPIRED);
        }

        @ParameterizedTest(name = "보기 번호 {0}")
        @CsvSource({"0", "5", "-1"})
        @DisplayName("보기 번호가 1~4가 아니면 거절하고 시험은 계속 진행 중")
        void invalidOption(int optionNo) {
            TestSession session = start(2);

            assertThatThrownBy(() -> session.submit(Map.of(1, optionNo), NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TEST_INVALID_OPTION);
            assertThat(session.getStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("이미 제출한 시험은 다시 제출할 수 없다")
        void twice() {
            TestSession session = start(2);
            session.submit(Map.of(), NOW);

            assertThatThrownBy(() -> session.submit(Map.of(), NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TEST_NOT_IN_PROGRESS);
        }

        @Test
        @DisplayName("만료된 시험은 제출할 수 없다")
        void afterExpire() {
            TestSession session = start(2);
            session.expire();

            assertThatThrownBy(() -> session.submit(Map.of(), NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.TEST_NOT_IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("상태")
    class State {

        @Test
        @DisplayName("진행 중 여부는 마감 시각을 기준으로 판단")
        void inProgressByDeadline() {
            TestSession session = start(5);

            assertThat(session.isInProgress(session.getDeadline())).isTrue();
            assertThat(session.isInProgress(session.getDeadline().plusMillis(1))).isFalse();
        }

        @Test
        @DisplayName("만료 : 진행 중이면 EXPIRED, 완료된 시험은 그대로")
        void expire() {
            TestSession inProgress = start(2);
            inProgress.expire();
            assertThat(inProgress.getStatus()).isEqualTo(TestStatus.EXPIRED);
            assertThat(inProgress.isInProgress(NOW)).isFalse();

            TestSession completed = start(2);
            completed.submit(Map.of(), NOW);
            completed.expire();
            assertThat(completed.getStatus()).isEqualTo(TestStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("정리 대상 판단 (isDisposable)")
    class Disposable {

        @Test
        @DisplayName("진행 중 : 마감 시각이 지나야 정리")
        void inProgress() {
            TestSession session = start(5);
            session.markQuestionsDelivered();

            assertThat(session.isDisposable(session.getDeadline(), RETENTION, UNDELIVERED)).isFalse();
            assertThat(session.isDisposable(session.getDeadline().plusMillis(1), RETENTION, UNDELIVERED)).isTrue();
        }

        @Test
        @DisplayName("진행 중 : 문제를 받아 가지 않았으면 시작 후 정해진 시간이 지나면 정리")
        void undelivered() {
            TestSession session = start(500);

            assertThat(session.isDisposable(NOW.plus(UNDELIVERED), RETENTION, UNDELIVERED)).isFalse();
            assertThat(session.isDisposable(NOW.plus(UNDELIVERED).plusMillis(1), RETENTION, UNDELIVERED)).isTrue();
        }

        @Test
        @DisplayName("진행 중 : 문제를 받아 갔으면 시작 후 정해진 시간이 지나도 마감 전에는 정리하지 않음")
        void delivered() {
            TestSession session = start(500);
            session.markQuestionsDelivered();

            assertThat(session.isDisposable(NOW.plus(UNDELIVERED).plusMillis(1), RETENTION, UNDELIVERED)).isFalse();
        }

        @Test
        @DisplayName("완료 : 제출 후 결과 보관 시간이 지나야 정리")
        void completed() {
            TestSession session = start(5);
            Instant submittedAt = NOW.plusSeconds(60);
            session.submit(Map.of(), submittedAt);

            assertThat(session.isDisposable(submittedAt.plus(RETENTION), RETENTION, UNDELIVERED)).isFalse();
            assertThat(session.isDisposable(submittedAt.plus(RETENTION).plusMillis(1), RETENTION, UNDELIVERED)).isTrue();
        }

        @Test
        @DisplayName("무효 : 바로 정리")
        void expired() {
            TestSession session = start(5);
            session.expire();

            assertThat(session.isDisposable(NOW, RETENTION, UNDELIVERED)).isTrue();
        }
    }

}

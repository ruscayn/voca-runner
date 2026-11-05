package org.ruscayn.vocarunner.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.answerOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.incorrectOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.questions;
import static org.ruscayn.vocarunner.support.TestWords.selected;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.Level;

@DisplayName("Exam : 시험 1회 (정답지·채점·마감)")
class ExamTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    private static final Duration GRACE = Duration.ofMinutes(3);
    private static final Examinee EXAMINEE = new Examinee("device-1", "127.0.0.1");

    private static Exam start(int questionCount) {
        return Exam.start(Level.B1, questions(questionCount), EXAMINEE, NOW, GRACE);
    }

    @Nested
    @DisplayName("시작")
    class Start {

        @Test
        @DisplayName("진행 중 상태로 만들어지고 문항 수·정답이 기록된다")
        void createsInProgress() {
            List<Question> questions = questions(4);
            Exam exam = Exam.start(Level.A1, questions, EXAMINEE, NOW, GRACE);

            assertThat(exam.getStatus()).isEqualTo(ExamStatus.IN_PROGRESS);
            assertThat(exam.getQuestionCount()).isEqualTo(4);
            assertThat(exam.isInProgress(NOW)).isTrue();
            for (int no = 1; no <= 4; no++) {
                Question q = questions.get(no - 1);
                assertThat(exam.answerLevelWordId(no)).isEqualTo(q.answer().levelWordId());
                assertThat(exam.optionLevelWordIds(no))
                        .containsExactlyElementsOf(q.options().stream().map(o -> o.levelWordId()).toList());
            }
        }

        @Test
        @DisplayName("방향은 EN_KO, 문항당 시간은 기본 5초, 마감 = 시작 + 분 단위 허용 시간 (500문항 × 5초 + 3분 → 45분)")
        void defaults() {
            Exam exam = start(500);

            assertThat(exam.getDirection()).isEqualTo(Direction.EN_KO);
            assertThat(exam.getTimePerQuestionSec()).isEqualTo(Exam.DEFAULT_TIME_PER_QUESTION_SEC);
            assertThat(exam.getTime().startedAt()).isEqualTo(NOW);
            assertThat(exam.getTime().deadline()).isEqualTo(NOW.plus(Duration.ofMinutes(45)));
            assertThat(Exam.allowedTime(500, GRACE)).isEqualTo(Duration.ofMinutes(45));
        }

        @Test
        @DisplayName("시작한 기기만 일치한다 (다른 기기·쿠키 없음은 불일치)")
        void startedBy() {
            Exam exam = start(1);

            assertThat(exam.isStartedBy("device-1")).isTrue();
            assertThat(exam.isStartedBy("device-2")).isFalse();
            assertThat(exam.isStartedBy(null)).isFalse();
        }

        @Test
        @DisplayName("없는 문제 번호를 조회하면 예외")
        void questionNoRange() {
            Exam exam = start(3);

            assertThatThrownBy(() -> exam.answerLevelWordId(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> exam.answerLevelWordId(4)).isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested
    @DisplayName("문항 전달")
    class DeliverQuestions {

        @Test
        @DisplayName("진행 중이면 문항 전달 여부를 남긴다")
        void marksDelivered() {
            Exam exam = start(2);

            exam.deliverQuestions(NOW);

            assertThat(exam.isQuestionsDelivered()).isTrue();
        }

        @Test
        @DisplayName("마감이 지났거나 제출한 시험이면 EXAM_NOT_IN_PROGRESS, 전달 여부는 그대로")
        void notInProgress() {
            Exam pastDeadline = start(2);
            Exam completed = start(2);
            completed.submit(List.of(), NOW);

            assertThatThrownBy(() -> pastDeadline.deliverQuestions(pastDeadline.getTime().deadline().plusMillis(1)))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EXAM_NOT_IN_PROGRESS);
            assertThatThrownBy(() -> completed.deliverQuestions(NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EXAM_NOT_IN_PROGRESS);
            assertThat(pastDeadline.isQuestionsDelivered()).isFalse();
            assertThat(completed.isQuestionsDelivered()).isFalse();
        }

    }

    @Nested
    @DisplayName("제출·채점")
    class Submit {

        @Test
        @DisplayName("정답·오답·시간 초과(null)·미응답을 구분해 채점한다")
        void grades() {
            Map<Integer, Integer> answers = new HashMap<>();
            answers.put(1, answerOptionNo(1));   // 정답
            answers.put(2, incorrectOptionNo(2));     // 오답
            answers.put(3, null);                 // 시간 초과
            // 4번은 아예 없음 (미응답)

            Exam exam = start(4);
            exam.submit(selected(answers), NOW.plusSeconds(30));

            assertThat(exam.getStatus()).isEqualTo(ExamStatus.COMPLETED);
            assertThat(exam.isCompleted()).isTrue();
            assertThat(exam.getGradingResult().correctCount()).isEqualTo(1);
            assertThat(exam.getGradingResult().score()).isEqualTo(25);
            assertThat(exam.getGradingResult().submittedAt()).isEqualTo(NOW.plusSeconds(30));

            assertThat(exam.isCorrect(1)).isTrue();
            assertThat(exam.isCorrect(2)).isFalse();
            assertThat(exam.isCorrect(3)).isFalse();
            assertThat(exam.isCorrect(4)).isFalse();

            assertThat(exam.selectedLevelWordId(1)).isEqualTo(exam.answerLevelWordId(1));
            assertThat(exam.selectedLevelWordId(2))
                    .isEqualTo(exam.optionLevelWordIds(2).get(incorrectOptionNo(2) - 1));
            assertThat(exam.selectedLevelWordId(3)).isNull();
            assertThat(exam.selectedLevelWordId(4)).isNull();
            assertThat(exam.wrongQuestionNos()).containsExactly(2, 3, 4);
        }

        @Test
        @DisplayName("점수는 버림 (3문항 중 2개 → 66점)")
        void scoreFloor() {
            Exam exam = start(3);

            exam.submit(selected(Map.of(1, answerOptionNo(1), 2, answerOptionNo(2))), NOW);

            assertThat(exam.getGradingResult().score()).isEqualTo(66);
        }

        @Test
        @DisplayName("하나라도 틀리면 100점이 아니다 (997문항 중 995개 → 99점, U1)")
        void notPerfectIsNot100() {
            Exam exam = start(997);
            Map<Integer, Integer> answers = new HashMap<>();
            for (int no = 1; no <= 995; no++) {
                answers.put(no, answerOptionNo(no));
            }

            exam.submit(selected(answers), NOW);

            assertThat(exam.getGradingResult().correctCount()).isEqualTo(995);
            assertThat(exam.getGradingResult().score()).isEqualTo(99);
        }

        @Test
        @DisplayName("모두 맞히면 100점")
        void perfect() {
            Exam exam = start(5);
            Map<Integer, Integer> answers = new HashMap<>();
            for (int no = 1; no <= 5; no++) {
                answers.put(no, answerOptionNo(no));
            }

            exam.submit(selected(answers), NOW);

            assertThat(exam.getGradingResult().correctCount()).isEqualTo(5);
            assertThat(exam.getGradingResult().score()).isEqualTo(100);
        }

        @Test
        @DisplayName("마감 시각 정각까지는 제출할 수 있다")
        void atDeadline() {
            Exam exam = start(5);

            exam.submit(List.of(), exam.getTime().deadline());

            assertThat(exam.isCompleted()).isTrue();
        }

        @Test
        @DisplayName("마감 시각이 지나면 만료(EXPIRED) 처리되고 EXAM_EXPIRED")
        void afterDeadline() {
            Exam exam = start(5);
            Instant late = exam.getTime().deadline().plusMillis(1);

            assertThatThrownBy(() -> exam.submit(selected(Map.of(1, 1)), late))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EXAM_EXPIRED);
            assertThat(exam.getStatus()).isEqualTo(ExamStatus.EXPIRED);
        }

        @ParameterizedTest(name = "보기 번호 {0}")
        @CsvSource({"0", "5", "-1"})
        @DisplayName("보기 번호가 1~4가 아니면 거절하고 시험은 계속 진행 중")
        void invalidOption(int optionNo) {
            Exam exam = start(2);

            assertThatThrownBy(() -> exam.submit(selected(Map.of(1, optionNo)), NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EXAM_INVALID_OPTION);
            assertThat(exam.getStatus()).isEqualTo(ExamStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("이미 제출한 시험은 다시 제출할 수 없다")
        void twice() {
            Exam exam = start(2);
            exam.submit(List.of(), NOW);

            assertThatThrownBy(() -> exam.submit(List.of(), NOW))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }

        @Test
        @DisplayName("만료된 시험은 다시 제출할 수 없다")
        void afterExpire() {
            Exam exam = start(2);
            Instant late = exam.getTime().deadline().plusMillis(1);
            assertThatThrownBy(() -> exam.submit(List.of(), late)).isInstanceOf(CustomException.class);

            assertThatThrownBy(() -> exam.submit(List.of(), late))
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }

    }

    @Nested
    @DisplayName("상태")
    class State {

        @Test
        @DisplayName("진행 중 여부는 마감 시각을 기준으로 판단")
        void inProgressByDeadline() {
            Exam exam = start(5);

            assertThat(exam.isInProgress(exam.getTime().deadline())).isTrue();
            assertThat(exam.isInProgress(exam.getTime().deadline().plusMillis(1))).isFalse();
        }

        @Test
        @DisplayName("중단은 시작한 기기가 진행 중일 때만 할 수 있다")
        void abortable() {
            Exam inProgress = start(2);
            Exam completed = start(2);
            completed.submit(List.of(), NOW);

            assertThat(inProgress.isAbortableBy("device-1", NOW)).isTrue();
            assertThat(inProgress.isAbortableBy("device-2", NOW)).isFalse();
            assertThat(inProgress.isAbortableBy("device-1", inProgress.getTime().deadline().plusMillis(1))).isFalse();
            assertThat(completed.isAbortableBy("device-1", NOW)).isFalse();
        }

        @Test
        @DisplayName("완료 검사 : 제출하지 않았으면 EXAM_NOT_COMPLETED")
        void requireCompleted() {
            Exam inProgress = start(2);
            Exam completed = start(2);
            completed.submit(List.of(), NOW);

            completed.requireCompleted();
            assertThatThrownBy(inProgress::requireCompleted)
                    .isInstanceOf(CustomException.class)
                    .extracting(e -> ((CustomException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EXAM_NOT_COMPLETED);
        }

    }

}

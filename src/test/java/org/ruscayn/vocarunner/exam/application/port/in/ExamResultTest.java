package org.ruscayn.vocarunner.exam.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ruscayn.vocarunner.support.TestWords.answerOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.catalogOf;
import static org.ruscayn.vocarunner.support.TestWords.incorrectOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.questions;
import static org.ruscayn.vocarunner.support.TestWords.selected;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.Question;
import org.ruscayn.vocarunner.word.domain.Level;

@DisplayName("ExamResult : 결과 화면 집계")
class ExamResultTest {

    @Test
    @DisplayName("완료한 시험에서 만든다 : 맞힌 수 · 점수 · 보관 마감, 틀린 항목은 문항 차례 (시간 초과는 고른 글 없음)")
    void of() {
        Instant submittedAt = Instant.parse("2026-10-08T00:00:30Z");
        List<Question> questions = questions(3);
        Exam exam = Exam.start(Level.B1, questions, new Examinee("device-1", "10.0.0.1"),
                submittedAt.minusSeconds(30), Duration.ofMinutes(3));
        exam.submit(selected(Map.of(1, answerOptionNo(1), 2, incorrectOptionNo(2))), submittedAt);

        ExamResult result = ExamResult.of(exam, catalogOf(questions), Duration.ofHours(1));

        assertThat(result.level()).isEqualTo("B1");
        assertThat(result.groupName()).isEqualTo("Independent");
        assertThat(result.correctCount()).isEqualTo(1);
        assertThat(result.score()).isEqualTo(33);
        assertThat(result.retentionDeadline()).isEqualTo(submittedAt.plus(Duration.ofHours(1)));
        assertThat(result.retentionMinutes()).isEqualTo(60);
        assertThat(result.wrongItems()).extracting(ExamResult.WrongItem::no).containsExactly(2, 3);
        Question second = questions.get(1);
        ExamResult.WrongItem incorrect = result.wrongItems().get(0);
        assertThat(incorrect.prompt()).isEqualTo(second.answer().headword());
        assertThat(incorrect.answerText()).isEqualTo(second.answer().optionText());
        assertThat(incorrect.selectedText()).isEqualTo(second.options().get(incorrectOptionNo(2) - 1).optionText());
        assertThat(result.wrongItems().get(1).selectedText()).isNull();
    }

    @Test
    @DisplayName("틀린 항목을 오답(보기를 고름)과 시간 초과(고른 보기 없음)로 나눠 센다")
    void wrongAndTimeoutCount() {
        List<ExamResult.WrongItem> wrongItems = List.of(
                new ExamResult.WrongItem(1, "abandon", null, "채택하다", "버리다", "abandon"),
                new ExamResult.WrongItem(2, "reluctant", null, null, "꺼리는", "reluctant"),
                new ExamResult.WrongItem(3, "distinct", null, "먼", "뚜렷한", "distinct"),
                new ExamResult.WrongItem(4, "compensate", null, null, "보상하다", "compensate"),
                new ExamResult.WrongItem(5, "for the sake of", "for one's sake", null, "~을 위해서",
                        "for the sake of"));

        ExamResult result = new ExamResult(
                UUID.randomUUID(), "B1", "Independent", 10, 5, 50, Instant.now(), 20, wrongItems);

        assertThat(result.incorrectCount()).isEqualTo(2);
        assertThat(result.timeoutCount()).isEqualTo(3);
        assertThat(result.correctCount() + result.incorrectCount() + result.timeoutCount())
                .isEqualTo(result.questionCount());
    }

    @Test
    @DisplayName("모두 맞히면 오답·시간 초과 0")
    void perfect() {
        ExamResult result = new ExamResult(
                UUID.randomUUID(), "A1", "Basic", 5, 5, 100, Instant.now(), 20, List.of());

        assertThat(result.incorrectCount()).isZero();
        assertThat(result.timeoutCount()).isZero();
    }

    @Test
    @DisplayName("보관 시간 안내 : 60분 단위면 시간, 아니면 분")
    void retentionText() {
        assertThat(new ExamResult(UUID.randomUUID(), "A1", "Basic", 5, 5, 100, Instant.now(), 120, List.of())
                .retentionText()).isEqualTo("2시간");
        assertThat(new ExamResult(UUID.randomUUID(), "A1", "Basic", 5, 5, 100, Instant.now(), 90, List.of())
                .retentionText()).isEqualTo("90분");
    }

}

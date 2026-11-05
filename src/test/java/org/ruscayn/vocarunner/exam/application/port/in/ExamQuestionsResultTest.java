package org.ruscayn.vocarunner.exam.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ruscayn.vocarunner.support.TestWords.catalogOf;
import static org.ruscayn.vocarunner.support.TestWords.questions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.Question;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;

@DisplayName("ExamQuestionsResult : 문항 전달 결과 만들기")
class ExamQuestionsResultTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");

    @Test
    @DisplayName("문항을 제시어 · 보기 글로 바꾸고 허용 분 · 남은 초를 담는다 (정답 보기 번호는 담지 않음)")
    void of() {
        List<Question> questions = questions(2);
        Exam exam = Exam.start(Level.A1, questions, new Examinee("device-1", "10.0.0.1"), NOW, Duration.ofMinutes(3));

        ExamQuestionsResult result = ExamQuestionsResult.of(exam, catalogOf(questions), NOW.plusSeconds(30));

        assertThat(result.examId()).isEqualTo(exam.getId());
        assertThat(result.level()).isEqualTo("A1");
        assertThat(result.groupName()).isEqualTo("Basic");
        assertThat(result.direction()).isEqualTo("EN_KO");
        assertThat(result.questionCount()).isEqualTo(2);
        assertThat(result.timePerQuestionSec()).isEqualTo(Exam.DEFAULT_TIME_PER_QUESTION_SEC);
        assertThat(result.allowedMinutes()).isEqualTo(4);   // 2문항 × 5초 + 3분 → 4분
        assertThat(result.remainingSec()).isEqualTo(4 * 60 - 30);
        ExamQuestionsResult.QuestionItem first = result.questions().get(0);
        assertThat(first.no()).isEqualTo(1);
        assertThat(first.prompt()).isEqualTo(questions.get(0).answer().headword());
        assertThat(first.options()).extracting(ExamQuestionsResult.Option::no).containsExactly(1, 2, 3, 4);
        assertThat(first.options()).extracting(ExamQuestionsResult.Option::text)
                .containsExactlyElementsOf(questions.get(0).options().stream().map(CatalogWord::optionText).toList());
    }

}

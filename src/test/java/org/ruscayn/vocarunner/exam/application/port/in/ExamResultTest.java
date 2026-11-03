package org.ruscayn.vocarunner.exam.application.port.in;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExamResult : 결과 화면 집계")
class ExamResultTest {

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

package org.ruscayn.vocarunner.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("GradingResult : 채점 결과 (맞힌 수 · 점수 · 제출 시각)")
class GradingResultTest {

    private static final Instant SUBMITTED_AT = Instant.parse("2026-10-08T00:00:00Z");

    @ParameterizedTest(name = "{1}문항 중 {0}개 → {2}점")
    @CsvSource({
            "2,   3,   66",    // 66.6 → 66 (버림)
            "995, 997, 99",    // 99.8 → 99, 하나라도 틀리면 100점이 아님 (U1)
            "5,   5,   100",
            "0,   4,   0"
    })
    @DisplayName("점수는 버림")
    void scoreFloor(int correctCount, int questionCount, int expectedScore) {
        GradingResult gradingResult = GradingResult.of(correctCount, questionCount, SUBMITTED_AT);

        assertThat(gradingResult.correctCount()).isEqualTo(correctCount);
        assertThat(gradingResult.score()).isEqualTo(expectedScore);
        assertThat(gradingResult.submittedAt()).isEqualTo(SUBMITTED_AT);
    }

    @Test
    @DisplayName("보관 마감 = 제출 시각 + 결과 보관")
    void retentionDeadline() {
        GradingResult gradingResult = GradingResult.of(1, 2, SUBMITTED_AT);

        assertThat(gradingResult.retentionDeadline(Duration.ofHours(1))).isEqualTo(SUBMITTED_AT.plusSeconds(3600));
    }

}

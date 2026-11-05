package org.ruscayn.vocarunner.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("GradingResult : 채점 결과 (맞힌 수 · 점수)")
class GradingResultTest {

    @ParameterizedTest(name = "{1}문항 중 {0}개 → {2}점")
    @CsvSource({
            "2,   3,   66",    // 66.6 → 66 (버림)
            "995, 997, 99",    // 99.8 → 99, 하나라도 틀리면 100점이 아님 (U1)
            "5,   5,   100",
            "0,   4,   0"
    })
    @DisplayName("점수는 버림")
    void scoreFloor(int correctCount, int questionCount, int expectedScore) {
        GradingResult gradingResult = GradingResult.of(correctCount, questionCount);

        assertThat(gradingResult.correctCount()).isEqualTo(correctCount);
        assertThat(gradingResult.score()).isEqualTo(expectedScore);
    }

}

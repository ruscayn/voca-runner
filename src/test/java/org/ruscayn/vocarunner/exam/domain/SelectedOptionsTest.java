package org.ruscayn.vocarunner.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;

@DisplayName("SelectedOptions : 선택한 보기 기록")
class SelectedOptionsTest {

    private static final int QUESTION_COUNT = 3;

    private static ErrorCode errorCodeOf(List<SelectedOption> selected) {
        Throwable thrown = catchThrowable(() -> SelectedOptions.record(selected, QUESTION_COUNT));
        return ((CustomException) thrown).getErrorCode();
    }

    @Test
    @DisplayName("문항마다 선택한 보기 번호를 남긴다 (시간 초과 · 항목 없음 = 0)")
    void records() {
        SelectedOptions selectedOptions = SelectedOptions.record(
                List.of(new SelectedOption(1, 3), new SelectedOption(2, null)), QUESTION_COUNT);

        assertThat(selectedOptions.toSelectedOptionNoArray()).containsExactly(3, 0, 0);
        assertThat(selectedOptions.isSelected(1)).isTrue();
        assertThat(selectedOptions.isSelected(2)).isFalse();
        assertThat(selectedOptions.isSelected(3)).isFalse();
    }

    @ParameterizedTest(name = "문제 번호 {0}")
    @CsvSource({"0", "4"})
    @DisplayName("없는 문제 번호면 INVALID_INPUT_VALUE")
    void nonexistentQuestionNo(int no) {
        assertThatThrownBy(() -> SelectedOptions.record(List.of(new SelectedOption(no, 1)), QUESTION_COUNT))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("없는 문제 번호")
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @Test
    @DisplayName("같은 문제 번호가 두 번 오면 INVALID_INPUT_VALUE (시간 초과 항목도 셈)")
    void duplicatedQuestionNo() {
        assertThatThrownBy(() -> SelectedOptions.record(
                List.of(new SelectedOption(2, null), new SelectedOption(2, 1)), QUESTION_COUNT))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("중복된 문제 번호")
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ParameterizedTest(name = "보기 번호 {0}")
    @CsvSource({"0", "5", "-1"})
    @DisplayName("보기 번호가 1~4가 아니면 EXAM_INVALID_OPTION")
    void invalidOption(int optionNo) {
        assertThat(errorCodeOf(List.of(new SelectedOption(1, optionNo)))).isEqualTo(ErrorCode.EXAM_INVALID_OPTION);
    }

    @Test
    @DisplayName("문제 번호를 먼저 모두 검사한다 (앞 항목의 보기 번호가 틀려도 뒤의 없는 번호를 먼저 알림)")
    void questionNosFirst() {
        List<SelectedOption> selected = List.of(new SelectedOption(1, 9), new SelectedOption(QUESTION_COUNT + 1, 1));

        assertThat(errorCodeOf(selected)).isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

}

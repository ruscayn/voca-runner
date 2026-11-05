package org.ruscayn.vocarunner.exam.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;

/** 문항 n(1부터)의 선택한 보기 번호 = selectedOptionNo[n-1] (1~4, 0 = 선택하지 않음 · 시간 초과) */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class SelectedOptions {

    private static final int NONE = 0;

    private final byte[] selectedOptionNo;

    static SelectedOptions none(int questionCount) {
        return new SelectedOptions(new byte[questionCount]);
    }

    public static SelectedOptions of(byte[] selectedOptionNo) {
        return new SelectedOptions(selectedOptionNo.clone());
    }

    static SelectedOptions record(List<SelectedOption> selected, int questionCount) {
        requireValidQuestionNos(selected, questionCount);
        selected.forEach(SelectedOptions::requireValidOption);
        byte[] selectedOptionNo = new byte[questionCount];
        selected.forEach(option ->
                selectedOptionNo[Question.index(option.questionNo(), questionCount)] = toByte(option.optionNo()));
        return new SelectedOptions(selectedOptionNo);
    }

    private static void requireValidQuestionNos(List<SelectedOption> selected, int questionCount) {
        Set<Integer> questionNos = new HashSet<>();
        for (SelectedOption option : selected) {
            requireExistingQuestionNo(option.questionNo(), questionCount);
            requireNotDuplicated(questionNos, option.questionNo());
        }
    }

    private static void requireExistingQuestionNo(int no, int questionCount) {
        if (no < 1 || no > questionCount) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "없는 문제 번호입니다: " + no);
        }
    }

    private static void requireNotDuplicated(Set<Integer> questionNos, int no) {
        if (!questionNos.add(no)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "중복된 문제 번호입니다: " + no);
        }
    }

    private static void requireValidOption(SelectedOption option) {
        Integer optionNo = option.optionNo();
        if (optionNo != null && (optionNo < 1 || optionNo > Question.OPTION_COUNT)) {
            throw new CustomException(ErrorCode.EXAM_INVALID_OPTION,
                    "문제 " + option.questionNo() + "의 보기 번호가 올바르지 않습니다: " + optionNo);
        }
    }

    private static byte toByte(Integer optionNo) {
        return optionNo == null ? NONE : optionNo.byteValue();
    }

    int questionCount() {
        return selectedOptionNo.length;
    }

    boolean isSelected(int no) {
        return optionNo(no) != NONE;
    }

    int optionNo(int no) {
        return selectedOptionNo[Question.index(no, questionCount())];
    }

    public byte[] toSelectedOptionNoArray() {
        return selectedOptionNo.clone();
    }

}

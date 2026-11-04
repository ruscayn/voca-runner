package org.ruscayn.vocarunner.exam.domain;

import java.util.Map;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;

/** 문항 n(1부터)의 고른 보기 번호 = selectedOptionNo[n-1] (1~4, 0 = 고르지 않음 · 시간 초과) */
public final class SelectedOptions {

    private static final int NONE = 0;

    private final byte[] selectedOptionNo;

    private SelectedOptions(byte[] selectedOptionNo) {
        this.selectedOptionNo = selectedOptionNo;
    }

    static SelectedOptions none(int questionCount) {
        return new SelectedOptions(new byte[questionCount]);
    }

    public static SelectedOptions of(byte[] selectedOptionNo) {
        return new SelectedOptions(selectedOptionNo.clone());
    }

    static SelectedOptions record(Map<Integer, Integer> selectedByQuestionNo, int questionCount) {
        selectedByQuestionNo.forEach(SelectedOptions::requireValidOption);
        byte[] selected = new byte[questionCount];
        for (int q = 0; q < questionCount; q++) {
            Integer optionNo = selectedByQuestionNo.get(q + 1);
            selected[q] = optionNo == null ? NONE : optionNo.byteValue();
        }
        return new SelectedOptions(selected);
    }

    private static void requireValidOption(Integer no, Integer selected) {
        if (selected != null && (selected < 1 || selected > Question.OPTION_COUNT)) {
            throw new CustomException(ErrorCode.EXAM_INVALID_OPTION,
                    "문제 " + no + "의 보기 번호가 올바르지 않습니다: " + selected);
        }
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

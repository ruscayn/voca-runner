package org.ruscayn.vocarunner.exam.application.port.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 시험 제출.
 *
 * @param answers 문항별 고른 보기 번호. 시간 초과 문제는 selectedOptionNo 가 null 이거나 항목이 없어도 된다.
 *                항목 자체가 null 이면 형식 오류 (U44)
 */
public record ExamSubmitRequest(@NotNull List<@NotNull @Valid Answer> answers) {

    public record Answer(
            @NotNull @Min(1) Integer questionNo,
            @Min(1) @Max(4) Integer selectedOptionNo
    ) {
    }

}

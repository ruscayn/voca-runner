package org.ruscayn.myvoca.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 시험 제출.
 *
 * @param answers 문제별 답. 시간 초과 문제는 selectedOptionNo 가 null 이거나 항목이 없어도 된다.
 */
public record TestSubmitRequest(@NotNull @Valid List<Answer> answers) {

    public record Answer(
            @NotNull @Min(1) Integer questionNo,
            @Min(1) @Max(4) Integer selectedOptionNo
    ) {
    }

}

package org.ruscayn.vocarunner.exam.application.port.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.ruscayn.vocarunner.common.SelfValidating;
import org.ruscayn.vocarunner.exam.domain.SelectedOption;

public record SubmitExamCommand(
        @NotNull UUID examId,
        String deviceId,
        @NotNull List<@NotNull @Valid Answer> answers
) {

    public SubmitExamCommand(UUID examId, String deviceId, List<Answer> answers) {
        this.examId = examId;
        this.deviceId = deviceId;
        this.answers = answers;
        SelfValidating.validate(this);
    }

    public List<SelectedOption> toSelectedOptions() {
        return answers.stream()
                .map(answer -> new SelectedOption(answer.questionNo(), answer.selectedOptionNo()))
                .toList();
    }

    public record Answer(
            @NotNull @Min(1) Integer questionNo,
            @Min(1) @Max(4) Integer selectedOptionNo
    ) {

    }

}

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
        @NotNull List<@NotNull @Valid SelectedOptionItem> selectedOptions
) {

    public SubmitExamCommand(UUID examId, String deviceId, List<SelectedOptionItem> selectedOptions) {
        this.examId = examId;
        this.deviceId = deviceId;
        this.selectedOptions = selectedOptions;
        SelfValidating.validate(this);
    }

    public List<SelectedOption> toSelectedOptions() {
        return selectedOptions.stream()
                .map(item -> new SelectedOption(item.questionNo(), item.selectedOptionNo()))
                .toList();
    }

    public record SelectedOptionItem(
            @NotNull @Min(1) Integer questionNo,
            @Min(1) @Max(4) Integer selectedOptionNo
    ) {

    }

}

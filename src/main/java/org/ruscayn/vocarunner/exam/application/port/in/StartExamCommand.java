package org.ruscayn.vocarunner.exam.application.port.in;

import jakarta.validation.constraints.NotNull;
import org.ruscayn.vocarunner.common.SelfValidating;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.word.domain.Level;

public record StartExamCommand(
        @NotNull Level level,
        @NotNull Examinee examinee
) {

    public StartExamCommand(Level level, Examinee examinee) {
        this.level = level;
        this.examinee = examinee;
        SelfValidating.validate(this);
    }

}

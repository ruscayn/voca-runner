package org.ruscayn.vocarunner.exam.application.port.in;

import jakarta.validation.constraints.NotNull;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
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
        requireDetailLevel(level);
    }

    private static void requireDetailLevel(Level level) {
        if (level.isGroup()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "묶음 레벨 시험은 아직 볼 수 없습니다: " + level);
        }
    }

}

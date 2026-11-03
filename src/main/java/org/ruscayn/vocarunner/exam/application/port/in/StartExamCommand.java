package org.ruscayn.vocarunner.exam.application.port.in;

import jakarta.validation.constraints.NotNull;
import org.ruscayn.vocarunner.common.SelfValidating;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.word.domain.Level;

/**
 * 시험 시작 입력. 만들 때 스스로 검증한다 (어기면 400 COMMON_002).
 *
 * @param level    시험 레벨
 * @param examinee 시작한 응시자 (기기 아이디 + 사용자 IP)
 */
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

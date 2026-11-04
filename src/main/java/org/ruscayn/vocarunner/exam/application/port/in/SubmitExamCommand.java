package org.ruscayn.vocarunner.exam.application.port.in;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.ruscayn.vocarunner.common.SelfValidating;

/**
 * 시험 제출 입력. 만들 때 스스로 검증한다 (어기면 400 COMMON_002).
 *
 * @param examId   시험 아이디
 * @param deviceId 제출한 기기 아이디 (쿠키). 없거나 시험을 시작한 기기가 아니면 서비스가 거절한다
 * @param answers  문항별 고른 보기 번호. 시간 초과 문항은 selectedOptionNo 가 null 이거나 항목이 없어도 된다.
 *                 항목 자체가 null 이면 형식 오류 (U44)
 */
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

    /**
     * 문항 하나의 고른 보기.
     *
     * @param questionNo       문항 번호 (1부터)
     * @param selectedOptionNo 고른 보기 번호 1~4. 고르지 못했으면 null
     */
    public record Answer(
            @NotNull @Min(1) Integer questionNo,
            @Min(1) @Max(4) Integer selectedOptionNo
    ) {
    }
}

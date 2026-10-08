package org.ruscayn.vocarunner.application.dto.response;

import java.util.List;
import java.util.UUID;

/** 시험 결과 (틀린 문제 목록 포함) */
public record TestResultResponse(
        UUID sessionId,
        String level,
        String levelGroup,
        int questionCount,
        int correctCount,
        int score,
        List<WrongItem> wrongItems
) {

    /** 시간 초과(답을 고르지 못한) 문제 수 */
    public long timeoutCount() {
        return wrongItems.stream().filter(item -> item.selectedText() == null).count();
    }

    /** 답을 골랐지만 틀린 문제 수 */
    public long wrongCount() {
        return wrongItems.size() - timeoutCount();
    }

    /**
     * 틀린 문제.
     *
     * @param no           문제 번호
     * @param prompt       문제 (EN_KO: 영어 단어)
     * @param selectedText 내가 고른 답. 미입력(시간 초과)이면 null
     * @param answerText   정답
     * @param headword     영어 단어 (TTS 용)
     */
    public record WrongItem(int no, String prompt, String selectedText, String answerText, String headword) {
    }

}

package org.ruscayn.vocarunner.exam.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 시험 결과 (틀린 문제 목록 포함)
 *
 * @param availableUntil   결과를 볼 수 있는 마지막 시각 (제출 시각 + 보관 시간). 지나면 정리 작업이 지운다
 * @param retentionMinutes 결과 보관 시간 (분)
 */
public record TestResultResponse(
        UUID sessionId,
        String level,
        String levelGroup,
        int questionCount,
        int correctCount,
        int score,
        Instant availableUntil,
        long retentionMinutes,
        List<WrongItem> wrongItems
) {

    /** 결과 화면 안내용 보관 시간 (예: 2시간, 90분) */
    public String retentionText() {
        return retentionMinutes % 60 == 0 ? (retentionMinutes / 60) + "시간" : retentionMinutes + "분";
    }

    /** 시간 초과(답을 고르지 못한) 문제 수 */
    public long timeoutCount() {
        return wrongItems.stream().filter(item -> item.selectedText() == null).count();
    }

    /** 답을 골랐지만 틀린 문제 수 */
    public long incorrectCount() {
        return wrongItems.size() - timeoutCount();
    }

    /**
     * 틀린 문제.
     *
     * @param no           문제 번호
     * @param prompt       문제 (EN_KO: 영어 단어)
     * @param promptSub    문제 아래 줄에 함께 표시할 형태. 없으면 null
     * @param selectedText 내가 고른 답. 미입력(시간 초과)이면 null
     * @param answerText   정답
     * @param headword     영어 단어 (TTS 용)
     */
    public record WrongItem(int no, String prompt, String promptSub, String selectedText, String answerText,
                            String headword) {
    }

}

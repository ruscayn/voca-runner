package org.ruscayn.vocarunner.exam.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 시험 결과 (틀린 문제 목록 포함)
 *
 * @param retentionDeadline 보관 마감 = 결과를 볼 수 있는 마지막 시각 (제출 시각 + 보관 시간). 지나면 정리 작업이 지운다
 * @param retentionMinutes  결과 보관 시간 (분)
 */
public record ExamResult(
        UUID examId,
        String level,
        String groupName,
        int questionCount,
        int correctCount,
        int score,
        Instant retentionDeadline,
        long retentionMinutes,
        List<WrongItem> wrongItems
) {

    /** 결과 화면 안내용 보관 시간 (예: 2시간, 90분) */
    public String retentionText() {
        return retentionMinutes % 60 == 0 ? (retentionMinutes / 60) + "시간" : retentionMinutes + "분";
    }

    /** 시간 초과(보기를 고르지 못한) 문항 수 */
    public long timeoutCount() {
        return wrongItems.stream().filter(item -> item.selectedText() == null).count();
    }

    /** 오답 수 = 보기를 골랐지만 정답이 아닌 문항 수 */
    public long incorrectCount() {
        return wrongItems.size() - timeoutCount();
    }

    /**
     * 틀린 문제.
     *
     * @param no           문제 번호
     * @param prompt       문제 (EN_KO: 영어 단어)
     * @param promptSub    제시어 보조 (제시어 아래 줄에 함께 보여 줄 형태). 없으면 null
     * @param selectedText 고른 보기의 글. 고르지 못했으면(시간 초과) null
     * @param answerText   정답
     * @param headword     영어 단어 (TTS 용)
     */
    public record WrongItem(int no, String prompt, String promptSub, String selectedText, String answerText,
                            String headword) {
    }

}

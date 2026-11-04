package org.ruscayn.vocarunner.exam.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    public String retentionText() {
        return retentionMinutes % 60 == 0 ? (retentionMinutes / 60) + "시간" : retentionMinutes + "분";
    }

    public long timeoutCount() {
        return wrongItems.stream().filter(item -> item.selectedText() == null).count();
    }

    public long incorrectCount() {
        return wrongItems.size() - timeoutCount();
    }

    public record WrongItem(int no, String prompt, String promptSub, String selectedText, String answerText,
                            String headword) {

    }

}

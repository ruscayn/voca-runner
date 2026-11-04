package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * @param remainingSec 마감까지 남은 초. 기기 시계 차이를 피하려고 시각이 아닌 남은 시간으로 준다
 */
public record ExamQuestionsResult(
        UUID examId,
        String level,
        String groupName,
        String direction,
        int questionCount,
        int timePerQuestionSec,
        long allowedMinutes,
        long remainingSec,
        List<QuestionItem> questions
) {

    public record QuestionItem(int no, String prompt, String promptSub, List<Option> options) {

    }

    public record Option(int no, String text) {

    }

}

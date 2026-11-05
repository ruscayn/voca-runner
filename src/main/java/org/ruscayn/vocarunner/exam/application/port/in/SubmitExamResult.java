package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.UUID;
import org.ruscayn.vocarunner.exam.domain.Exam;

public record SubmitExamResult(
        UUID examId,
        int questionCount,
        int correctCount,
        int score
) {

    public static SubmitExamResult from(Exam exam) {
        return new SubmitExamResult(exam.getId(), exam.getQuestionCount(), exam.getGradingResult().correctCount(),
                exam.getGradingResult().score());
    }

}

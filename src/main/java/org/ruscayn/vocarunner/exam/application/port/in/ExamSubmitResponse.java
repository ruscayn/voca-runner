package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.UUID;

public record ExamSubmitResponse(
        UUID examId,
        int questionCount,
        int correctCount,
        int score
) {
}

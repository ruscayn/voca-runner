package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.UUID;

public record SubmitExamResult(
        UUID examId,
        int questionCount,
        int correctCount,
        int score
) {
}

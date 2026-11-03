package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.UUID;

public record TestSubmitResponse(
        UUID sessionId,
        int questionCount,
        int correctCount,
        int score
) {
}

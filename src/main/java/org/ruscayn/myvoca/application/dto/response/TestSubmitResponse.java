package org.ruscayn.myvoca.application.dto.response;

import java.util.UUID;

public record TestSubmitResponse(
        UUID sessionId,
        int questionCount,
        int correctCount,
        int score
) {
}

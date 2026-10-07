package org.ruscayn.myvoca.application.dto.response;

import java.util.List;
import java.util.UUID;

public record TestQuestionsResponse(
        UUID sessionId,
        String level,
        String direction,
        int questionCount,
        int timeLimitSec,
        List<TestQuestionView> questions
) {
}

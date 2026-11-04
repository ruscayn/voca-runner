package org.ruscayn.vocarunner.exam.application.service;

import java.time.Duration;

public record ExamProperties(
        int maxQuestionCount,
        Duration grace,
        Duration resultRetention,
        int maxInProgress,
        int maxInProgressPerIp,
        Duration undeliveredTimeout
) {

    public ExamProperties {
        if (maxQuestionCount < 0) {
            throw new IllegalArgumentException(
                    "voca-runner.test.question-count 는 0(전체) 이상이어야 합니다: " + maxQuestionCount);
        }
    }
}

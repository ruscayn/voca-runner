package org.ruscayn.vocarunner.config;

import java.time.Duration;
import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

@ConfigurationProperties(prefix = "voca-runner.test")
public record ExamConfigProperties(
        @Name("question-count") @DefaultValue("500") int maxQuestionCount,
        @DefaultValue("3m") Duration grace,
        @DefaultValue("1h") Duration resultRetention,
        @DefaultValue("20000") int maxInProgress,
        @DefaultValue("50") int maxInProgressPerIp,
        @DefaultValue("5m") Duration undeliveredTimeout
) {

    public ExamProperties toExamProperties() {
        return new ExamProperties(maxQuestionCount, grace, resultRetention, maxInProgress, maxInProgressPerIp,
                undeliveredTimeout);
    }
}

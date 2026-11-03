package org.ruscayn.vocarunner.config;

import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

import java.time.Duration;

/**
 * 시험 설정 묶기 (application.yaml : voca-runner.test.*). 값의 뜻은 ExamProperties 참고.
 * 자바 이름(maxQuestionCount)과 설정 이름(question-count)이 달라 @Name 으로 묶는다.
 *
 * @param cleanupInterval 정리 주기. 정리 작업(ExamCleanupScheduler)이 @Scheduled 에서 설정 이름으로 직접 읽는다.
 *                        설정 이름 · 기본값을 한곳에 모아 두려고 함께 묶는다 (서비스에는 넘기지 않음)
 */
@ConfigurationProperties(prefix = "voca-runner.test")
public record ExamConfigProperties(
        @Name("question-count") @DefaultValue("500") int maxQuestionCount,
        @DefaultValue("3m") Duration grace,
        @DefaultValue("1h") Duration resultRetention,
        @DefaultValue("20000") int maxInProgress,
        @DefaultValue("50") int maxInProgressPerIp,
        @DefaultValue("1m") Duration cleanupInterval,
        @DefaultValue("5m") Duration undeliveredTimeout
) {

    /** 서비스가 받는 순수 값으로 */
    public ExamProperties toExamProperties() {
        return new ExamProperties(maxQuestionCount, grace, resultRetention, maxInProgress, maxInProgressPerIp,
                undeliveredTimeout);
    }
}

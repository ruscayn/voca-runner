package org.ruscayn.vocarunner.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * 시험 운영 설정 (application.yaml : voca-runner.test.*)
 *
 * @param questionCount        문항 수
 * @param grace                마감 여유 시간 (문항 수 × 제한 시간 + 여유)
 * @param resultRetention      제출 후 결과를 메모리에 보관하는 시간
 * @param maxInProgress        전체 동시 진행 상한. 넘으면 새 시작 거절
 * @param maxInProgressPerClient 사용자(IP)당 동시 진행 상한
 * @param cleanupInterval      만료 시험 정리 주기
 */
@ConfigurationProperties(prefix = "voca-runner.test")
public record TestProperties(
        @DefaultValue("500") int questionCount,
        @DefaultValue("3m") Duration grace,
        @DefaultValue("10m") Duration resultRetention,
        @DefaultValue("20000") int maxInProgress,
        @DefaultValue("3") int maxInProgressPerClient,
        @DefaultValue("1m") Duration cleanupInterval
) {
}

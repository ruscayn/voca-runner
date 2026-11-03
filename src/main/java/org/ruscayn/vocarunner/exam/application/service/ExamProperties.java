package org.ruscayn.vocarunner.exam.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

import java.time.Duration;

/**
 * 시험 운영 설정 (application.yaml : voca-runner.test.*)
 *
 * @param maxQuestionCount     최대 문항 수. 레벨의 출제 가능 단어가 더 적으면 그 레벨 단어 전체를 출제.
 *                             0 이면 레벨 단어 전체를 출제 (개발·베타 테스트용, 운영은 500)
 * @param grace                마감 여유 시간. 마감 = (문항 수 × 제한 시간 + 여유)를 분 단위로 올림
 * @param resultRetention      제출 후 결과를 보관하는 시간. 지나면 정리 작업이 지운다
 * @param maxInProgress        전체 동시 진행 상한. 넘으면 새 시작 거절
 * @param maxInProgressPerIp   IP당 동시 진행 상한. 학교·통신사처럼 여러 사람이 IP 하나를 함께 쓰므로 넉넉하게.
 *                             쿠키를 지워 가며 시험을 계속 만드는 것만 막는다 (기기당은 1개, 새로 시작하면 이전 시험 종료)
 * @param cleanupInterval      만료 시험 정리 주기
 * @param undeliveredTimeout   시작 후 이 시간 안에 문제를 받아 가지 않은 시험은 정리 (시작 직후 연결 끊김 등)
 */
@ConfigurationProperties(prefix = "voca-runner.test")
public record ExamProperties(
        @Name("question-count") @DefaultValue("500") int maxQuestionCount,
        @DefaultValue("3m") Duration grace,
        @DefaultValue("1h") Duration resultRetention,
        @DefaultValue("20000") int maxInProgress,
        @DefaultValue("50") int maxInProgressPerIp,
        @DefaultValue("1m") Duration cleanupInterval,
        @DefaultValue("5m") Duration undeliveredTimeout
) {

    public ExamProperties {
        if (maxQuestionCount < 0) {
            throw new IllegalArgumentException("voca-runner.test.question-count 는 0(전체) 이상이어야 합니다: " + maxQuestionCount);
        }
    }
}

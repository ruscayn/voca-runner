package org.ruscayn.vocarunner.exam.application.service;

import java.time.Duration;

/**
 * 시험 운영 설정 값 (서비스가 받는 순수 값). 설정 파일(voca-runner.test.*)은 config 의 ExamConfigProperties 가 묶어 넘긴다.
 *
 * @param maxQuestionCount     최대 문항 수. 레벨의 정답 후보가 더 적으면 그 레벨 단어 전체를 출제.
 *                             0 이면 레벨 단어 전체를 출제 (개발·베타 테스트용, 운영은 500)
 * @param grace                마감 여유 시간. 마감 = (문항 수 × 문항당 시간 + 여유)를 분 단위로 올림
 * @param resultRetention      제출 후 결과를 보관하는 시간. 지나면 정리 작업이 지운다
 * @param maxInProgress        최대 진행 중 = 전체 진행 중 시험 수의 최대. 넘으면 새 시작 거절
 * @param maxInProgressPerIp   IP당 최대 진행 중. 학교·통신사처럼 여러 사람이 IP 하나를 함께 쓰므로 넉넉하게.
 *                             쿠키를 지워 가며 시험을 계속 만드는 것만 막는다 (기기당은 1개, 새로 시작하면 이전 시험 중단)
 * @param undeliveredTimeout   시작 후 이 시간 안에 문제를 받아 가지 않은 시험은 정리 (시작 직후 연결 끊김 등)
 */
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

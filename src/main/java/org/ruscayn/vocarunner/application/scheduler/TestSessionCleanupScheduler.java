package org.ruscayn.vocarunner.application.scheduler;

import org.ruscayn.vocarunner.application.config.TestProperties;
import org.ruscayn.vocarunner.domain.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 메모리의 끝난 시험 정리.
 * 마감이 지난 진행 중 시험, 시작 후 문제를 받아 가지 않은 시험, 보관 시간이 지난 결과, 무효 처리된 시험을 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestSessionCleanupScheduler {

    private final TestSessionRepository testSessionRepository;
    private final TestProperties testProperties;

    @Scheduled(fixedDelayString = "${voca-runner.test.cleanup-interval:1m}")
    public void cleanup() {
        Instant now = Instant.now();
        int removed = testSessionRepository.removeIf(
                session -> session.isDisposable(now, testProperties.resultRetention(), testProperties.undeliveredTimeout()));
        if (removed > 0) {
            log.info("시험 정리: {}개 삭제, 남은 {}개 (진행 중 {}개)",
                    removed, testSessionRepository.size(), testSessionRepository.countInProgress(now));
        }
    }

}

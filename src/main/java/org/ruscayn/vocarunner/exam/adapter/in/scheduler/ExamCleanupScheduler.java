package org.ruscayn.vocarunner.exam.adapter.in.scheduler;

import org.ruscayn.vocarunner.exam.application.port.out.ExamRepository;
import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 끝난 시험 정리 (test_session 삭제).
 * 마감이 지난 진행 중 시험, 시작 후 문제를 받아 가지 않은 시험, 보관 시간이 지난 결과, 무효 처리된 시험을 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamCleanupScheduler {

    private final ExamRepository examRepository;
    private final ExamProperties examProperties;

    @Scheduled(fixedDelayString = "${voca-runner.test.cleanup-interval:1m}")
    public void cleanup() {
        Instant now = Instant.now();
        int removed = examRepository.deleteDisposable(
                now, examProperties.resultRetention(), examProperties.undeliveredTimeout());
        if (removed > 0) {
            log.info("시험 정리: {}개 삭제, 남은 {}개 (진행 중 {}개)",
                    removed, examRepository.count(), examRepository.countInProgress(now));
        }
    }

}

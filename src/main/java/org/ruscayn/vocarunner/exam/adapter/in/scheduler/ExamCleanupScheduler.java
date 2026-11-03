package org.ruscayn.vocarunner.exam.adapter.in.scheduler;

import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 끝난 시험 정리 (test_session 삭제). 정리 주기마다 시험 기능의 정리(ExamUseCase.cleanup)를 부른다.
 */
@Component
@RequiredArgsConstructor
public class ExamCleanupScheduler {

    private final ExamUseCase examUseCase;

    @Scheduled(fixedDelayString = "${voca-runner.test.cleanup-interval:1m}")
    public void cleanup() {
        examUseCase.cleanup();
    }

}

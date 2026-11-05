package org.ruscayn.vocarunner.exam.adapter.in.scheduler;

import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.exam.application.port.in.CleanUpExamsUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExamCleanupScheduler {

    private final CleanUpExamsUseCase cleanUpExamsUseCase;

    @Scheduled(fixedDelayString = "${voca-runner.exam.cleanup-interval:1m}")
    public void cleanup() {
        cleanUpExamsUseCase.cleanUp();
    }

}

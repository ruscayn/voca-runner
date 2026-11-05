package org.ruscayn.vocarunner.exam.application.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.exam.application.port.in.CleanUpExamsUseCase;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CleanUpExamsService implements CleanUpExamsUseCase {

    private final ExamPersistencePort examPersistencePort;
    private final ExamProperties examProperties;

    @Override
    public void cleanUp() {
        Instant now = Instant.now();
        int removed = examPersistencePort.deleteDisposable(
                now, examProperties.resultRetention(), examProperties.undeliveredTimeout());
        if (removed > 0) {
            log.info("시험 정리: {}개 삭제, 남은 {}개 (진행 중 {}개)",
                    removed, examPersistencePort.count(), examPersistencePort.countInProgress(now));
        }
    }

}

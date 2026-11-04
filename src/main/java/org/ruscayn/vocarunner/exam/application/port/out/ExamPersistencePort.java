package org.ruscayn.vocarunner.exam.application.port.out;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ruscayn.vocarunner.exam.domain.Exam;

public interface ExamPersistencePort {

    void save(Exam exam);

    Optional<Exam> findById(UUID examId);

    void delete(UUID examId);

    void lockForStart();

    int countInProgress(Instant now);

    List<Exam> findInProgressByDevice(String deviceId, Instant now);

    int countInProgressByIp(String ip, Instant now);

    int deleteDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout);

    long count();

}

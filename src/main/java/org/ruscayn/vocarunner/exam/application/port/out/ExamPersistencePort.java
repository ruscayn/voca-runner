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

    /**
     * 진행 중 시험은 마감이 지나면, 완료한 시험은 결과 보관 시간이 지나면, 만료된 시험은 바로 지운다.
     * 문제를 받아 가지 않은 시험은 연결이 끊겨 중단 요청도 못 보냈을 수 있어, 마감까지 두면 동시 진행 수를 차지하므로
     * undeliveredTimeout 이 지나면 지운다
     */
    int deleteDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout);

    long count();

}

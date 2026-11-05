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
     * 진행 중 시험은 마감이 지나면, 완료한 시험은 결과 보관 시간이 지나면 지운다.
     * 문항을 전달하지 못한 시험은 연결이 끊겨 중단 요청도 못 보냈을 수 있어, 마감까지 두면 진행 중 시험 수를 차지하므로
     * 미전달 시간 초과(undeliveredTimeout)가 지나면 지운다
     */
    int deleteDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout);

    long count();

}

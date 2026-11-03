package org.ruscayn.vocarunner.exam.application.port.out;

import org.ruscayn.vocarunner.exam.domain.Exam;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 진행 중·결과 보관 중인 시험 저장소 (test_session).
 */
public interface TestSessionRepository {

    void save(Exam session);

    Optional<Exam> findById(UUID sessionId);

    void delete(UUID sessionId);

    /**
     * 시험 시작 잠금 (트랜잭션이 끝날 때 풀림).
     * 상한 확인과 저장 사이에 다른 시작 요청이 끼어들어 상한을 넘기지 않도록 시작을 한 번에 하나씩 처리한다 (B3)
     */
    void lockForStart();

    /** 진행 중인 시험 수 (전체) */
    int countInProgress(Instant now);

    /** 진행 중인 시험 (기기 기준) */
    List<Exam> findInProgressByDevice(String deviceId, Instant now);

    /** 진행 중인 시험 수 (IP 기준) */
    int countInProgressByIp(String ip, Instant now);

    /**
     * 끝난 시험 삭제. 삭제한 개수 반환. 조건은 Exam.isDisposable 과 같다
     * (마감이 지난 진행 중 시험, 문제를 받아 가지 않은 채 undeliveredTimeout 이 지난 시험, 보관 시간이 지난 결과, 무효)
     */
    int deleteDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout);

    /** 보관 중인 전체 시험 수 (로그용) */
    long count();

}

package org.ruscayn.myvoca.domain.repository;

import org.ruscayn.myvoca.domain.model.test.TestSession;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 진행 중·결과 보관 중인 시험 저장소.
 * 현재 구현은 서버 메모리. 서버가 여러 대가 되면 Redis 등 공용 저장소 구현으로 교체한다.
 */
public interface TestSessionRepository {

    void save(TestSession session);

    Optional<TestSession> findById(UUID sessionId);

    void delete(UUID sessionId);

    /** 진행 중인 시험 수 (전체) */
    int countInProgress(Instant now);

    /** 진행 중인 시험 수 (사용자 구분 값 기준) */
    int countInProgress(String clientKey, Instant now);

    /** 조건에 맞는 시험 삭제. 삭제한 개수 반환 */
    int removeIf(Predicate<TestSession> condition);

    /** 보관 중인 전체 시험 수 (로그용) */
    int size();

}

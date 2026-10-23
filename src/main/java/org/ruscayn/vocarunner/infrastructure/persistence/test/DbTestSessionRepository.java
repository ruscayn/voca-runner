package org.ruscayn.vocarunner.infrastructure.persistence.test;

import org.ruscayn.vocarunner.domain.model.test.TestSession;
import org.ruscayn.vocarunner.domain.model.test.TestStatus;
import org.ruscayn.vocarunner.domain.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 시험 저장소 (PostgreSQL test_session).
 * 진행 중 = 상태 IN_PROGRESS 이고 마감 전 (TestSession.isInProgress 와 같은 조건)
 */
@Repository
@RequiredArgsConstructor
public class DbTestSessionRepository implements TestSessionRepository {

    private final JpaTestSessionRepository jpa;

    @Override
    public void save(TestSession session) {
        jpa.save(session);
    }

    @Override
    public Optional<TestSession> findById(UUID sessionId) {
        return jpa.findById(sessionId);
    }

    @Override
    public void delete(UUID sessionId) {
        jpa.deleteById(sessionId);
    }

    @Override
    public void lockForStart() {
        jpa.advisoryLock(JpaTestSessionRepository.START_LOCK_KEY);
    }

    @Override
    public int countInProgress(Instant now) {
        return Math.toIntExact(jpa.countActive(TestStatus.IN_PROGRESS, now));
    }

    @Override
    public List<TestSession> findInProgressByDevice(String deviceId, Instant now) {
        return jpa.findActiveByDevice(TestStatus.IN_PROGRESS, now, deviceId);
    }

    @Override
    public int countInProgressByIp(String ip, Instant now) {
        return Math.toIntExact(jpa.countActiveByIp(TestStatus.IN_PROGRESS, now, ip));
    }

    @Override
    @Transactional
    public int deleteDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout) {
        return jpa.deleteDisposable(now, now.minus(undeliveredTimeout), now.minus(resultRetention));
    }

    @Override
    public long count() {
        return jpa.count();
    }

}

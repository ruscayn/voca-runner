package org.ruscayn.vocarunner.infrastructure.memory;

import org.ruscayn.vocarunner.domain.model.test.TestSession;
import org.ruscayn.vocarunner.domain.repository.TestSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 시험 메모리 저장소 (서버 1대 전제).
 *
 * <p>개수 조회는 전체를 훑는다. 상한(수만 개) 규모에서 1ms 내외라 별도 카운터를 두지 않는다.
 * 동시 시작 요청의 상한 확인·저장은 TestApplicationService 가 잠금으로 묶는다.
 */
@Repository
public class InMemoryTestSessionRepository implements TestSessionRepository {

    private final Map<UUID, TestSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(TestSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public Optional<TestSession> findById(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void delete(UUID sessionId) {
        sessions.remove(sessionId);
    }

    @Override
    public int countInProgress(Instant now) {
        int count = 0;
        for (TestSession session : sessions.values()) {
            if (session.isInProgress(now)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public List<TestSession> findInProgressByDevice(String deviceId, Instant now) {
        List<TestSession> found = new ArrayList<>();
        for (TestSession session : sessions.values()) {
            if (session.isInProgress(now) && deviceId.equals(session.getClient().deviceId())) {
                found.add(session);
            }
        }
        return found;
    }

    @Override
    public int countInProgressByIp(String ip, Instant now) {
        int count = 0;
        for (TestSession session : sessions.values()) {
            if (session.isInProgress(now) && ip.equals(session.getClient().ip())) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int removeIf(Predicate<TestSession> condition) {
        int before = sessions.size();
        sessions.values().removeIf(condition);
        return Math.max(0, before - sessions.size());
    }

    @Override
    public int size() {
        return sessions.size();
    }

}

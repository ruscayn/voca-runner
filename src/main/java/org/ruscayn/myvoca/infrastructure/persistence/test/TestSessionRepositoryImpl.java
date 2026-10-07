package org.ruscayn.myvoca.infrastructure.persistence.test;

import org.ruscayn.myvoca.domain.model.test.TestSession;
import org.ruscayn.myvoca.domain.repository.TestSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TestSessionRepositoryImpl implements TestSessionRepository {

    private final JpaTestSessionRepository jpaTestSessionRepository;

    @Override
    public TestSession save(TestSession session) {
        return jpaTestSessionRepository.save(session);
    }

    @Override
    public Optional<TestSession> findById(UUID sessionId) {
        return jpaTestSessionRepository.findById(sessionId);
    }

    @Override
    public Optional<TestSession> findByIdWithQuestions(UUID sessionId) {
        return jpaTestSessionRepository.findByIdWithQuestions(sessionId);
    }

}

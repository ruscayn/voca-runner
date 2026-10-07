package org.ruscayn.myvoca.domain.repository;

import org.ruscayn.myvoca.domain.model.test.TestSession;

import java.util.Optional;
import java.util.UUID;

public interface TestSessionRepository {

    TestSession save(TestSession session);

    Optional<TestSession> findById(UUID sessionId);

    /** 문제까지 함께 조회 (채점·결과 화면용) */
    Optional<TestSession> findByIdWithQuestions(UUID sessionId);

}

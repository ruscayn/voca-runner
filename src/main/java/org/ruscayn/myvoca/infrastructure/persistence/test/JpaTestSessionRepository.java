package org.ruscayn.myvoca.infrastructure.persistence.test;

import org.ruscayn.myvoca.domain.model.test.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaTestSessionRepository extends JpaRepository<TestSession, UUID> {

    @Query("select s from TestSession s left join fetch s.questions where s.id = :id")
    Optional<TestSession> findByIdWithQuestions(@Param("id") UUID id);

}

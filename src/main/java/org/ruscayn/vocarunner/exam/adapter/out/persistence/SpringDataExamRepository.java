package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataExamRepository extends JpaRepository<ExamJpaEntity, UUID> {

    long START_LOCK_KEY = 7_726_001L;

    @Query(value = "select 1 from (select pg_advisory_xact_lock(:key)) l", nativeQuery = true)
    Integer advisoryLock(@Param("key") long key);

    @Query("""
            select count(s) from ExamJpaEntity s
             where s.status = :status and s.deadline >= :now
            """)
    long countActive(@Param("status") ExamStatus status, @Param("now") Instant now);

    @Query("""
            select s from ExamJpaEntity s
             where s.status = :status and s.deadline >= :now and s.deviceId = :deviceId
            """)
    List<ExamJpaEntity> findActiveByDevice(@Param("status") ExamStatus status, @Param("now") Instant now,
                                           @Param("deviceId") String deviceId);

    @Query("""
            select count(s) from ExamJpaEntity s
             where s.status = :status and s.deadline >= :now and s.clientIp = :ip
            """)
    long countActiveByIp(@Param("status") ExamStatus status, @Param("now") Instant now, @Param("ip") String ip);

    @Modifying
    @Query("""
            delete from ExamJpaEntity s
             where (s.status = org.ruscayn.vocarunner.exam.domain.ExamStatus.IN_PROGRESS
                    and (s.deadline < :now
                         or (s.questionsDelivered = false and s.startedAt < :undeliveredBefore)))
                or (s.status = org.ruscayn.vocarunner.exam.domain.ExamStatus.COMPLETED
                    and s.submittedAt < :completedBefore)
            """)
    int deleteDisposable(@Param("now") Instant now, @Param("undeliveredBefore") Instant undeliveredBefore,
                         @Param("completedBefore") Instant completedBefore);

}

package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** test_session Spring Data 저장소. 도메인 저장소(ExamRepository) 구현에서만 쓴다 */
public interface JpaExamRepository extends JpaRepository<Exam, UUID> {

    /** 시험 시작 잠금 키 (pg_advisory_xact_lock). 다른 용도의 잠금과 겹치지 않는 임의의 값 */
    long START_LOCK_KEY = 7_726_001L;

    /** 트랜잭션 잠금. 다른 트랜잭션이 같은 키를 잡고 있으면 끝날 때까지 기다린다 (반환값은 의미 없음) */
    @Query(value = "select 1 from (select pg_advisory_xact_lock(:key)) l", nativeQuery = true)
    Integer advisoryLock(@Param("key") long key);

    @Query("""
            select count(s) from Exam s
             where s.status = :status and s.deadline >= :now
            """)
    long countActive(@Param("status") ExamStatus status, @Param("now") Instant now);

    @Query("""
            select s from Exam s
             where s.status = :status and s.deadline >= :now and s.deviceId = :deviceId
            """)
    List<Exam> findActiveByDevice(@Param("status") ExamStatus status, @Param("now") Instant now,
                                         @Param("deviceId") String deviceId);

    @Query("""
            select count(s) from Exam s
             where s.status = :status and s.deadline >= :now and s.clientIp = :ip
            """)
    long countActiveByIp(@Param("status") ExamStatus status, @Param("now") Instant now, @Param("ip") String ip);

    @Modifying
    @Query("""
            delete from Exam s
             where s.status = org.ruscayn.vocarunner.exam.domain.ExamStatus.EXPIRED
                or (s.status = org.ruscayn.vocarunner.exam.domain.ExamStatus.IN_PROGRESS
                    and (s.deadline < :now
                         or (s.questionsDelivered = false and s.startedAt < :undeliveredBefore)))
                or (s.status = org.ruscayn.vocarunner.exam.domain.ExamStatus.COMPLETED
                    and s.submittedAt < :completedBefore)
            """)
    int deleteDisposable(@Param("now") Instant now, @Param("undeliveredBefore") Instant undeliveredBefore,
                         @Param("completedBefore") Instant completedBefore);

}

package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import org.ruscayn.vocarunner.exam.application.port.out.ExamRepository;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
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
 * 진행 중 = 상태 IN_PROGRESS 이고 마감 전 (Exam.isInProgress 와 같은 조건)
 */
@Repository
@RequiredArgsConstructor
public class DbExamRepository implements ExamRepository {

    private final JpaExamRepository jpa;

    @Override
    public void save(Exam exam) {
        jpa.save(exam);
    }

    @Override
    public Optional<Exam> findById(UUID examId) {
        return jpa.findById(examId);
    }

    @Override
    public void delete(UUID examId) {
        jpa.deleteById(examId);
    }

    @Override
    public void lockForStart() {
        jpa.advisoryLock(JpaExamRepository.START_LOCK_KEY);
    }

    @Override
    public int countInProgress(Instant now) {
        return Math.toIntExact(jpa.countActive(ExamStatus.IN_PROGRESS, now));
    }

    @Override
    public List<Exam> findInProgressByDevice(String deviceId, Instant now) {
        return jpa.findActiveByDevice(ExamStatus.IN_PROGRESS, now, deviceId);
    }

    @Override
    public int countInProgressByIp(String ip, Instant now) {
        return Math.toIntExact(jpa.countActiveByIp(ExamStatus.IN_PROGRESS, now, ip));
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

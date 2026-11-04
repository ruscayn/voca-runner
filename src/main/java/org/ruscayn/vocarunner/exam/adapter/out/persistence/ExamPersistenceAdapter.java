package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.PersistenceAdapter;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시험 저장소 (PostgreSQL test_session).
 * 진행 중 = 상태 IN_PROGRESS 이고 마감 전 (Exam.isInProgress 와 같은 조건)
 */
@PersistenceAdapter
@RequiredArgsConstructor
public class ExamPersistenceAdapter implements ExamPersistencePort {

    private final SpringDataExamRepository springDataRepository;

    /**
     * 새 시험(버전 없음)은 새 행으로 넣고, 이미 있는 시험은 읽어 온 행에 바뀐 상태만 옮겨 담는다 (커밋 때 UPDATE).
     *
     * <p>새 행을 만들어 merge 하지 않는 까닭: 도메인에 없는 만든 · 고친 시각(BaseEntity)을 지키고,
     * 바뀐 것이 없으면 UPDATE 를 내지 않아 버전이 그대로이게 (새 행으로 merge 하면 고친 시각이 비어 늘 UPDATE).
     *
     * <p>낙관적 잠금: 같은 트랜잭션에서 읽은 시험이면 읽어 온 행의 버전으로 UPDATE 해
     * 그사이 다른 요청이 바꿨으면 커밋 때 실패.
     * 다른 트랜잭션에서 읽은 시험이면 여기서 버전을 비교해 다르거나 행이 없으면 바로 실패
     */
    @Override
    @Transactional
    public void save(Exam exam) {
        if (exam.getVersion() == null) {
            springDataRepository.save(ExamJpaEntity.from(exam));
            return;
        }
        ExamJpaEntity entity = springDataRepository.findById(exam.getId())
                .filter(found -> found.getVersion().equals(exam.getVersion()))
                .orElseThrow(() -> new ObjectOptimisticLockingFailureException(ExamJpaEntity.class, exam.getId()));
        entity.update(exam);
    }

    @Override
    public Optional<Exam> findById(UUID examId) {
        return springDataRepository.findById(examId).map(ExamJpaEntity::toDomain);
    }

    @Override
    public void delete(UUID examId) {
        springDataRepository.deleteById(examId);
    }

    @Override
    public void lockForStart() {
        springDataRepository.advisoryLock(SpringDataExamRepository.START_LOCK_KEY);
    }

    @Override
    public int countInProgress(Instant now) {
        return Math.toIntExact(springDataRepository.countActive(ExamStatus.IN_PROGRESS, now));
    }

    @Override
    public List<Exam> findInProgressByDevice(String deviceId, Instant now) {
        return springDataRepository.findActiveByDevice(ExamStatus.IN_PROGRESS, now, deviceId).stream()
                .map(ExamJpaEntity::toDomain)
                .toList();
    }

    @Override
    public int countInProgressByIp(String ip, Instant now) {
        return Math.toIntExact(springDataRepository.countActiveByIp(ExamStatus.IN_PROGRESS, now, ip));
    }

    @Override
    @Transactional
    public int deleteDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout) {
        return springDataRepository.deleteDisposable(now, now.minus(undeliveredTimeout), now.minus(resultRetention));
    }

    @Override
    public long count() {
        return springDataRepository.count();
    }

}

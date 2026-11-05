package org.ruscayn.vocarunner.exam.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuestionsResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamResult;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.Question;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.ruscayn.vocarunner.exam.domain.SelectedOption;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ExamService implements ExamUseCase {

    private final ExamPersistencePort examPersistencePort;
    private final QuestionGenerator questionGenerator = new QuestionGenerator();
    private final WordCatalog wordCatalog;
    private final ExamProperties examProperties;
    private final TransactionTemplate transactionTemplate;

    /**
     * 기기 하나에는 진행 중 시험이 1개만 있다. 새로 시작하면 그 기기의 진행 중 시험은 중단한다
     * (브라우저가 강제 종료돼 중단 요청을 못 보낸 시험이 새 시작을 막지 않게).
     * 확인과 저장 사이에 다른 시작 요청이 끼어들어 최대를 넘기지 않도록 마지막 확인과 저장은 시작 잠금 안에서 하고,
     * 시간이 걸리는 문항 생성은 잠금 밖에서 한다. 최대에 걸릴 요청이 생성부터 하지 않게 잠금 없이 한 번 먼저 확인한다
     */
    @Override
    public UUID start(StartExamCommand command) {
        long begin = System.currentTimeMillis();
        Instant now = Instant.now();
        checkCapacity(command.examinee(), now);
        Exam exam = newExam(command, now);
        transactionTemplate.executeWithoutResult(tx -> saveReplacingOld(exam));
        logStarted(exam, begin);
        return exam.getId();
    }

    /** 돌려주는 값 = 새로 시작하면 중단할 그 기기의 진행 중 시험 */
    private List<Exam> checkCapacity(Examinee examinee, Instant now) {
        List<Exam> examsOnDevice = examPersistencePort.findInProgressByDevice(examinee.deviceId(), now);
        int inProgressCount = examPersistencePort.countInProgress(now);
        int inProgressCountByIp = examPersistencePort.countInProgressByIp(examinee.clientIp(), now);
        requireCapacity(examinee, inProgressCount, inProgressCountByIp, examsOnDevice);
        return examsOnDevice;
    }

    private void requireCapacity(Examinee examinee, int inProgressCount, int inProgressCountByIp,
                                 List<Exam> examsOnDevice) {
        try {
            examProperties.capacity().check(examinee, inProgressCount, inProgressCountByIp, examsOnDevice);
        } catch (CustomException e) {
            log.warn("시험 시작 거절 ({}) ip={} 진행 중={} IP 진행 중={}", e.getErrorCode().getCode(),
                    examinee.clientIp(), inProgressCount, inProgressCountByIp);
            throw e;
        }
    }

    private Exam newExam(StartExamCommand command, Instant now) {
        List<Question> questions = questionGenerator.generate(wordCatalog, command.level(),
                examProperties.maxQuestionCount());
        return Exam.start(command.level(), questions, command.examinee(), now, examProperties.grace());
    }

    private void saveReplacingOld(Exam exam) {
        examPersistencePort.lockForStart();
        checkCapacity(exam.getExaminee(), Instant.now()).forEach(this::deleteReplaced);
        examPersistencePort.save(exam);
    }

    private void deleteReplaced(Exam old) {
        examPersistencePort.delete(old.getId());
        log.info("시험 중단 (같은 기기에서 새 시험 시작) exam={}", old.getId());
    }

    private static void logStarted(Exam exam, long begin) {
        log.info("시험 시작 exam={} level={} 문제={} 마감={} ({}ms)",
                exam.getId(), exam.getLevel(), exam.getQuestionCount(), exam.getTime().deadline(),
                System.currentTimeMillis() - begin);
    }

    @Override
    @Transactional
    public ExamQuestionsResult deliverQuestions(UUID examId, String deviceId) {
        Exam exam = findStartedBy(examId, deviceId);
        Instant now = Instant.now();
        exam.deliverQuestions(now);
        examPersistencePort.save(exam);
        return ExamQuestionsResult.of(exam, wordCatalog, now);
    }

    /** 다른 기기면 없는 시험과 같은 응답을 준다. 시험 id 가 새어도 다른 사람이 문제를 받아 가거나 대신 제출할 수 없게 */
    private Exam findStartedBy(UUID examId, String deviceId) {
        Exam exam = find(examId);
        if (!exam.isStartedBy(deviceId)) {
            log.warn("다른 기기의 시험 요청 거절 exam={}", examId);
            throw new ExamNotFoundException();
        }
        return exam;
    }

    private Exam find(UUID examId) {
        return examPersistencePort.findById(examId)
                .orElseThrow(ExamNotFoundException::new);
    }

    /** 거절해도 마감 초과 삭제는 반영해야 하므로 업무 예외로는 되돌리지 않는다 (채점 전 검사에서 거절하면 바뀐 것이 없다) */
    @Override
    @Transactional(noRollbackFor = CustomException.class)
    public SubmitExamResult submit(SubmitExamCommand command) {
        Exam exam = findStartedBy(command.examId(), command.deviceId());
        submitOrDeleteIfExpired(exam, command.toSelectedOptions());
        examPersistencePort.save(exam);
        logSubmittedAfterCommit(exam);
        return SubmitExamResult.from(exam);
    }

    private void submitOrDeleteIfExpired(Exam exam, List<SelectedOption> selected) {
        try {
            exam.submit(selected, Instant.now());
        } catch (CustomException e) {
            deleteIfExpired(exam, e);
            throw e;
        }
    }

    private void deleteIfExpired(Exam exam, CustomException e) {
        if (e.getErrorCode() == ErrorCode.EXAM_EXPIRED) {
            examPersistencePort.delete(exam.getId());
            log.info("시험 제출 거절 (마감 초과) exam={} 마감={}", exam.getId(), exam.getTime().deadline());
        }
    }

    /**
     * 같은 시험을 동시에 두 번 제출하면 늦은 쪽은 커밋할 때 저장 충돌(@Version)로 되돌려지므로,
     * 커밋된 뒤에 남겨 실제로 채점 · 저장된 제출만 로그에 한 줄 남긴다
     */
    private static void logSubmittedAfterCommit(Exam exam) {
        afterCommit(() -> log.info("시험 제출 exam={} 맞힌 수={}/{} 점수={}", exam.getId(),
                exam.getGradingResult().correctCount(), exam.getQuestionCount(), exam.getGradingResult().score()));
    }

    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    @Override
    @Transactional
    public void abortIfInProgress(UUID examId, String deviceId) {
        Instant now = Instant.now();
        examPersistencePort.findById(examId)
                .filter(exam -> exam.isAbortableBy(deviceId, now))
                .ifPresent(exam -> {
                    examPersistencePort.delete(examId);
                    log.info("시험 중단 exam={}", examId);
                });
    }

    @Override
    public void cleanup() {
        Instant now = Instant.now();
        int removed = examPersistencePort.deleteDisposable(
                now, examProperties.resultRetention(), examProperties.undeliveredTimeout());
        if (removed > 0) {
            log.info("시험 정리: {}개 삭제, 남은 {}개 (진행 중 {}개)",
                    removed, examPersistencePort.count(), examPersistencePort.countInProgress(now));
        }
    }

}

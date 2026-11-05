package org.ruscayn.vocarunner.exam.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.Question;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class StartExamService implements StartExamUseCase {

    private final ExamPersistencePort examPersistencePort;
    private final QuestionGenerator questionGenerator;
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
        List<Question> questions = questionGenerator.generate(command.level(), examProperties.maxQuestionCount());
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

}

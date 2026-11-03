package org.ruscayn.vocarunner.exam.application.service;

import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuestionsResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamResult;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamResult;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Direction;
import org.ruscayn.vocarunner.exam.domain.Question;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * 시험 시작·문제·제출·결과.
 * 시험(정답지·고른 보기·결과)은 DB(test_session)에 저장해 재시작·배포 때도 이어진다 (U21).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamApplicationService {

    private final ExamPersistencePort examPersistencePort;
    private final QuestionGenerator questionGenerator;
    private final WordCatalog wordCatalog;
    private final ExamProperties examProperties;
    private final TransactionTemplate transactionTemplate;

    /**
     * 시험 시작 : 수용량 확인 → 문항 생성 → (시험 시작 잠금 → 다시 수용량 확인 → 같은 기기의 이전 시험 중단 → 저장).
     * 1단계는 EN_KO 고정.
     *
     * <p>기기 하나에는 진행 중 시험이 1개만 있다. 새로 시작하면 그 기기에서 진행 중이던 시험은 중단한다.
     * 브라우저가 강제 종료돼 중단 요청을 못 보낸 시험이 남아도 새 시작을 막지 않는다 (U32·U8)
     *
     * <p>확인과 저장 사이에 다른 시작 요청이 끼어들면 동시 요청으로 최대를 넘길 수 있으므로(B3)
     * 마지막 확인과 저장은 DB 잠금(트랜잭션 단위)으로 한 번에 하나씩 한다. 문항 생성은 시간이 걸려 잠금 밖에서 하고,
     * 최대에 걸릴 요청이 생성부터 하지 않도록 잠금 없이 한 번 먼저 확인한다.
     *
     * @param command 시험 레벨과 시작한 응시자 (기기·IP)
     */
    public UUID start(StartExamCommand command) {
        Level level = command.level();
        Examinee examinee = command.examinee();
        long begin = System.currentTimeMillis();
        Instant now = Instant.now();
        checkCapacity(examinee, now);

        List<Question> questions = questionGenerator.generate(level, examProperties.maxQuestionCount());
        Exam exam = Exam.start(level, Direction.EN_KO, Exam.DEFAULT_TIME_PER_QUESTION_SEC,
                questions, examinee, now, examProperties.grace());
        transactionTemplate.executeWithoutResult(tx -> {
            examPersistencePort.lockForStart();
            List<Exam> replaced = checkCapacity(examinee, Instant.now());
            replaced.forEach(old -> {
                examPersistencePort.delete(old.getId());
                log.info("시험 중단 (같은 기기에서 새 시험 시작) exam={}", old.getId());
            });
            examPersistencePort.save(exam);
        });

        log.info("시험 시작 exam={} level={} 문제={} 마감={} ({}ms)",
                exam.getId(), level, exam.getQuestionCount(), exam.getDeadline(),
                System.currentTimeMillis() - begin);
        return exam.getId();
    }

    /**
     * 수용량 확인 (최대 진행 중 · IP당 최대 진행 중). 넘으면 예외.
     * 같은 기기의 진행 중 시험은 새 시험으로 바뀌므로 세지 않는다
     *
     * @return 새 시험을 시작하면 중단할 같은 기기의 진행 중 시험
     */
    private List<Exam> checkCapacity(Examinee examinee, Instant now) {
        List<Exam> byDevice = examPersistencePort.findInProgressByDevice(examinee.deviceId(), now);
        int inProgress = examPersistencePort.countInProgress(now) - byDevice.size();
        if (inProgress >= examProperties.maxInProgress()) {
            log.warn("시험 시작 거절 (최대 진행 중) 진행 중={} 최대={}", inProgress, examProperties.maxInProgress());
            throw new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS);
        }
        long sameIp = byDevice.stream().filter(s -> examinee.clientIp().equals(s.getExaminee().clientIp())).count();
        long byIp = examPersistencePort.countInProgressByIp(examinee.clientIp(), now) - sameIp;
        if (byIp >= examProperties.maxInProgressPerIp()) {
            log.warn("시험 시작 거절 (IP당 최대 진행 중) ip={} 진행 중={}", examinee.clientIp(), byIp);
            throw new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP);
        }
        return byDevice;
    }

    /** 시험 화면용 문제 (진행 중인 시험만, 정답 번호 제외). 시작한 기기만 */
    @Transactional
    public ExamQuestionsResult getQuestions(UUID examId, String deviceId) {
        Exam exam = findStartedBy(examId, deviceId);
        Instant now = Instant.now();
        if (!exam.isInProgress(now)) {
            throw new CustomException(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }
        long remainingSec = Math.max(0, Duration.between(now, exam.getDeadline()).toSeconds());
        exam.markQuestionsDelivered();

        Direction direction = exam.getDirection();
        List<ExamQuestionsResult.QuestionItem> questions = new ArrayList<>(exam.getQuestionCount());
        for (int no = 1; no <= exam.getQuestionCount(); no++) {
            questions.add(toQuestionItem(exam, no, direction));
        }

        return new ExamQuestionsResult(
                exam.getId(),
                exam.getLevel().name(),
                exam.getLevel().groupName(),
                direction.name(),
                exam.getQuestionCount(),
                exam.getTimePerQuestionSec(),
                Duration.between(exam.getStartedAt(), exam.getDeadline()).toMinutes(),
                remainingSec,
                questions);
    }

    /**
     * 제출 및 채점. 마감 시각이 지났으면 만료로 바꾼 뒤 지움. 시작한 기기만.
     * 거절(예외)해도 마감 초과 삭제는 반영해야 하므로 업무 예외로는 되돌리지 않는다 (채점 전 검사에서 거절하면 바뀐 내용 없음)
     */
    @Transactional(noRollbackFor = CustomException.class)
    public SubmitExamResult submit(SubmitExamCommand command) {
        UUID examId = command.examId();
        Exam exam = findStartedBy(examId, command.deviceId());

        Map<Integer, Integer> selectedByNo = new HashMap<>();  // null 값 허용 (시간 초과)
        for (SubmitExamCommand.Answer answer : command.answers()) {
            int no = answer.questionNo();
            if (no > exam.getQuestionCount()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "없는 문제 번호입니다: " + no);
            }
            if (selectedByNo.containsKey(no)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "중복된 문제 번호입니다: " + no);
            }
            selectedByNo.put(no, answer.selectedOptionNo());
        }

        try {
            exam.submit(selectedByNo, Instant.now());
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.EXAM_EXPIRED) {
                examPersistencePort.delete(examId);
                log.info("시험 제출 거절 (마감 초과) exam={} 마감={}", examId, exam.getDeadline());
            }
            throw e;
        }
        // 같은 시험을 동시에 두 번 제출하면 늦은 쪽은 커밋할 때 저장 충돌(@Version)로 되돌려지므로
        // 저장이 끝난 뒤에 남긴다. 실제로 채점·저장된 제출만 로그에 한 줄 (U39)
        afterCommit(() -> log.info("시험 제출 exam={} 맞힌 수={}/{} 점수={}",
                exam.getId(), exam.getCorrectCount(), exam.getQuestionCount(), exam.getScore()));

        return new SubmitExamResult(
                exam.getId(), exam.getQuestionCount(), exam.getCorrectCount(), exam.getScore());
    }

    /** 결과 (완료된 시험만). 없거나(보관 시간 경과 포함) 완료 전이면 empty */
    @Transactional(readOnly = true)
    public Optional<ExamResult> findResult(UUID examId) {
        return examPersistencePort.findById(examId)
                .filter(Exam::isCompleted)
                .map(this::toResult);
    }

    /** 제출을 마쳐 결과를 보관 중인지 (보관 시간이 지나 정리됐으면 false) */
    @Transactional(readOnly = true)
    public boolean hasResult(UUID examId) {
        return examPersistencePort.findById(examId)
                .filter(Exam::isCompleted)
                .isPresent();
    }

    /** 결과 (API) */
    @Transactional(readOnly = true)
    public ExamResult getResult(UUID examId) {
        Exam exam = find(examId);
        if (!exam.isCompleted()) {
            throw new CustomException(ErrorCode.EXAM_NOT_COMPLETED);
        }
        return toResult(exam);
    }

    /** 진행 중이면 중단(바로 지움). 없거나 이미 끝났거나 다른 기기에서 시작한 시험은 무시 */
    @Transactional
    public void abortExam(UUID examId, String deviceId) {
        Instant now = Instant.now();
        examPersistencePort.findById(examId)
                .filter(exam -> exam.isStartedBy(deviceId) && exam.isInProgress(now))
                .ifPresent(exam -> {
                    examPersistencePort.delete(examId);
                    log.info("시험 중단 exam={}", examId);
                });
    }

    /** 진행 중(마감 전)인 시험인지 */
    @Transactional(readOnly = true)
    public boolean isInProgress(UUID examId) {
        Instant now = Instant.now();
        return examPersistencePort.findById(examId)
                .map(exam -> exam.isInProgress(now))
                .orElse(false);
    }

    private ExamResult toResult(Exam exam) {
        Direction direction = exam.getDirection();
        List<ExamResult.WrongItem> wrongItems = new ArrayList<>();
        for (int no = 1; no <= exam.getQuestionCount(); no++) {
            if (exam.isCorrect(no)) {
                continue;
            }
            CatalogWord answer = wordCatalog.get(exam.answerLevelWordId(no));
            Long selectedId = exam.selectedLevelWordId(no);
            wrongItems.add(new ExamResult.WrongItem(
                    no,
                    promptText(answer, direction),
                    promptSubText(answer, direction),
                    selectedId == null ? null : optionText(wordCatalog.get(selectedId), direction),
                    optionText(answer, direction),
                    answer.headword()));
        }
        return new ExamResult(
                exam.getId(),
                exam.getLevel().name(),
                exam.getLevel().groupName(),
                exam.getQuestionCount(),
                exam.getCorrectCount(),
                exam.getScore(),
                exam.getSubmittedAt().plus(examProperties.resultRetention()),
                examProperties.resultRetention().toMinutes(),
                wrongItems);
    }

    private ExamQuestionsResult.QuestionItem toQuestionItem(Exam exam, int no, Direction direction) {
        List<Long> optionIds = exam.optionLevelWordIds(no);
        List<ExamQuestionsResult.Option> options = IntStream.range(0, optionIds.size())
                .mapToObj(i -> new ExamQuestionsResult.Option(i + 1,
                        optionText(wordCatalog.get(optionIds.get(i)), direction)))
                .toList();
        CatalogWord answer = wordCatalog.get(exam.answerLevelWordId(no));
        return new ExamQuestionsResult.QuestionItem(no, promptText(answer, direction),
                promptSubText(answer, direction), options);
    }

    private String promptText(CatalogWord answer, Direction direction) {
        return direction == Direction.EN_KO ? answer.headword() : answer.optionText();
    }

    /** 영어 단어가 제시어일 때만 제시어 보조(표제어 보조)를 붙인다 */
    private String promptSubText(CatalogWord answer, Direction direction) {
        return direction == Direction.EN_KO ? answer.headwordSub() : null;
    }

    private String optionText(CatalogWord option, Direction direction) {
        return direction == Direction.EN_KO ? option.optionText() : option.headword();
    }

    /**
     * 이 기기에서 시작한 시험. 다른 기기면 없는 시험과 같은 응답을 준다.
     * 주소(시험 id)가 새어도 다른 사람이 문제를 받아 가거나 대신 제출해 끝내 버릴 수 없게 한다 (U9)
     */
    private Exam findStartedBy(UUID examId, String deviceId) {
        Exam exam = find(examId);
        if (!exam.isStartedBy(deviceId)) {
            log.warn("다른 기기의 시험 요청 거절 exam={}", examId);
            throw notFound();
        }
        return exam;
    }

    private Exam find(UUID examId) {
        return examPersistencePort.findById(examId)
                .orElseThrow(ExamApplicationService::notFound);
    }

    private static CustomException notFound() {
        return new CustomException(ErrorCode.ENTITY_NOT_FOUND, "시험을 찾을 수 없습니다. (종료되었거나 만료됨)");
    }

    /** 지금 트랜잭션이 커밋된 뒤에 실행한다. 되돌려지면 실행하지 않는다 */
    private static void afterCommit(Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

}

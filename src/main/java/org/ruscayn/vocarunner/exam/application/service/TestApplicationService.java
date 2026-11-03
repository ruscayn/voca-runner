package org.ruscayn.vocarunner.exam.application.service;

import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.port.in.TestQuestionView;
import org.ruscayn.vocarunner.exam.application.port.in.TestQuestionsResponse;
import org.ruscayn.vocarunner.exam.application.port.in.TestResultResponse;
import org.ruscayn.vocarunner.exam.application.port.in.TestSubmitRequest;
import org.ruscayn.vocarunner.exam.application.port.in.TestSubmitResponse;
import org.ruscayn.vocarunner.exam.application.port.out.TestSessionRepository;
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
 * 시험(정답지·고른 답·결과)은 DB(test_session)에 저장해 재시작·배포 때도 이어진다 (U21).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestApplicationService {

    private final TestSessionRepository testSessionRepository;
    private final QuestionGenerator questionGenerator;
    private final WordCatalog wordCatalog;
    private final ExamProperties examProperties;
    private final TransactionTemplate transactionTemplate;

    /**
     * 시험 시작 : 상한 확인 → 문제 생성 → (시작 잠금 → 다시 상한 확인 → 같은 기기의 이전 시험 종료 → 저장). 1단계는 EN_KO 고정.
     *
     * <p>기기 하나에는 진행 중 시험이 1개만 있다. 새로 시작하면 그 기기에서 진행 중이던 시험은 종료한다.
     * 브라우저가 강제 종료돼 만료 요청을 못 보낸 시험이 남아도 새 시작을 막지 않는다 (U32·U8)
     *
     * <p>확인과 저장 사이에 다른 시작 요청이 끼어들면 동시 요청으로 상한을 넘길 수 있으므로(B3)
     * 마지막 확인과 저장은 DB 잠금(트랜잭션 단위)으로 한 번에 하나씩 한다. 문제 생성은 시간이 걸려 잠금 밖에서 하고,
     * 상한에 걸릴 요청이 생성부터 하지 않도록 잠금 없이 한 번 먼저 확인한다.
     *
     * @param client 시작한 사용자 (기기·IP)
     */
    public UUID start(Level level, Examinee client) {
        long begin = System.currentTimeMillis();
        Instant now = Instant.now();
        checkCapacity(client, now);

        List<Question> questions = questionGenerator.generate(level, examProperties.questionCount());
        Exam session = Exam.start(level, Direction.EN_KO, Exam.DEFAULT_TIME_LIMIT_SEC,
                questions, client, now, examProperties.grace());
        transactionTemplate.executeWithoutResult(tx -> {
            testSessionRepository.lockForStart();
            List<Exam> replaced = checkCapacity(client, Instant.now());
            replaced.forEach(old -> {
                testSessionRepository.delete(old.getId());
                log.info("시험 만료 (같은 기기에서 새 시험 시작) session={}", old.getId());
            });
            testSessionRepository.save(session);
        });

        log.info("시험 시작 session={} level={} 문제={} 마감={} ({}ms)",
                session.getId(), level, session.getQuestionCount(), session.getDeadline(),
                System.currentTimeMillis() - begin);
        return session.getId();
    }

    /**
     * 전체·IP별 동시 진행 상한 확인. 넘으면 예외.
     * 같은 기기의 진행 중 시험은 새 시험으로 바뀌므로 세지 않는다
     *
     * @return 새 시험을 시작하면 종료할 같은 기기의 진행 중 시험
     */
    private List<Exam> checkCapacity(Examinee client, Instant now) {
        List<Exam> byDevice = testSessionRepository.findInProgressByDevice(client.deviceId(), now);
        int inProgress = testSessionRepository.countInProgress(now) - byDevice.size();
        if (inProgress >= examProperties.maxInProgress()) {
            log.warn("시험 시작 거절 (전체 상한) 진행 중={} 상한={}", inProgress, examProperties.maxInProgress());
            throw new CustomException(ErrorCode.TEST_SERVER_BUSY);
        }
        long sameIp = byDevice.stream().filter(s -> client.clientIp().equals(s.getClient().clientIp())).count();
        long byIp = testSessionRepository.countInProgressByIp(client.clientIp(), now) - sameIp;
        if (byIp >= examProperties.maxInProgressPerIp()) {
            log.warn("시험 시작 거절 (IP 상한) ip={} 진행 중={}", client.clientIp(), byIp);
            throw new CustomException(ErrorCode.TEST_TOO_MANY_IN_PROGRESS_SAME_NETWORK);
        }
        return byDevice;
    }

    /** 시험 화면용 문제 (진행 중인 시험만, 정답 번호 제외). 시작한 기기만 */
    @Transactional
    public TestQuestionsResponse getQuestions(UUID sessionId, String deviceId) {
        Exam session = findStartedBy(sessionId, deviceId);
        Instant now = Instant.now();
        if (!session.isInProgress(now)) {
            throw new CustomException(ErrorCode.TEST_NOT_IN_PROGRESS);
        }
        long remainingSec = Math.max(0, Duration.between(now, session.getDeadline()).toSeconds());
        session.markQuestionsDelivered();

        Direction direction = session.getDirection();
        List<TestQuestionView> questions = new ArrayList<>(session.getQuestionCount());
        for (int no = 1; no <= session.getQuestionCount(); no++) {
            questions.add(toView(session, no, direction));
        }

        return new TestQuestionsResponse(
                session.getId(),
                session.getLevel().name(),
                session.getLevel().groupName(),
                direction.name(),
                session.getQuestionCount(),
                session.getTimeLimitSec(),
                Duration.between(session.getStartedAt(), session.getDeadline()).toMinutes(),
                remainingSec,
                questions);
    }

    /**
     * 제출 및 채점. 마감 시각이 지났으면 무효(삭제). 시작한 기기만.
     * 거절(예외)해도 마감 초과 삭제는 반영해야 하므로 업무 예외로는 되돌리지 않는다 (채점 전 검사에서 거절하면 바뀐 내용 없음)
     */
    @Transactional(noRollbackFor = CustomException.class)
    public TestSubmitResponse submit(UUID sessionId, String deviceId, TestSubmitRequest request) {
        Exam session = findStartedBy(sessionId, deviceId);

        Map<Integer, Integer> selectedByNo = new HashMap<>();  // null 값 허용 (시간 초과)
        for (TestSubmitRequest.Answer answer : request.answers()) {
            int no = answer.questionNo();
            if (no > session.getQuestionCount()) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "없는 문제 번호입니다: " + no);
            }
            if (selectedByNo.containsKey(no)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "중복된 문제 번호입니다: " + no);
            }
            selectedByNo.put(no, answer.selectedOptionNo());
        }

        try {
            session.submit(selectedByNo, Instant.now());
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.TEST_TIME_OVER) {
                testSessionRepository.delete(sessionId);
                log.info("시험 제출 거절 (마감 초과) session={} 마감={}", sessionId, session.getDeadline());
            }
            throw e;
        }
        // 같은 시험을 동시에 두 번 제출하면 늦은 쪽은 커밋할 때 저장 충돌(@Version)로 되돌려지므로
        // 저장이 끝난 뒤에 남긴다. 실제로 채점·저장된 제출만 로그에 한 줄 (U39)
        afterCommit(() -> log.info("시험 제출 session={} 맞힌 수={}/{} 점수={}",
                session.getId(), session.getCorrectCount(), session.getQuestionCount(), session.getScore()));

        return new TestSubmitResponse(
                session.getId(), session.getQuestionCount(), session.getCorrectCount(), session.getScore());
    }

    /** 결과 (완료된 시험만). 없거나(보관 시간 경과 포함) 완료 전이면 empty */
    @Transactional(readOnly = true)
    public Optional<TestResultResponse> findResult(UUID sessionId) {
        return testSessionRepository.findById(sessionId)
                .filter(Exam::isCompleted)
                .map(this::toResult);
    }

    /** 제출을 마쳐 결과를 보관 중인지 (보관 시간이 지나 정리됐으면 false) */
    @Transactional(readOnly = true)
    public boolean hasResult(UUID sessionId) {
        return testSessionRepository.findById(sessionId)
                .filter(Exam::isCompleted)
                .isPresent();
    }

    /** 결과 (API) */
    @Transactional(readOnly = true)
    public TestResultResponse getResult(UUID sessionId) {
        Exam session = find(sessionId);
        if (!session.isCompleted()) {
            throw new CustomException(ErrorCode.TEST_NOT_COMPLETED);
        }
        return toResult(session);
    }

    /** 진행 중이면 무효 처리(삭제). 없거나 이미 끝났거나 다른 기기에서 시작한 시험은 무시 */
    @Transactional
    public void expireIfInProgress(UUID sessionId, String deviceId) {
        Instant now = Instant.now();
        testSessionRepository.findById(sessionId)
                .filter(session -> session.isStartedBy(deviceId) && session.isInProgress(now))
                .ifPresent(session -> {
                    testSessionRepository.delete(sessionId);
                    log.info("시험 만료 session={}", sessionId);
                });
    }

    /** 진행 중(마감 전)인 시험인지 */
    @Transactional(readOnly = true)
    public boolean isInProgress(UUID sessionId) {
        Instant now = Instant.now();
        return testSessionRepository.findById(sessionId)
                .map(session -> session.isInProgress(now))
                .orElse(false);
    }

    private TestResultResponse toResult(Exam session) {
        Direction direction = session.getDirection();
        List<TestResultResponse.WrongItem> wrongItems = new ArrayList<>();
        for (int no = 1; no <= session.getQuestionCount(); no++) {
            if (session.isCorrect(no)) {
                continue;
            }
            CatalogWord answer = wordCatalog.get(session.answerLevelWordId(no));
            Long selectedId = session.selectedLevelWordId(no);
            wrongItems.add(new TestResultResponse.WrongItem(
                    no,
                    promptText(answer, direction),
                    promptSubText(answer, direction),
                    selectedId == null ? null : optionText(wordCatalog.get(selectedId), direction),
                    optionText(answer, direction),
                    answer.headword()));
        }
        return new TestResultResponse(
                session.getId(),
                session.getLevel().name(),
                session.getLevel().groupName(),
                session.getQuestionCount(),
                session.getCorrectCount(),
                session.getScore(),
                session.getSubmittedAt().plus(examProperties.resultRetention()),
                examProperties.resultRetention().toMinutes(),
                wrongItems);
    }

    private TestQuestionView toView(Exam session, int no, Direction direction) {
        List<Long> optionIds = session.optionLevelWordIds(no);
        List<TestQuestionView.Option> optionViews = IntStream.range(0, optionIds.size())
                .mapToObj(i -> new TestQuestionView.Option(i + 1, optionText(wordCatalog.get(optionIds.get(i)), direction)))
                .toList();
        CatalogWord answer = wordCatalog.get(session.answerLevelWordId(no));
        return new TestQuestionView(no, promptText(answer, direction), promptSubText(answer, direction), optionViews);
    }

    private String promptText(CatalogWord answer, Direction direction) {
        return direction == Direction.EN_KO ? answer.headword() : answer.meaningText();
    }

    /** 영어 단어가 문제일 때만 함께 표시할 형태를 붙인다 */
    private String promptSubText(CatalogWord answer, Direction direction) {
        return direction == Direction.EN_KO ? answer.headwordSub() : null;
    }

    private String optionText(CatalogWord option, Direction direction) {
        return direction == Direction.EN_KO ? option.meaningText() : option.headword();
    }

    /**
     * 이 기기에서 시작한 시험. 다른 기기면 없는 시험과 같은 응답을 준다.
     * 주소(시험 id)가 새어도 다른 사람이 문제를 받아 가거나 대신 제출해 끝내 버릴 수 없게 한다 (U9)
     */
    private Exam findStartedBy(UUID sessionId, String deviceId) {
        Exam session = find(sessionId);
        if (!session.isStartedBy(deviceId)) {
            log.warn("다른 기기의 시험 요청 거절 session={}", sessionId);
            throw notFound();
        }
        return session;
    }

    private Exam find(UUID sessionId) {
        return testSessionRepository.findById(sessionId)
                .orElseThrow(TestApplicationService::notFound);
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

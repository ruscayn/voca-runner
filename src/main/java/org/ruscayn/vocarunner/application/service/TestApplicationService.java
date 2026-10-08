package org.ruscayn.vocarunner.application.service;

import org.ruscayn.vocarunner.application.config.TestProperties;
import org.ruscayn.vocarunner.application.dto.request.TestSubmitRequest;
import org.ruscayn.vocarunner.application.dto.response.TestQuestionView;
import org.ruscayn.vocarunner.application.dto.response.TestQuestionsResponse;
import org.ruscayn.vocarunner.application.dto.response.TestResultResponse;
import org.ruscayn.vocarunner.application.dto.response.TestSubmitResponse;
import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.test.Direction;
import org.ruscayn.vocarunner.domain.model.test.GeneratedQuestion;
import org.ruscayn.vocarunner.domain.model.test.TestSession;
import org.ruscayn.vocarunner.domain.model.vocabulary.CatalogWord;
import org.ruscayn.vocarunner.domain.model.vocabulary.WordCatalog;
import org.ruscayn.vocarunner.domain.repository.TestSessionRepository;
import org.ruscayn.vocarunner.domain.service.TestQuestionGenerator;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
 * 시험(정답지)은 메모리 저장소에만 두고 DB 를 사용하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestApplicationService {

    private final TestSessionRepository testSessionRepository;
    private final TestQuestionGenerator testQuestionGenerator;
    private final WordCatalog wordCatalog;
    private final TestProperties testProperties;

    /**
     * 시험 시작 : 상한 확인 → 문제 생성 → 메모리 저장. 1단계는 EN_KO 고정.
     *
     * @param clientKey 사용자 구분 값 (현재는 IP)
     */
    public UUID start(Level level, String clientKey) {
        long begin = System.currentTimeMillis();
        Instant now = Instant.now();

        int inProgress = testSessionRepository.countInProgress(now);
        if (inProgress >= testProperties.maxInProgress()) {
            log.warn("시험 시작 거절 (전체 상한) 진행 중={} 상한={}", inProgress, testProperties.maxInProgress());
            throw new CustomException(ErrorCode.TEST_SERVER_BUSY);
        }
        int byClient = testSessionRepository.countInProgress(clientKey, now);
        if (byClient >= testProperties.maxInProgressPerClient()) {
            log.warn("시험 시작 거절 (사용자 상한) client={} 진행 중={}", clientKey, byClient);
            throw new CustomException(ErrorCode.TEST_TOO_MANY_IN_PROGRESS);
        }

        List<GeneratedQuestion> questions = testQuestionGenerator.generate(level, testProperties.questionCount());
        TestSession session = TestSession.start(level, Direction.EN_KO, TestSession.DEFAULT_TIME_LIMIT_SEC,
                questions, clientKey, now, testProperties.grace());
        testSessionRepository.save(session);

        log.info("시험 시작 session={} level={} 문제={} 마감={} ({}ms)",
                session.getId(), level, session.getQuestionCount(), session.getDeadline(),
                System.currentTimeMillis() - begin);
        return session.getId();
    }

    /** 시험 화면용 문제 (진행 중인 시험만, 정답 번호 제외) */
    public TestQuestionsResponse getQuestions(UUID sessionId) {
        TestSession session = find(sessionId);
        Instant now = Instant.now();
        if (!session.isInProgress(now)) {
            throw new CustomException(ErrorCode.TEST_NOT_IN_PROGRESS);
        }
        long remainingSec = Math.max(0, Duration.between(now, session.getDeadline()).toSeconds());

        Direction direction = session.getDirection();
        List<TestQuestionView> questions = new ArrayList<>(session.getQuestionCount());
        for (int no = 1; no <= session.getQuestionCount(); no++) {
            questions.add(toView(session, no, direction));
        }

        return new TestQuestionsResponse(
                session.getId(),
                session.getLevel().name(),
                direction.name(),
                session.getQuestionCount(),
                session.getTimeLimitSec(),
                remainingSec,
                questions);
    }

    /** 제출 및 채점. 마감 시각이 지났으면 무효 */
    public TestSubmitResponse submit(UUID sessionId, TestSubmitRequest request) {
        TestSession session = find(sessionId);

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
        log.info("시험 제출 session={} 맞힌 수={}/{} 점수={}",
                session.getId(), session.getCorrectCount(), session.getQuestionCount(), session.getScore());

        return new TestSubmitResponse(
                session.getId(), session.getQuestionCount(), session.getCorrectCount(), session.getScore());
    }

    /** 결과 (완료된 시험만). 없거나(보관 시간 경과 포함) 완료 전이면 empty */
    public Optional<TestResultResponse> findResult(UUID sessionId) {
        return testSessionRepository.findById(sessionId)
                .filter(TestSession::isCompleted)
                .map(this::toResult);
    }

    /** 결과 (API) */
    public TestResultResponse getResult(UUID sessionId) {
        TestSession session = find(sessionId);
        if (!session.isCompleted()) {
            throw new CustomException(ErrorCode.TEST_NOT_COMPLETED);
        }
        return toResult(session);
    }

    /** 진행 중이면 무효 처리 후 메모리에서 삭제. 없거나 이미 끝난 시험은 무시 */
    public void expireIfInProgress(UUID sessionId) {
        Instant now = Instant.now();
        testSessionRepository.findById(sessionId)
                .filter(session -> session.isInProgress(now))
                .ifPresent(session -> {
                    session.expire();
                    testSessionRepository.delete(sessionId);
                    log.info("시험 만료 session={}", sessionId);
                });
    }

    public int getQuestionCount() {
        return testProperties.questionCount();
    }

    /** 시험을 마쳐야 하는 전체 시간(초) = 문항 수 × 제한 시간 + 여유 시간. 시작 전 안내용 */
    public long getAllowedSec() {
        return TestSession.allowedDuration(
                testProperties.questionCount(), TestSession.DEFAULT_TIME_LIMIT_SEC, testProperties.grace()).toSeconds();
    }

    /** 진행 중(마감 전)인 시험인지 */
    public boolean isInProgress(UUID sessionId) {
        Instant now = Instant.now();
        return testSessionRepository.findById(sessionId)
                .map(session -> session.isInProgress(now))
                .orElse(false);
    }

    private TestResultResponse toResult(TestSession session) {
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
                    selectedId == null ? null : optionText(wordCatalog.get(selectedId), direction),
                    optionText(answer, direction),
                    answer.headword()));
        }
        return new TestResultResponse(
                session.getId(),
                session.getLevel().name(),
                session.getQuestionCount(),
                session.getCorrectCount(),
                session.getScore(),
                wrongItems);
    }

    private TestQuestionView toView(TestSession session, int no, Direction direction) {
        List<Long> optionIds = session.optionLevelWordIds(no);
        List<TestQuestionView.Option> optionViews = IntStream.range(0, optionIds.size())
                .mapToObj(i -> new TestQuestionView.Option(i + 1, optionText(wordCatalog.get(optionIds.get(i)), direction)))
                .toList();
        return new TestQuestionView(no, promptText(wordCatalog.get(session.answerLevelWordId(no)), direction), optionViews);
    }

    private String promptText(CatalogWord answer, Direction direction) {
        return direction == Direction.EN_KO ? answer.headword() : answer.meaningText();
    }

    private String optionText(CatalogWord option, Direction direction) {
        return direction == Direction.EN_KO ? option.meaningText() : option.headword();
    }

    private TestSession find(UUID sessionId) {
        return testSessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND, "시험을 찾을 수 없습니다. (종료되었거나 만료됨)"));
    }

}

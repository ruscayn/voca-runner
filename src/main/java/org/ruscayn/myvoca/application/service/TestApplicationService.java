package org.ruscayn.myvoca.application.service;

import org.ruscayn.myvoca.application.dto.request.TestSubmitRequest;
import org.ruscayn.myvoca.application.dto.response.TestQuestionView;
import org.ruscayn.myvoca.application.dto.response.TestQuestionsResponse;
import org.ruscayn.myvoca.application.dto.response.TestResultResponse;
import org.ruscayn.myvoca.application.dto.response.TestSubmitResponse;
import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.test.Direction;
import org.ruscayn.myvoca.domain.model.test.TestQuestion;
import org.ruscayn.myvoca.domain.model.test.TestSession;
import org.ruscayn.myvoca.domain.model.vocabulary.CatalogWord;
import org.ruscayn.myvoca.domain.model.vocabulary.WordCatalog;
import org.ruscayn.myvoca.domain.repository.LevelWordRepository;
import org.ruscayn.myvoca.domain.repository.TestSessionRepository;
import org.ruscayn.myvoca.domain.service.TestQuestionGenerator;
import org.ruscayn.myvoca.domain.service.TestQuestionGenerator.GeneratedQuestion;
import org.ruscayn.myvoca.shared.exception.CustomException;
import org.ruscayn.myvoca.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestApplicationService {

    private final LevelWordRepository levelWordRepository;
    private final TestSessionRepository testSessionRepository;
    private final TestQuestionGenerator testQuestionGenerator;
    private final WordCatalog wordCatalog;

    /** 시험 문항 수 (application.yaml : my-voca.test.question-count) */
    @Value("${my-voca.test.question-count:" + TestSession.DEFAULT_QUESTION_COUNT + "}")
    private int questionCount;

    /** 시험 시작 : 문제 생성(메모리 캐시) 후 저장. 1단계는 EN_KO 고정 */
    @Transactional
    public UUID start(Level level) {
        long begin = System.currentTimeMillis();
        List<GeneratedQuestion> generated = testQuestionGenerator.generate(level, questionCount);

        TestSession session = TestSession.start(level, Direction.EN_KO, questionCount, TestSession.DEFAULT_TIME_LIMIT_SEC);
        for (GeneratedQuestion g : generated) {
            // test_question FK 저장용 참조. DB 조회 없음
            session.addQuestion(
                    levelWordRepository.getReference(g.answer().levelWordId()),
                    g.options().stream().map(o -> levelWordRepository.getReference(o.levelWordId())).toList());
        }
        TestSession saved = testSessionRepository.save(session);

        log.info("시험 시작 session={} level={} 문제={} ({}ms)",
                saved.getId(), level, saved.getQuestions().size(), System.currentTimeMillis() - begin);
        return saved.getId();
    }

    /** 시험 화면용 문제 (진행 중인 시험만) */
    public TestQuestionsResponse getQuestions(UUID sessionId) {
        TestSession session = findWithQuestions(sessionId);
        requireInProgress(session);

        List<TestQuestionView> questions = session.getQuestions().stream()
                .map(q -> toView(q, session.getDirection()))
                .toList();

        return new TestQuestionsResponse(
                session.getId(),
                session.getLevel().name(),
                session.getDirection().name(),
                session.getQuestionCount(),
                session.getTimeLimitSec(),
                questions);
    }

    /** 제출 및 채점 */
    @Transactional
    public TestSubmitResponse submit(UUID sessionId, TestSubmitRequest request) {
        TestSession session = findWithQuestions(sessionId);
        requireInProgress(session);

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

        session.submit(selectedByNo);
        log.info("시험 제출 session={} 맞힌 수={}/{} 점수={}",
                session.getId(), session.getCorrectCount(), session.getQuestionCount(), session.getScore());

        return new TestSubmitResponse(
                session.getId(), session.getQuestionCount(), session.getCorrectCount(), session.getScore());
    }

    /** 결과 (완료된 시험만). 없거나 완료 전이면 empty */
    public Optional<TestResultResponse> findResult(UUID sessionId) {
        return testSessionRepository.findByIdWithQuestions(sessionId)
                .filter(TestSession::isCompleted)
                .map(this::toResult);
    }

    /** 결과 (API) */
    public TestResultResponse getResult(UUID sessionId) {
        TestSession session = findWithQuestions(sessionId);
        if (!session.isCompleted()) {
            throw new CustomException(ErrorCode.TEST_NOT_COMPLETED);
        }
        return toResult(session);
    }

    private TestResultResponse toResult(TestSession session) {
        Direction direction = session.getDirection();
        List<TestResultResponse.WrongItem> wrongItems = session.getQuestions().stream()
                .filter(q -> !Boolean.TRUE.equals(q.getIsCorrect()))
                .map(q -> {
                    CatalogWord answer = wordCatalog.get(q.answerLevelWordId());
                    Long selectedId = q.selectedLevelWordId();
                    return new TestResultResponse.WrongItem(
                            q.getQuestionNo(),
                            promptText(answer, direction),
                            selectedId == null ? null : optionText(wordCatalog.get(selectedId), direction),
                            optionText(answer, direction),
                            answer.headword());
                })
                .toList();
        return new TestResultResponse(
                session.getId(),
                session.getLevel().name(),
                session.getQuestionCount(),
                session.getCorrectCount(),
                session.getScore(),
                wrongItems);
    }

    /** 진행 중이면 만료 처리. 없거나 이미 끝난 시험은 무시 */
    @Transactional
    public void expireIfInProgress(UUID sessionId) {
        testSessionRepository.findById(sessionId)
                .filter(TestSession::isInProgress)
                .ifPresent(session -> {
                    session.expire();
                    log.info("시험 만료 session={}", sessionId);
                });
    }

    public int getQuestionCount() {
        return questionCount;
    }

    /** 진행 중인 시험인지 */
    public boolean isInProgress(UUID sessionId) {
        return testSessionRepository.findById(sessionId)
                .map(TestSession::isInProgress)
                .orElse(false);
    }

    private TestQuestionView toView(TestQuestion q, Direction direction) {
        List<Long> optionIds = q.optionLevelWordIds();
        List<TestQuestionView.Option> optionViews = IntStream.range(0, optionIds.size())
                .mapToObj(i -> new TestQuestionView.Option(i + 1, optionText(wordCatalog.get(optionIds.get(i)), direction)))
                .toList();
        return new TestQuestionView(q.getQuestionNo(),
                promptText(wordCatalog.get(q.answerLevelWordId()), direction), optionViews);
    }

    private String promptText(CatalogWord answer, Direction direction) {
        return direction == Direction.EN_KO ? answer.headword() : answer.meaningText();
    }

    private String optionText(CatalogWord option, Direction direction) {
        return direction == Direction.EN_KO ? option.meaningText() : option.headword();
    }

    private TestSession findWithQuestions(UUID sessionId) {
        return testSessionRepository.findByIdWithQuestions(sessionId)
                .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND, "시험을 찾을 수 없습니다."));
    }

    private void requireInProgress(TestSession session) {
        if (!session.isInProgress()) {
            throw new CustomException(ErrorCode.TEST_NOT_IN_PROGRESS);
        }
    }

}

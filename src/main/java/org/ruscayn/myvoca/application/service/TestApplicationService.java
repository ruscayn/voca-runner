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
import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;
import org.ruscayn.myvoca.domain.repository.LevelWordRepository;
import org.ruscayn.myvoca.domain.repository.TestSessionRepository;
import org.ruscayn.myvoca.domain.service.TestQuestionGenerator;
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

    /** 시험 문항 수 (application.yaml : my-voca.test.question-count) */
    @Value("${my-voca.test.question-count:" + TestSession.DEFAULT_QUESTION_COUNT + "}")
    private int questionCount;

    /** 시험 시작 : 문제 생성 후 저장. 1단계는 EN_KO 고정 */
    @Transactional
    public UUID start(Level level) {
        long begin = System.currentTimeMillis();
        List<LevelWord> candidates = levelWordRepository.findAllByLevels(level.detailLevels());

        TestSession session = TestSession.start(level, Direction.EN_KO, questionCount, TestSession.DEFAULT_TIME_LIMIT_SEC);
        testQuestionGenerator.generate(session, candidates);
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
                    LevelWord selected = q.selectedOption();
                    return new TestResultResponse.WrongItem(
                            q.getQuestionNo(),
                            promptText(q.getAnswer(), direction),
                            selected == null ? null : optionText(selected, direction),
                            optionText(q.getAnswer(), direction),
                            q.getAnswer().headword());
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
        List<LevelWord> options = q.options();
        List<TestQuestionView.Option> optionViews = IntStream.range(0, options.size())
                .mapToObj(i -> new TestQuestionView.Option(i + 1, optionText(options.get(i), direction)))
                .toList();
        return new TestQuestionView(q.getQuestionNo(), promptText(q.getAnswer(), direction), optionViews);
    }

    private String promptText(LevelWord answer, Direction direction) {
        return direction == Direction.EN_KO ? answer.headword() : String.join(", ", answer.displayMeaningTexts());
    }

    private String optionText(LevelWord option, Direction direction) {
        return direction == Direction.EN_KO ? String.join(", ", option.displayMeaningTexts()) : option.headword();
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

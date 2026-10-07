package org.ruscayn.myvoca.domain.model.test;

import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;
import org.ruscayn.myvoca.shared.common.BaseEntity;
import org.ruscayn.myvoca.shared.exception.CustomException;
import org.ruscayn.myvoca.shared.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 시험 1회 (test_session). 문제(TestQuestion)를 포함하는 Aggregate Root.
 */
@Getter
@Entity
@Table(name = "test_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestSession extends BaseEntity {

    public static final int DEFAULT_QUESTION_COUNT = 500;
    public static final int DEFAULT_TIME_LIMIT_SEC = 5;
    public static final int MIN_TIME_LIMIT_SEC = 5;
    public static final int MAX_TIME_LIMIT_SEC = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    private UUID id;

    /** 2단계(로그인)에서 사용. 1단계는 항상 null */
    @Column(name = "user_id")
    private Long userId;

    /** 사용자가 고른 레벨. 묶음(A/B/C) 또는 세부(A1~C2) */
    @Enumerated(EnumType.STRING)
    @Column(name = "level_code", nullable = false, length = 2)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private Direction direction;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "question_count", nullable = false)
    private Integer questionCount;

    /** 문제당 제한 시간(초) 5~10 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "time_limit_sec", nullable = false)
    private Integer timeLimitSec;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private TestStatus status;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "correct_count")
    private Integer correctCount;

    /** 0~100 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "score")
    private Integer score;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNo ASC")
    private List<TestQuestion> questions = new ArrayList<>();

    private TestSession(Level level, Direction direction, int questionCount, int timeLimitSec) {
        if (questionCount < 1) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "문제 수는 1 이상이어야 합니다.");
        }
        if (timeLimitSec < MIN_TIME_LIMIT_SEC || timeLimitSec > MAX_TIME_LIMIT_SEC) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                    "제한 시간은 " + MIN_TIME_LIMIT_SEC + "~" + MAX_TIME_LIMIT_SEC + "초여야 합니다.");
        }
        this.level = level;
        this.direction = direction;
        this.questionCount = questionCount;
        this.timeLimitSec = timeLimitSec;
        this.status = TestStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    /** 시험 시작 (1단계 기본값: 500문항, 5초) */
    public static TestSession start(Level level, Direction direction) {
        return new TestSession(level, direction, DEFAULT_QUESTION_COUNT, DEFAULT_TIME_LIMIT_SEC);
    }

    public static TestSession start(Level level, Direction direction, int questionCount, int timeLimitSec) {
        return new TestSession(level, direction, questionCount, timeLimitSec);
    }

    /** 문제 추가. 번호는 1부터 순서대로 부여 */
    public TestQuestion addQuestion(LevelWord answer, List<LevelWord> options) {
        TestQuestion question = TestQuestion.create(this, questions.size() + 1, answer, options);
        questions.add(question);
        return question;
    }

    /**
     * 제출 및 채점.
     *
     * @param selectedByQuestionNo 문제 번호 → 고른 보기 번호(1~4). 시간 초과/미응답 문제는 없거나 null
     */
    public void submit(Map<Integer, Integer> selectedByQuestionNo) {
        requireInProgress();
        int correct = 0;
        for (TestQuestion question : questions) {
            question.answer(selectedByQuestionNo.get(question.getQuestionNo()));
            if (Boolean.TRUE.equals(question.getIsCorrect())) {
                correct++;
            }
        }
        this.correctCount = correct;
        this.score = (int) Math.round(correct * 100.0 / questionCount);
        this.status = TestStatus.COMPLETED;
        this.submittedAt = Instant.now();
    }

    /** 만료 처리 (처리 방식은 미결 – 우선 상태만 변경) */
    public void expire() {
        requireInProgress();
        this.status = TestStatus.EXPIRED;
    }

    public boolean isInProgress() {
        return status == TestStatus.IN_PROGRESS;
    }

    public List<TestQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    private void requireInProgress() {
        if (!isInProgress()) {
            throw new CustomException(ErrorCode.TEST_NOT_IN_PROGRESS);
        }
    }

}

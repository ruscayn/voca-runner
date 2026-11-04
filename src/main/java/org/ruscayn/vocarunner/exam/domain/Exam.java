package org.ruscayn.vocarunner.exam.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.Getter;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.Level;

@Getter
public class Exam {

    public static final int DEFAULT_TIME_PER_QUESTION_SEC = 5;
    public static final int MIN_TIME_PER_QUESTION_SEC = 5;
    public static final int MAX_TIME_PER_QUESTION_SEC = 10;

    private final UUID id;
    private final Level level;
    private final Direction direction;
    private final int timePerQuestionSec;
    private final Examinee examinee;
    private final Instant startedAt;
    private final Instant deadline;
    private final AnswerKey answerKey;
    private SelectedOptions selectedOptions;
    private ExamStatus status;
    private boolean questionsDelivered;
    private Integer correctCount;
    private Integer score;
    private Instant submittedAt;
    private final Integer version;

    // CHECKSTYLE.SUPPRESS: MethodLength — 매개변수를 필드에 옮겨 담기만 함
    private Exam(UUID id, Level level, Direction direction, int timePerQuestionSec, Examinee examinee,
                 Instant startedAt, Instant deadline, AnswerKey answerKey, SelectedOptions selectedOptions,
                 ExamStatus status, boolean questionsDelivered, Integer correctCount, Integer score,
                 Instant submittedAt, Integer version) {
        this.id = id;
        this.level = level;
        this.direction = direction;
        this.timePerQuestionSec = timePerQuestionSec;
        this.examinee = examinee;
        this.startedAt = startedAt;
        this.deadline = deadline;
        this.answerKey = answerKey;
        this.selectedOptions = selectedOptions;
        this.status = status;
        this.questionsDelivered = questionsDelivered;
        this.correctCount = correctCount;
        this.score = score;
        this.submittedAt = submittedAt;
        this.version = version;
    }

    public static Exam start(Level level, Direction direction, int timePerQuestionSec,
                             List<Question> questions, Examinee examinee,
                             Instant now, Duration grace) {
        requireQuestions(questions);
        requireTimePerQuestion(timePerQuestionSec);
        int count = questions.size();
        return new Exam(UUID.randomUUID(), level, direction, timePerQuestionSec, examinee,
                now, now.plus(allowedTime(count, timePerQuestionSec, grace)),
                AnswerKey.from(questions), SelectedOptions.none(count),
                ExamStatus.IN_PROGRESS, false, null, null, null, null);
    }

    private static void requireQuestions(List<Question> questions) {
        if (questions.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "문제 수는 1 이상이어야 합니다.");
        }
    }

    private static void requireTimePerQuestion(int timePerQuestionSec) {
        if (timePerQuestionSec < MIN_TIME_PER_QUESTION_SEC || timePerQuestionSec > MAX_TIME_PER_QUESTION_SEC) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                    "문항당 시간은 " + MIN_TIME_PER_QUESTION_SEC + "~" + MAX_TIME_PER_QUESTION_SEC + "초여야 합니다.");
        }
    }

    public static Exam withId(UUID id, Level level, Direction direction, int timePerQuestionSec, Examinee examinee,
                              Instant startedAt, Instant deadline, AnswerKey answerKey, SelectedOptions selectedOptions,
                              ExamStatus status, boolean questionsDelivered, Integer correctCount, Integer score,
                              Instant submittedAt, Integer version) {
        if (selectedOptions.questionCount() != answerKey.questionCount()) {
            throw new IllegalArgumentException("문항 배열 길이가 맞지 않습니다: exam=" + id);
        }
        return new Exam(id, level, direction, timePerQuestionSec, examinee, startedAt, deadline,
                answerKey, selectedOptions, status, questionsDelivered, correctCount, score, submittedAt, version);
    }

    /**
     * (문항 수 × 문항당 시간 + 여유 시간)을 분 단위로 올림. 안내 문구를 "45분"처럼 분 단위로 보여 주므로 마감도 같은 값으로 맞춘다.
     * 예: 500문항 × 5초 + 3분 = 44분 40초 → 45분
     */
    public static Duration allowedTime(int questionCount, int timePerQuestionSec, Duration grace) {
        long seconds = Duration.ofSeconds((long) questionCount * timePerQuestionSec).plus(grace).toSeconds();
        return Duration.ofMinutes(Math.ceilDiv(seconds, 60));
    }

    public void submit(Map<Integer, Integer> selectedByQuestionNo, Instant now) {
        requireInProgress();
        expireIfPastDeadline(now);
        this.selectedOptions = SelectedOptions.record(selectedByQuestionNo, getQuestionCount());
        this.correctCount = countCorrect();
        this.score = toScore(correctCount);
        this.submittedAt = now;
        this.status = ExamStatus.COMPLETED;
    }

    private void expireIfPastDeadline(Instant now) {
        if (now.isAfter(deadline)) {
            status = ExamStatus.EXPIRED;
            throw new CustomException(ErrorCode.EXAM_EXPIRED);
        }
    }

    private int countCorrect() {
        return (int) IntStream.rangeClosed(1, getQuestionCount()).filter(this::isCorrect).count();
    }

    /** 반올림이 아니라 버림 : 틀린 문제가 있으면 100점이 되지 않게 (반올림이면 997문항 중 995개를 맞혀도 100점) */
    private int toScore(int correct) {
        return correct * 100 / getQuestionCount();
    }

    public void markQuestionsDelivered() {
        this.questionsDelivered = true;
    }

    public void expire() {
        if (status == ExamStatus.IN_PROGRESS) {
            status = ExamStatus.EXPIRED;
        }
    }

    public int getQuestionCount() {
        return answerKey.questionCount();
    }

    /** 문제 받기 · 제출 · 중단은 시험을 시작한 기기만 할 수 있다 */
    public boolean isStartedBy(String deviceId) {
        return examinee.deviceId().equals(deviceId);
    }

    public boolean isInProgress(Instant now) {
        return status == ExamStatus.IN_PROGRESS && !now.isAfter(deadline);
    }

    public boolean isCompleted() {
        return status == ExamStatus.COMPLETED;
    }

    public long answerLevelWordId(int no) {
        return answerKey.answerLevelWordId(no);
    }

    public List<Long> optionLevelWordIds(int no) {
        return answerKey.optionLevelWordIds(no);
    }

    public Long selectedLevelWordId(int no) {
        if (!selectedOptions.isSelected(no)) {
            return null;
        }
        return answerKey.optionLevelWordId(no, selectedOptions.optionNo(no));
    }

    public boolean isCorrect(int no) {
        return selectedOptions.isSelected(no) && answerKey.isAnswer(no, selectedOptions.optionNo(no));
    }

    private void requireInProgress() {
        if (status != ExamStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }
    }

}

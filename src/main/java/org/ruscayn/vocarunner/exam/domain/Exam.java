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
    private final ExamTime time;
    private final AnswerKey answerKey;
    private SelectedOptions selectedOptions;
    private ExamStatus status;
    private boolean questionsDelivered;
    private GradingResult gradingResult;
    private final Integer version;

    // CHECKSTYLE.SUPPRESS: MethodLength — 매개변수를 필드에 옮겨 담기만 함
    private Exam(UUID id, Level level, Direction direction, int timePerQuestionSec, Examinee examinee, ExamTime time,
                 AnswerKey answerKey, SelectedOptions selectedOptions, ExamStatus status, boolean questionsDelivered,
                 GradingResult gradingResult, Integer version) {
        this.id = id;
        this.level = level;
        this.direction = direction;
        this.timePerQuestionSec = timePerQuestionSec;
        this.examinee = examinee;
        this.time = time;
        this.answerKey = answerKey;
        this.selectedOptions = selectedOptions;
        this.status = status;
        this.questionsDelivered = questionsDelivered;
        this.gradingResult = gradingResult;
        this.version = version;
    }

    public static Exam start(Level level, Direction direction, int timePerQuestionSec,
                             List<Question> questions, Examinee examinee,
                             Instant now, Duration grace) {
        requireQuestions(questions);
        requireTimePerQuestion(timePerQuestionSec);
        int count = questions.size();
        return new Exam(UUID.randomUUID(), level, direction, timePerQuestionSec, examinee,
                ExamTime.start(now, count, timePerQuestionSec, grace), AnswerKey.from(questions),
                SelectedOptions.none(count), ExamStatus.IN_PROGRESS, false, null, null);
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
                              ExamTime time, AnswerKey answerKey, SelectedOptions selectedOptions, ExamStatus status,
                              boolean questionsDelivered, GradingResult gradingResult, Integer version) {
        if (selectedOptions.questionCount() != answerKey.questionCount()) {
            throw new IllegalArgumentException("문항 배열 길이가 맞지 않습니다: exam=" + id);
        }
        return new Exam(id, level, direction, timePerQuestionSec, examinee, time, answerKey, selectedOptions, status,
                questionsDelivered, gradingResult, version);
    }

    public static Duration allowedTime(int questionCount, int timePerQuestionSec, Duration grace) {
        return ExamTime.allowedTime(questionCount, timePerQuestionSec, grace);
    }

    public void submit(Map<Integer, Integer> selectedByQuestionNo, Instant now) {
        requireInProgress();
        expireIfPastDeadline(now);
        this.selectedOptions = SelectedOptions.record(selectedByQuestionNo, getQuestionCount());
        this.gradingResult = GradingResult.of(countCorrect(), getQuestionCount(), now);
        this.status = ExamStatus.COMPLETED;
    }

    private void expireIfPastDeadline(Instant now) {
        if (time.isPastDeadline(now)) {
            status = ExamStatus.EXPIRED;
            throw new CustomException(ErrorCode.EXAM_EXPIRED);
        }
    }

    private int countCorrect() {
        return (int) IntStream.rangeClosed(1, getQuestionCount()).filter(this::isCorrect).count();
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
        return status == ExamStatus.IN_PROGRESS && !time.isPastDeadline(now);
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

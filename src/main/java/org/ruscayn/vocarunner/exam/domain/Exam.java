package org.ruscayn.vocarunner.exam.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.Level;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Exam {

    public static final int DEFAULT_TIME_PER_QUESTION_SEC = 5;

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

    public static Exam start(Level level, List<Question> questions, Examinee examinee, Instant now, Duration grace) {
        int count = questions.size();
        return new Exam(UUID.randomUUID(), level, Direction.EN_KO, DEFAULT_TIME_PER_QUESTION_SEC, examinee,
                ExamTime.start(now, count, DEFAULT_TIME_PER_QUESTION_SEC, grace), AnswerKey.from(questions),
                SelectedOptions.none(count), ExamStatus.IN_PROGRESS, false, null, null);
    }

    public static Duration calculateAllowedTime(int questionCount, Duration grace) {
        return ExamTime.calculateAllowedTime(questionCount, DEFAULT_TIME_PER_QUESTION_SEC, grace);
    }

    public static Exam of(UUID id, Level level, Direction direction, int timePerQuestionSec, Examinee examinee,
                          ExamTime time, AnswerKey answerKey, SelectedOptions selectedOptions, ExamStatus status,
                          boolean questionsDelivered, GradingResult gradingResult, Integer version) {
        if (selectedOptions.questionCount() != answerKey.questionCount()) {
            throw new IllegalArgumentException("문항 배열 길이가 맞지 않습니다: exam=" + id);
        }
        return new Exam(id, level, direction, timePerQuestionSec, examinee, time, answerKey, selectedOptions, status,
                questionsDelivered, gradingResult, version);
    }

    public void deliverQuestions(Instant now) {
        if (!isInProgress(now)) {
            throw new CustomException(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }
        this.questionsDelivered = true;
    }

    public void submit(List<SelectedOption> selected, Instant now) {
        requireInProgress();
        requireNotPastDeadline(now);
        this.selectedOptions = SelectedOptions.record(selected, getQuestionCount());
        this.gradingResult = GradingResult.of(countCorrect(), getQuestionCount(), now);
        this.status = ExamStatus.COMPLETED;
    }

    private void requireInProgress() {
        if (status != ExamStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }
    }

    private void requireNotPastDeadline(Instant now) {
        if (time.isPastDeadline(now)) {
            throw new CustomException(ErrorCode.EXAM_EXPIRED);
        }
    }

    private int countCorrect() {
        return (int) IntStream.rangeClosed(1, getQuestionCount()).filter(this::isCorrect).count();
    }

    public boolean isAbortableBy(String deviceId, Instant now) {
        return isStartedBy(deviceId) && isInProgress(now);
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

    public void requireCompleted() {
        if (!isCompleted()) {
            throw new CustomException(ErrorCode.EXAM_NOT_COMPLETED);
        }
    }

    public long answerOptionId(int no) {
        return answerKey.answerOptionId(no);
    }

    public List<Long> optionIds(int no) {
        return answerKey.optionIds(no);
    }

    public Long selectedOptionId(int no) {
        if (!selectedOptions.isSelected(no)) {
            return null;
        }
        return answerKey.optionId(no, selectedOptions.optionNo(no));
    }

    public boolean isCorrect(int no) {
        return selectedOptions.isSelected(no) && answerKey.isAnswer(no, selectedOptions.optionNo(no));
    }

    public List<Integer> wrongQuestionNos() {
        return IntStream.rangeClosed(1, getQuestionCount())
                .filter(no -> !isCorrect(no))
                .boxed()
                .toList();
    }

}

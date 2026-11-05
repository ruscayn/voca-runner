package org.ruscayn.vocarunner.exam.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    private final Subject subject;
    private final int timePerQuestionSec;
    private final Examinee examinee;
    private ExamTime time;
    private final Questions questions;
    private SelectedOptions selectedOptions;
    private ExamStatus status;
    private boolean questionsDelivered;
    private GradingResult gradingResult;
    private final Integer version;

    public static Exam start(Level level, List<Question> questions, Examinee examinee, Instant now, Duration grace) {
        int count = questions.size();
        return new Exam(UUID.randomUUID(), level, Subject.ENGLISH, DEFAULT_TIME_PER_QUESTION_SEC, examinee,
                ExamTime.start(now, count, DEFAULT_TIME_PER_QUESTION_SEC, grace), Questions.from(questions),
                SelectedOptions.none(count), ExamStatus.IN_PROGRESS, false, null, null);
    }

    public static Duration calculateAllowedTime(int questionCount, Duration grace) {
        return ExamTime.calculateAllowedTime(questionCount, DEFAULT_TIME_PER_QUESTION_SEC, grace);
    }

    public static Exam of(UUID id, Level level, Subject subject, int timePerQuestionSec, Examinee examinee,
                          ExamTime time, Questions questions, SelectedOptions selectedOptions, ExamStatus status,
                          boolean questionsDelivered, GradingResult gradingResult, Integer version) {
        if (questions.getQuestionCount() != selectedOptions.getQuestionCount()) {
            throw new IllegalArgumentException("문항 목록과 선택한 보기의 문항 수가 맞지 않습니다: exam=" + id);
        }
        return new Exam(id, level, subject, timePerQuestionSec, examinee, time, questions, selectedOptions, status,
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
        this.gradingResult = GradingResult.of(questions.countCorrect(selectedOptions), getQuestionCount());
        this.time = time.submit(now);
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

    public boolean isAbortableBy(String deviceId, Instant now) {
        return isStartedBy(deviceId) && isInProgress(now);
    }

    public int getQuestionCount() {
        return questions.getQuestionCount();
    }

    /** 문항 전달 · 제출 · 중단은 시험을 시작한 기기만 할 수 있다 */
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

    public Long selectedOptionId(int no) {
        return questions.selectedOptionId(no, selectedOptions);
    }

    public List<Integer> getWrongQuestionNos() {
        return questions.findWrongQuestionNos(selectedOptions);
    }

}

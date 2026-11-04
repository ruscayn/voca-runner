package org.ruscayn.vocarunner.exam.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.Getter;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.Level;

/**
 * 문항은 배열로 보관한다.
 * <ul>
 *   <li>optionIds : 보기 level_word_id. 문항 n(1부터)의 보기 i(1~4) = optionIds[(n-1)*4 + (i-1)]</li>
 *   <li>answerOptionNo : 정답 보기 번호 1~4</li>
 *   <li>selectedOptionNo : 고른 보기 번호 1~4, 0 = 미입력(시간 초과)</li>
 * </ul>
 */
@Getter
public class Exam {

    public static final int DEFAULT_TIME_PER_QUESTION_SEC = 5;
    public static final int MIN_TIME_PER_QUESTION_SEC = 5;
    public static final int MAX_TIME_PER_QUESTION_SEC = 10;

    private static final int OPTION_COUNT = Question.OPTION_COUNT;

    private final UUID id;
    private final Level level;
    private final Direction direction;
    private final int questionCount;
    private final int timePerQuestionSec;

    @Getter(AccessLevel.NONE)
    private final String deviceId;

    @Getter(AccessLevel.NONE)
    private final String clientIp;

    private final Instant startedAt;

    private final Instant deadline;

    @Getter(AccessLevel.NONE)
    private final int[] optionIds;

    @Getter(AccessLevel.NONE)
    private final byte[] answerOptionNo;

    @Getter(AccessLevel.NONE)
    private final byte[] selectedOptionNo;

    private ExamStatus status;

    private boolean questionsDelivered;

    private Integer correctCount;

    private Integer score;

    private Instant submittedAt;

    private final Integer version;

    // CHECKSTYLE.SUPPRESS: MethodLength — 매개변수를 필드에 옮겨 담기만 함 (매개변수 줄이기는 클래스 단계에서)
    private Exam(UUID id, Level level, Direction direction, int timePerQuestionSec, Examinee examinee,
                 Instant startedAt, Instant deadline, int[] optionIds, byte[] answerOptionNo, byte[] selectedOptionNo,
                 ExamStatus status, boolean questionsDelivered, Integer correctCount, Integer score,
                 Instant submittedAt, Integer version) {
        this.id = id;
        this.level = level;
        this.direction = direction;
        this.questionCount = answerOptionNo.length;
        this.timePerQuestionSec = timePerQuestionSec;
        this.deviceId = examinee.deviceId();
        this.clientIp = examinee.clientIp();
        this.startedAt = startedAt;
        this.deadline = deadline;
        this.optionIds = optionIds;
        this.answerOptionNo = answerOptionNo;
        this.selectedOptionNo = selectedOptionNo;
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
                toOptionIds(questions), toAnswerOptionNo(questions), new byte[count],
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

    private static int[] toOptionIds(List<Question> questions) {
        return questions.stream()
                .flatMap(question -> question.options().stream())
                .mapToInt(option -> Math.toIntExact(option.levelWordId()))
                .toArray();
    }

    private static byte[] toAnswerOptionNo(List<Question> questions) {
        byte[] answerOptionNo = new byte[questions.size()];
        for (int q = 0; q < questions.size(); q++) {
            answerOptionNo[q] = (byte) questions.get(q).answerOptionNo();
        }
        return answerOptionNo;
    }

    public static Exam withId(UUID id, Level level, Direction direction, int timePerQuestionSec, Examinee examinee,
                              Instant startedAt, Instant deadline,
                              int[] optionIds, byte[] answerOptionNo, byte[] selectedOptionNo,
                              ExamStatus status, boolean questionsDelivered, Integer correctCount, Integer score,
                              Instant submittedAt, Integer version) {
        int count = answerOptionNo.length;
        if (optionIds.length != count * OPTION_COUNT || selectedOptionNo.length != count) {
            throw new IllegalArgumentException("문항 배열 길이가 맞지 않습니다: exam=" + id);
        }
        return new Exam(id, level, direction, timePerQuestionSec, examinee, startedAt, deadline,
                optionIds.clone(), answerOptionNo.clone(), selectedOptionNo.clone(),
                status, questionsDelivered, correctCount, score, submittedAt, version);
    }

    /**
     * (문항 수 × 문항당 시간 + 여유 시간)을 분 단위로 올림. 안내 문구를 "45분"처럼 분 단위로 보여 주므로 마감도 같은 값으로 맞춘다.
     * 예: 500문항 × 5초 + 3분 = 44분 40초 → 45분
     */
    public static Duration allowedTime(int questionCount, int timePerQuestionSec, Duration grace) {
        long seconds = Duration.ofSeconds((long) questionCount * timePerQuestionSec).plus(grace).toSeconds();
        return Duration.ofMinutes(Math.ceilDiv(seconds, 60));
    }

    /** 보기 번호 검사를 먼저 모두 마친 뒤 기록한다 (거절할 때 일부만 바뀐 채 남지 않게) */
    public void submit(Map<Integer, Integer> selectedByQuestionNo, Instant now) {
        requireInProgress();
        expireIfPastDeadline(now);
        selectedByQuestionNo.forEach(Exam::requireValidOption);
        recordSelected(selectedByQuestionNo);
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

    private static void requireValidOption(Integer no, Integer selected) {
        if (selected != null && (selected < 1 || selected > OPTION_COUNT)) {
            throw new CustomException(ErrorCode.EXAM_INVALID_OPTION,
                    "문제 " + no + "의 보기 번호가 올바르지 않습니다: " + selected);
        }
    }

    private void recordSelected(Map<Integer, Integer> selectedByQuestionNo) {
        for (int q = 0; q < questionCount; q++) {
            Integer selected = selectedByQuestionNo.get(q + 1);
            selectedOptionNo[q] = selected == null ? 0 : selected.byteValue();
        }
    }

    private int countCorrect() {
        return (int) IntStream.rangeClosed(1, questionCount).filter(this::isCorrect).count();
    }

    /** 반올림이 아니라 버림 : 틀린 문제가 있으면 100점이 되지 않게 (반올림이면 997문항 중 995개를 맞혀도 100점) */
    private int toScore(int correct) {
        return correct * 100 / questionCount;
    }

    public void markQuestionsDelivered() {
        this.questionsDelivered = true;
    }

    public void expire() {
        if (status == ExamStatus.IN_PROGRESS) {
            status = ExamStatus.EXPIRED;
        }
    }

    public Examinee getExaminee() {
        return new Examinee(deviceId, clientIp);
    }

    /** 문제 받기 · 제출 · 중단은 시험을 시작한 기기만 할 수 있다 */
    public boolean isStartedBy(String deviceId) {
        return this.deviceId.equals(deviceId);
    }

    public boolean isInProgress(Instant now) {
        return status == ExamStatus.IN_PROGRESS && !now.isAfter(deadline);
    }

    public boolean isCompleted() {
        return status == ExamStatus.COMPLETED;
    }

    public int[] getOptionIds() {
        return optionIds.clone();
    }

    public byte[] getAnswerOptionNo() {
        return answerOptionNo.clone();
    }

    public byte[] getSelectedOptionNo() {
        return selectedOptionNo.clone();
    }

    public long answerLevelWordId(int no) {
        int q = index(no);
        return optionIds[q * OPTION_COUNT + answerOptionNo[q] - 1];
    }

    public List<Long> optionLevelWordIds(int no) {
        int q = index(no);
        List<Long> ids = new ArrayList<>(OPTION_COUNT);
        for (int i = 0; i < OPTION_COUNT; i++) {
            ids.add((long) optionIds[q * OPTION_COUNT + i]);
        }
        return ids;
    }

    public Long selectedLevelWordId(int no) {
        int q = index(no);
        int selected = selectedOptionNo[q];
        return selected == 0 ? null : (long) optionIds[q * OPTION_COUNT + selected - 1];
    }

    public boolean isCorrect(int no) {
        int q = index(no);
        return selectedOptionNo[q] != 0 && selectedOptionNo[q] == answerOptionNo[q];
    }

    private int index(int no) {
        if (no < 1 || no > questionCount) {
            throw new IllegalArgumentException("문제 번호 범위 밖: " + no);
        }
        return no - 1;
    }

    private void requireInProgress() {
        if (status != ExamStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }
    }

}

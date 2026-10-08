package org.ruscayn.vocarunner.domain.model.test;

import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 진행 중인 시험 1회 (정답지). DB 에 저장하지 않고 메모리에만 둔다.
 *
 * <p>메모리 절약을 위해 문항을 객체로 만들지 않고 배열로 보관한다.
 * <ul>
 *   <li>optionIds : 보기 level_word_id. 문항 n(1부터)의 보기 i(1~4) = optionIds[(n-1)*4 + (i-1)]</li>
 *   <li>correctOptionNo : 정답 보기 번호 1~4</li>
 *   <li>selectedOptionNo : 고른 보기 번호 1~4, 0 = 미입력(시간 초과)</li>
 * </ul>
 * 500문항 기준 약 10KB.
 *
 * <p>동시성: 상태 변경(submit, expire)은 synchronized. status 는 volatile 이며 마지막에 쓰므로,
 * 다른 스레드는 isCompleted() 확인 후 채점 결과를 읽으면 된다.
 */
@Getter
public class TestSession {

    public static final int DEFAULT_QUESTION_COUNT = 500;
    public static final int DEFAULT_TIME_LIMIT_SEC = 5;
    public static final int MIN_TIME_LIMIT_SEC = 5;
    public static final int MAX_TIME_LIMIT_SEC = 10;

    private static final int OPTION_COUNT = GeneratedQuestion.OPTION_COUNT;

    private final UUID id;
    private final Level level;
    private final Direction direction;
    private final int questionCount;
    private final int timeLimitSec;
    /** 시작한 사용자 구분 값 (현재는 IP). 동시 진행 개수 제한용 */
    private final String clientKey;
    private final Instant startedAt;
    /** 이 시각이 지나면 시험 무효 = 시작 + 문항 수 × 제한 시간 + 여유 시간 */
    private final Instant deadline;

    @Getter(AccessLevel.NONE)
    private final int[] optionIds;
    @Getter(AccessLevel.NONE)
    private final byte[] correctOptionNo;
    @Getter(AccessLevel.NONE)
    private final byte[] selectedOptionNo;

    private volatile TestStatus status;
    private int correctCount;
    /** 0~100 */
    private int score;
    private Instant submittedAt;

    private TestSession(Level level, Direction direction, int timeLimitSec, List<GeneratedQuestion> questions,
                        String clientKey, Instant now, Duration grace) {
        if (questions.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "문제 수는 1 이상이어야 합니다.");
        }
        if (timeLimitSec < MIN_TIME_LIMIT_SEC || timeLimitSec > MAX_TIME_LIMIT_SEC) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                    "제한 시간은 " + MIN_TIME_LIMIT_SEC + "~" + MAX_TIME_LIMIT_SEC + "초여야 합니다.");
        }
        int count = questions.size();
        this.id = UUID.randomUUID();
        this.level = level;
        this.direction = direction;
        this.questionCount = count;
        this.timeLimitSec = timeLimitSec;
        this.clientKey = clientKey;
        this.startedAt = now;
        this.deadline = now.plus(allowedDuration(count, timeLimitSec, grace));

        this.optionIds = new int[count * OPTION_COUNT];
        this.correctOptionNo = new byte[count];
        this.selectedOptionNo = new byte[count];
        for (int q = 0; q < count; q++) {
            GeneratedQuestion question = questions.get(q);
            for (int i = 0; i < OPTION_COUNT; i++) {
                optionIds[q * OPTION_COUNT + i] = Math.toIntExact(question.options().get(i).levelWordId());
            }
            correctOptionNo[q] = (byte) question.correctOptionNo();
        }
        this.status = TestStatus.IN_PROGRESS;
    }

    public static TestSession start(Level level, Direction direction, int timeLimitSec,
                                    List<GeneratedQuestion> questions, String clientKey,
                                    Instant now, Duration grace) {
        return new TestSession(level, direction, timeLimitSec, questions, clientKey, now, grace);
    }

    /** 시험을 마쳐야 하는 전체 시간 = 문항 수 × 제한 시간 + 여유 시간 (시작 전 안내에도 사용) */
    public static Duration allowedDuration(int questionCount, int timeLimitSec, Duration grace) {
        return Duration.ofSeconds((long) questionCount * timeLimitSec).plus(grace);
    }

    // ---------- 상태 변경 ----------

    /**
     * 제출 및 채점. 마감 시각이 지났으면 무효(EXPIRED) 처리 후 예외.
     *
     * @param selectedByQuestionNo 문제 번호 → 고른 보기 번호(1~4). 시간 초과/미응답 문제는 없거나 null
     */
    public synchronized void submit(Map<Integer, Integer> selectedByQuestionNo, Instant now) {
        requireInProgress();
        if (now.isAfter(deadline)) {
            status = TestStatus.EXPIRED;
            throw new CustomException(ErrorCode.TEST_TIME_OVER);
        }
        int correct = 0;
        for (int q = 0; q < questionCount; q++) {
            int no = q + 1;
            Integer selected = selectedByQuestionNo.get(no);
            if (selected != null && (selected < 1 || selected > OPTION_COUNT)) {
                throw new CustomException(ErrorCode.TEST_INVALID_OPTION,
                        "문제 " + no + "의 보기 번호가 올바르지 않습니다: " + selected);
            }
            selectedOptionNo[q] = selected == null ? 0 : selected.byteValue();
            if (selected != null && selected == correctOptionNo[q]) {
                correct++;
            }
        }
        this.correctCount = correct;
        this.score = (int) Math.round(correct * 100.0 / questionCount);
        this.submittedAt = now;
        this.status = TestStatus.COMPLETED;   // 마지막에 기록 (volatile)
    }

    /** 중도 이탈 등으로 무효 처리. 진행 중이 아니면 무시 */
    public synchronized void expire() {
        if (status == TestStatus.IN_PROGRESS) {
            status = TestStatus.EXPIRED;
        }
    }

    // ---------- 상태 조회 ----------

    /** 진행 중이고 마감 시각 전인지 */
    public boolean isInProgress(Instant now) {
        return status == TestStatus.IN_PROGRESS && !now.isAfter(deadline);
    }

    public boolean isCompleted() {
        return status == TestStatus.COMPLETED;
    }

    /**
     * 메모리에서 지워도 되는지.
     * 진행 중 → 마감 시각 경과, 완료 → 결과 보관 시간 경과, 무효 → 즉시
     */
    public boolean isDisposable(Instant now, Duration resultRetention) {
        return switch (status) {
            case IN_PROGRESS -> now.isAfter(deadline);
            case COMPLETED -> now.isAfter(submittedAt.plus(resultRetention));
            case EXPIRED -> true;
        };
    }

    // ---------- 문항 조회 (no: 1부터) ----------

    /** 출제 단어 level_word_id */
    public long answerLevelWordId(int no) {
        int q = index(no);
        return optionIds[q * OPTION_COUNT + correctOptionNo[q] - 1];
    }

    /** 보기 1~4 의 level_word_id */
    public List<Long> optionLevelWordIds(int no) {
        int q = index(no);
        List<Long> ids = new ArrayList<>(OPTION_COUNT);
        for (int i = 0; i < OPTION_COUNT; i++) {
            ids.add((long) optionIds[q * OPTION_COUNT + i]);
        }
        return ids;
    }

    /** 고른 보기의 level_word_id. 미입력이면 null (채점 후에만 의미 있음) */
    public Long selectedLevelWordId(int no) {
        int q = index(no);
        int selected = selectedOptionNo[q];
        return selected == 0 ? null : (long) optionIds[q * OPTION_COUNT + selected - 1];
    }

    /** 맞혔는지 (채점 후에만 의미 있음) */
    public boolean isCorrect(int no) {
        int q = index(no);
        return selectedOptionNo[q] != 0 && selectedOptionNo[q] == correctOptionNo[q];
    }

    private int index(int no) {
        if (no < 1 || no > questionCount) {
            throw new IllegalArgumentException("문제 번호 범위 밖: " + no);
        }
        return no - 1;
    }

    private void requireInProgress() {
        if (status != TestStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.TEST_NOT_IN_PROGRESS);
        }
    }

}

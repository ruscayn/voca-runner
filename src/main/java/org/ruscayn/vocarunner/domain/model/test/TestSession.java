package org.ruscayn.vocarunner.domain.model.test;

import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.shared.common.BaseEntity;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 시험 1회 (정답지·고른 답·채점 결과). test_session 1행.
 * 재시작·배포 때 진행 중 시험이 사라지지 않도록 DB 에 저장한다 (U21).
 *
 * <p>문항을 행으로 나누지 않고 배열로 1행에 보관한다.
 * <ul>
 *   <li>optionIds : 보기 level_word_id. 문항 n(1부터)의 보기 i(1~4) = optionIds[(n-1)*4 + (i-1)]</li>
 *   <li>correctOptionNo : 정답 보기 번호 1~4</li>
 *   <li>selectedOptionNo : 고른 보기 번호 1~4, 0 = 미입력(시간 초과)</li>
 * </ul>
 * 500문항 기준 약 10KB.
 *
 * <p>동시성: 같은 시험을 동시에 두 번 제출하면 version(낙관적 잠금)으로 하나만 반영된다.
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

    private static final int OPTION_COUNT = GeneratedQuestion.OPTION_COUNT;

    @Id
    @Column(name = "test_session_id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_code", nullable = false, length = 2)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 10)
    private Direction direction;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "time_limit_sec", nullable = false)
    private int timeLimitSec;

    /** 시작한 기기 (쿠키). 기기당 진행 중 시험 1개 */
    @Getter(AccessLevel.NONE)
    @Column(name = "device_id", nullable = false, length = 36)
    private String deviceId;

    /** 시작한 IP. IP당 진행 중 시험 상한용 */
    @Getter(AccessLevel.NONE)
    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    /** 이 시각이 지나면 시험 무효 = 시작 + allowedDuration (분 단위 올림) */
    @Column(name = "deadline", nullable = false)
    private Instant deadline;

    @Getter(AccessLevel.NONE)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "option_ids", nullable = false)
    private int[] optionIds;

    @Getter(AccessLevel.NONE)
    @Column(name = "correct_option_no", nullable = false)
    private byte[] correctOptionNo;

    @Getter(AccessLevel.NONE)
    @Column(name = "selected_option_no", nullable = false)
    private byte[] selectedOptionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private TestStatus status;

    /** 시험 화면이 문제를 한 번이라도 받아 갔는지. 못 받아 간 시험(시작 직후 연결 끊김 등)을 일찍 정리하는 데 쓴다 */
    @Column(name = "questions_delivered", nullable = false)
    private boolean questionsDelivered;

    @Column(name = "correct_count")
    private Integer correctCount;

    /** 0~100 */
    @Column(name = "score")
    private Integer score;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    /** 낙관적 잠금. null 이면 새 행(INSERT) */
    @Getter(AccessLevel.NONE)
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    private TestSession(Level level, Direction direction, int timeLimitSec, List<GeneratedQuestion> questions,
                        TestClient client, Instant now, Duration grace) {
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
        this.deviceId = client.deviceId();
        this.clientIp = client.ip();
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
                                    List<GeneratedQuestion> questions, TestClient client,
                                    Instant now, Duration grace) {
        return new TestSession(level, direction, timeLimitSec, questions, client, now, grace);
    }

    /**
     * 시험을 마쳐야 하는 전체 시간 = (문항 수 × 제한 시간 + 여유 시간)을 분 단위로 올림.
     * 안내 문구를 "45분"처럼 분 단위로 보여주므로 실제 마감도 같은 값으로 맞춘다.
     * 예: 500문항 × 5초 + 3분 = 44분 40초 → 45분
     */
    public static Duration allowedDuration(int questionCount, int timeLimitSec, Duration grace) {
        long seconds = Duration.ofSeconds((long) questionCount * timeLimitSec).plus(grace).toSeconds();
        return Duration.ofMinutes(Math.ceilDiv(seconds, 60));
    }

    // ---------- 상태 변경 ----------

    /**
     * 제출 및 채점. 마감 시각이 지났으면 무효(EXPIRED) 처리 후 예외.
     *
     * @param selectedByQuestionNo 문제 번호 → 고른 보기 번호(1~4). 시간 초과/미응답 문제는 없거나 null
     */
    public void submit(Map<Integer, Integer> selectedByQuestionNo, Instant now) {
        requireInProgress();
        if (now.isAfter(deadline)) {
            status = TestStatus.EXPIRED;
            throw new CustomException(ErrorCode.TEST_TIME_OVER);
        }
        // 검사를 먼저 모두 마친 뒤 기록한다 (거절할 때 일부만 바뀐 채 남지 않게)
        selectedByQuestionNo.forEach((no, selected) -> {
            if (selected != null && (selected < 1 || selected > OPTION_COUNT)) {
                throw new CustomException(ErrorCode.TEST_INVALID_OPTION,
                        "문제 " + no + "의 보기 번호가 올바르지 않습니다: " + selected);
            }
        });
        int correct = 0;
        for (int q = 0; q < questionCount; q++) {
            Integer selected = selectedByQuestionNo.get(q + 1);
            selectedOptionNo[q] = selected == null ? 0 : selected.byteValue();
            if (selected != null && selected == correctOptionNo[q]) {
                correct++;
            }
        }
        this.correctCount = correct;
        this.score = (int) Math.round(correct * 100.0 / questionCount);
        this.submittedAt = now;
        this.status = TestStatus.COMPLETED;
    }

    /** 시험 화면이 문제를 받아 감 */
    public void markQuestionsDelivered() {
        this.questionsDelivered = true;
    }

    /** 중도 이탈 등으로 무효 처리. 진행 중이 아니면 무시 */
    public void expire() {
        if (status == TestStatus.IN_PROGRESS) {
            status = TestStatus.EXPIRED;
        }
    }

    // ---------- 상태 조회 ----------

    /** 시작한 사용자 (기기·IP) */
    public TestClient getClient() {
        return new TestClient(deviceId, clientIp);
    }

    /** 이 기기에서 시작한 시험인지. 문제 받기·제출·만료는 시작한 기기만 할 수 있다 (U9) */
    public boolean isStartedBy(String deviceId) {
        return this.deviceId.equals(deviceId);
    }

    /** 진행 중이고 마감 시각 전인지 */
    public boolean isInProgress(Instant now) {
        return status == TestStatus.IN_PROGRESS && !now.isAfter(deadline);
    }

    public boolean isCompleted() {
        return status == TestStatus.COMPLETED;
    }

    /**
     * 지워도 되는지 (정리 작업의 삭제 조건과 같다. DbTestSessionRepository.deleteDisposable).
     * 진행 중 → 마감 시각 경과 또는 시작 후 문제를 받아 가지 않은 채 undeliveredTimeout 경과,
     * 완료 → 결과 보관 시간 경과, 무효 → 즉시
     *
     * <p>문제를 못 받아 간 시험은 화면이 만료 요청도 못 보냈을 수 있어(연결 끊김) 마감까지 두면
     * 사용자당 동시 진행 개수를 차지해 새 시험을 시작할 수 없다 (B1)
     */
    public boolean isDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout) {
        return switch (status) {
            case IN_PROGRESS -> now.isAfter(deadline)
                    || (!questionsDelivered && now.isAfter(startedAt.plus(undeliveredTimeout)));
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

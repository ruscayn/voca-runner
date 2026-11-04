package org.ruscayn.vocarunner.exam.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.Level;

/**
 * 시험 1회 (정답지·고른 보기·채점 결과).
 * 재시작·배포 때 진행 중 시험이 사라지지 않도록 DB 에 저장한다 (U21, 저장은 영속성 어댑터가 함).
 *
 * <p>문항은 배열로 보관한다.
 * <ul>
 *   <li>optionIds : 보기 level_word_id. 문항 n(1부터)의 보기 i(1~4) = optionIds[(n-1)*4 + (i-1)]</li>
 *   <li>answerOptionNo : 정답 보기 번호 1~4</li>
 *   <li>selectedOptionNo : 고른 보기 번호 1~4, 0 = 미입력(시간 초과)</li>
 * </ul>
 *
 * <p>동시성: 같은 시험을 동시에 두 번 제출하면 version(낙관적 잠금)으로 하나만 반영된다.
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

    /** 시작한 기기 (쿠키). 기기당 진행 중 시험 1개 */
    @Getter(AccessLevel.NONE)
    private final String deviceId;

    /** 시작한 IP. IP당 최대 진행 중 확인용 */
    @Getter(AccessLevel.NONE)
    private final String clientIp;

    private final Instant startedAt;

    /** 마감 = 시작 + allowedTime (분 단위 올림). 이 시각이 지나면 만료 */
    private final Instant deadline;

    @Getter(AccessLevel.NONE)
    private final int[] optionIds;

    @Getter(AccessLevel.NONE)
    private final byte[] answerOptionNo;

    @Getter(AccessLevel.NONE)
    private final byte[] selectedOptionNo;

    private ExamStatus status;

    /** 시험 화면이 문제를 한 번이라도 받아 갔는지. 못 받아 간 시험(시작 직후 연결 끊김 등)을 일찍 정리하는 데 쓴다 */
    private boolean questionsDelivered;

    private Integer correctCount;

    /** 0~100 (버림) */
    private Integer score;

    private Instant submittedAt;

    /** 낙관적 잠금 번호. DB 가 저장할 때마다 올린다. null 이면 아직 저장 전 */
    private final Integer version;

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

    /** 새 시험 시작. 아이디를 새로 만들고, 문항을 배열로 옮긴다 */
    public static Exam start(Level level, Direction direction, int timePerQuestionSec,
                             List<Question> questions, Examinee examinee,
                             Instant now, Duration grace) {
        if (questions.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "문제 수는 1 이상이어야 합니다.");
        }
        if (timePerQuestionSec < MIN_TIME_PER_QUESTION_SEC || timePerQuestionSec > MAX_TIME_PER_QUESTION_SEC) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                    "문항당 시간은 " + MIN_TIME_PER_QUESTION_SEC + "~" + MAX_TIME_PER_QUESTION_SEC + "초여야 합니다.");
        }
        int count = questions.size();
        int[] optionIds = new int[count * OPTION_COUNT];
        byte[] answerOptionNo = new byte[count];
        for (int q = 0; q < count; q++) {
            Question question = questions.get(q);
            for (int i = 0; i < OPTION_COUNT; i++) {
                optionIds[q * OPTION_COUNT + i] = Math.toIntExact(question.options().get(i).levelWordId());
            }
            answerOptionNo[q] = (byte) question.answerOptionNo();
        }
        return new Exam(UUID.randomUUID(), level, direction, timePerQuestionSec, examinee,
                now, now.plus(allowedTime(count, timePerQuestionSec, grace)),
                optionIds, answerOptionNo, new byte[count],
                ExamStatus.IN_PROGRESS, false, null, null, null, null);
    }

    /** 저장된 시험을 그 상태 그대로 되살린다 (영속성 어댑터가 DB 에서 읽을 때) */
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
     * 시험을 마쳐야 하는 전체 시간 = (문항 수 × 문항당 시간 + 여유 시간)을 분 단위로 올림.
     * 안내 문구를 "45분"처럼 분 단위로 보여주므로 실제 마감도 같은 값으로 맞춘다.
     * 예: 500문항 × 5초 + 3분 = 44분 40초 → 45분
     */
    public static Duration allowedTime(int questionCount, int timePerQuestionSec, Duration grace) {
        long seconds = Duration.ofSeconds((long) questionCount * timePerQuestionSec).plus(grace).toSeconds();
        return Duration.ofMinutes(Math.ceilDiv(seconds, 60));
    }

    // ---------- 상태 변경 ----------

    /**
     * 제출 및 채점. 마감 시각이 지났으면 만료(EXPIRED) 처리 후 예외.
     *
     * @param selectedByQuestionNo 문제 번호 → 고른 보기 번호(1~4). 시간 초과/미응답 문제는 없거나 null
     */
    public void submit(Map<Integer, Integer> selectedByQuestionNo, Instant now) {
        requireInProgress();
        if (now.isAfter(deadline)) {
            status = ExamStatus.EXPIRED;
            throw new CustomException(ErrorCode.EXAM_EXPIRED);
        }
        // 검사를 먼저 모두 마친 뒤 기록한다 (거절할 때 일부만 바뀐 채 남지 않게)
        selectedByQuestionNo.forEach((no, selected) -> {
            if (selected != null && (selected < 1 || selected > OPTION_COUNT)) {
                throw new CustomException(ErrorCode.EXAM_INVALID_OPTION,
                        "문제 " + no + "의 보기 번호가 올바르지 않습니다: " + selected);
            }
        });
        int correct = 0;
        for (int q = 0; q < questionCount; q++) {
            Integer selected = selectedByQuestionNo.get(q + 1);
            selectedOptionNo[q] = selected == null ? 0 : selected.byteValue();
            if (selected != null && selected == answerOptionNo[q]) {
                correct++;
            }
        }
        this.correctCount = correct;
        // 버림 : 틀린 문제가 있으면 100점이 되지 않게 (반올림이면 997문항 중 995개도 100점, U1)
        this.score = correct * 100 / questionCount;
        this.submittedAt = now;
        this.status = ExamStatus.COMPLETED;
    }

    /** 시험 화면이 문제를 받아 감 */
    public void markQuestionsDelivered() {
        this.questionsDelivered = true;
    }

    /** 만료 처리 (진행 중이면 EXPIRED). 진행 중이 아니면 무시 */
    public void expire() {
        if (status == ExamStatus.IN_PROGRESS) {
            status = ExamStatus.EXPIRED;
        }
    }

    // ---------- 상태 조회 ----------

    /** 시작한 응시자 (기기·IP) */
    public Examinee getExaminee() {
        return new Examinee(deviceId, clientIp);
    }

    /** 이 기기에서 시작한 시험인지. 문제 받기·제출·중단은 시작한 기기만 할 수 있다 (U9) */
    public boolean isStartedBy(String deviceId) {
        return this.deviceId.equals(deviceId);
    }

    /** 진행 중이고 마감 시각 전인지 */
    public boolean isInProgress(Instant now) {
        return status == ExamStatus.IN_PROGRESS && !now.isAfter(deadline);
    }

    public boolean isCompleted() {
        return status == ExamStatus.COMPLETED;
    }

    /**
     * 지워도 되는지 (정리 작업의 삭제 조건과 같다. ExamPersistenceAdapter.deleteDisposable).
     * 진행 중 → 마감 시각 경과 또는 시작 후 문제를 받아 가지 않은 채 undeliveredTimeout 경과,
     * 완료 → 결과 보관 시간 경과, 만료 → 즉시
     *
     * <p>문제를 못 받아 간 시험은 화면이 중단 요청도 못 보냈을 수 있어(연결 끊김) 마감까지 두면
     * 동시 진행 수를 차지해 새 시험을 시작할 수 없다 (B1)
     */
    public boolean isDisposable(Instant now, Duration resultRetention, Duration undeliveredTimeout) {
        return switch (status) {
            case IN_PROGRESS -> now.isAfter(deadline)
                    || (!questionsDelivered && now.isAfter(startedAt.plus(undeliveredTimeout)));
            case COMPLETED -> now.isAfter(submittedAt.plus(resultRetention));
            case EXPIRED -> true;
        };
    }

    // ---------- 저장용 배열 (영속성 어댑터가 옮겨 담을 때. 복사본) ----------

    public int[] getOptionIds() {
        return optionIds.clone();
    }

    public byte[] getAnswerOptionNo() {
        return answerOptionNo.clone();
    }

    public byte[] getSelectedOptionNo() {
        return selectedOptionNo.clone();
    }

    // ---------- 문항 조회 (no: 1부터) ----------

    /** 정답의 레벨 단어 아이디 (level_word_id) */
    public long answerLevelWordId(int no) {
        int q = index(no);
        return optionIds[q * OPTION_COUNT + answerOptionNo[q] - 1];
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

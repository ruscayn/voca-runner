package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ruscayn.vocarunner.common.adapter.out.persistence.BaseEntity;
import org.ruscayn.vocarunner.exam.domain.Direction;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.word.domain.Level;

/**
 * 시험 1회의 DB 저장용 클래스 = test_session 1행. 도메인 Exam 과는 from · toDomain 으로 오간다.
 *
 * <p>문항을 행으로 나누지 않고 배열로 1행에 보관한다 (500문항 기준 약 10KB). 배열 모양은 Exam 과 같다.
 * 정답 보기 번호(answerOptionNo)의 칼럼 이름은 correct_option_no.
 *
 * <p>동시성: 같은 시험을 동시에 두 번 제출하면 version(낙관적 잠금)으로 하나만 반영된다.
 */
@Getter
@Entity
@Table(name = "test_session")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamJpaEntity extends BaseEntity {

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
    private int timePerQuestionSec;

    @Column(name = "device_id", nullable = false, length = 36)
    private String deviceId;

    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "deadline", nullable = false)
    private Instant deadline;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "option_ids", nullable = false)
    private int[] optionIds;

    @Column(name = "correct_option_no", nullable = false)
    private byte[] answerOptionNo;

    @Column(name = "selected_option_no", nullable = false)
    private byte[] selectedOptionNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private ExamStatus status;

    @Column(name = "questions_delivered", nullable = false)
    private boolean questionsDelivered;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "score")
    private Integer score;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    /** 낙관적 잠금. null 이면 새 행(INSERT) */
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    /** 도메인 → 새 행. 처음 저장할 때만 쓴다 (이미 있는 행은 update) */
    static ExamJpaEntity from(Exam exam) {
        ExamJpaEntity entity = new ExamJpaEntity();
        entity.id = exam.getId();
        entity.version = exam.getVersion();
        entity.copyStart(exam);
        entity.copyQuestions(exam);
        entity.update(exam);
        return entity;
    }

    /** 시작할 때 정해져 바뀌지 않는 레벨 · 출제 방향 · 문항당 시간 · 응시자 · 시각 */
    private void copyStart(Exam exam) {
        this.level = exam.getLevel();
        this.direction = exam.getDirection();
        this.timePerQuestionSec = exam.getTimePerQuestionSec();
        this.deviceId = exam.getExaminee().deviceId();
        this.clientIp = exam.getExaminee().clientIp();
        this.startedAt = exam.getStartedAt();
        this.deadline = exam.getDeadline();
    }

    /** 문항 (보기 · 정답 보기 번호). 시작할 때 정해져 바뀌지 않는다 */
    private void copyQuestions(Exam exam) {
        this.questionCount = exam.getQuestionCount();
        this.optionIds = exam.getOptionIds();
        this.answerOptionNo = exam.getAnswerOptionNo();
    }

    /**
     * 시험을 하며 바뀌는 상태만 도메인에서 옮겨 담는다 (상태 · 문제 받음 · 고른 보기 · 채점 · 제출 시각).
     * 나머지(레벨 · 문항 · 마감 등)는 시작할 때 정해져 바뀌지 않는다
     */
    void update(Exam exam) {
        this.status = exam.getStatus();
        this.questionsDelivered = exam.isQuestionsDelivered();
        this.selectedOptionNo = exam.getSelectedOptionNo();
        this.correctCount = exam.getCorrectCount();
        this.score = exam.getScore();
        this.submittedAt = exam.getSubmittedAt();
    }

    /** 행 → 도메인 */
    Exam toDomain() {
        return Exam.withId(id, level, direction, timePerQuestionSec, new Examinee(deviceId, clientIp),
                startedAt, deadline, optionIds, answerOptionNo, selectedOptionNo,
                status, questionsDelivered, correctCount, score, submittedAt, version);
    }

}

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
import org.ruscayn.vocarunner.exam.domain.AnswerKey;
import org.ruscayn.vocarunner.exam.domain.Direction;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.exam.domain.SelectedOptions;
import org.ruscayn.vocarunner.word.domain.Level;

/** 문항을 행으로 나누지 않고 배열로 한 행에 둔다 (500문항 기준 약 10KB) */
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

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    static ExamJpaEntity from(Exam exam) {
        ExamJpaEntity entity = new ExamJpaEntity();
        entity.id = exam.getId();
        entity.version = exam.getVersion();
        entity.copyFixedAtStart(exam);
        entity.copyQuestions(exam);
        entity.copyChangedState(exam);
        return entity;
    }

    private void copyFixedAtStart(Exam exam) {
        this.level = exam.getLevel();
        this.direction = exam.getDirection();
        this.timePerQuestionSec = exam.getTimePerQuestionSec();
        this.deviceId = exam.getExaminee().deviceId();
        this.clientIp = exam.getExaminee().clientIp();
        this.startedAt = exam.getStartedAt();
        this.deadline = exam.getDeadline();
    }

    private void copyQuestions(Exam exam) {
        this.questionCount = exam.getQuestionCount();
        this.optionIds = exam.getAnswerKey().toOptionIdArray();
        this.answerOptionNo = exam.getAnswerKey().toAnswerOptionNoArray();
    }

    void copyChangedState(Exam exam) {
        this.status = exam.getStatus();
        this.questionsDelivered = exam.isQuestionsDelivered();
        this.selectedOptionNo = exam.getSelectedOptions().toSelectedOptionNoArray();
        this.correctCount = exam.getCorrectCount();
        this.score = exam.getScore();
        this.submittedAt = exam.getSubmittedAt();
    }

    Exam toDomain() {
        return Exam.withId(id, level, direction, timePerQuestionSec, new Examinee(deviceId, clientIp),
                startedAt, deadline, AnswerKey.of(optionIds, answerOptionNo), SelectedOptions.of(selectedOptionNo),
                status, questionsDelivered, correctCount, score, submittedAt, version);
    }

}

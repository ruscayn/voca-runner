package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ruscayn.vocarunner.common.adapter.out.persistence.BaseEntity;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.ExamStatus;
import org.ruscayn.vocarunner.exam.domain.GradingResult;
import org.ruscayn.vocarunner.exam.domain.Subject;
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
    private Subject subject;

    @Column(name = "time_limit_sec", nullable = false)
    private int timePerQuestionSec;

    @Embedded
    private ExamineeEmbeddable examinee;

    @Embedded
    private ExamTimeEmbeddable time;

    @Embedded
    private QuestionsEmbeddable questions;

    @Embedded
    private SelectedOptionsEmbeddable selectedOptions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private ExamStatus status;

    @Column(name = "questions_delivered", nullable = false)
    private boolean questionsDelivered;

    @Embedded
    private GradingResultEmbeddable gradingResult;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    static ExamJpaEntity from(Exam exam) {
        ExamJpaEntity entity = new ExamJpaEntity();
        entity.id = exam.getId();
        entity.version = exam.getVersion();
        entity.copyFixedAtStart(exam);
        entity.copyChangedState(exam);
        return entity;
    }

    private void copyFixedAtStart(Exam exam) {
        this.level = exam.getLevel();
        this.subject = exam.getSubject();
        this.timePerQuestionSec = exam.getTimePerQuestionSec();
        this.examinee = ExamineeEmbeddable.from(exam.getExaminee());
        this.questions = QuestionsEmbeddable.from(exam.getQuestions());
    }

    void copyChangedState(Exam exam) {
        this.status = exam.getStatus();
        this.questionsDelivered = exam.isQuestionsDelivered();
        this.time = ExamTimeEmbeddable.from(exam.getTime());
        this.selectedOptions = SelectedOptionsEmbeddable.from(exam.getSelectedOptions());
        this.gradingResult = GradingResultEmbeddable.from(exam.getGradingResult());
    }

    Exam toDomain() {
        return Exam.of(id, level, subject, timePerQuestionSec, examinee.toDomain(), time.toDomain(),
                questions.toDomain(), selectedOptions.toDomain(), status, questionsDelivered,
                toGradingResult(), version);
    }

    private GradingResult toGradingResult() {
        if (gradingResult == null) {
            return null;
        }
        return gradingResult.toDomain();
    }

}

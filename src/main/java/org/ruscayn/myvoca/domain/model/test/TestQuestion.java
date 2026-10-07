package org.ruscayn.myvoca.domain.model.test;

import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;
import org.ruscayn.myvoca.shared.common.BaseEntity;
import org.ruscayn.myvoca.shared.exception.CustomException;
import org.ruscayn.myvoca.shared.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * 시험 문제 (test_question). 시작 시 생성되고, 제출 시 채점 결과가 저장된다.
 * 생성은 TestSession.addQuestion 으로만 한다.
 */
@Getter
@Entity
@Table(name = "test_question")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TestQuestion extends BaseEntity {

    public static final int OPTION_COUNT = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private TestSession session;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "question_no", nullable = false)
    private Integer questionNo;

    /** 출제 단어 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_level_word_id", nullable = false)
    private LevelWord answer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option1_id", nullable = false)
    private LevelWord option1;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option2_id", nullable = false)
    private LevelWord option2;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option3_id", nullable = false)
    private LevelWord option3;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option4_id", nullable = false)
    private LevelWord option4;

    /** 정답 보기 번호 1~4 */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "correct_option_no", nullable = false)
    private Integer correctOptionNo;

    /** 고른 보기 번호 1~4. 시간 초과면 null */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "selected_option_no")
    private Integer selectedOptionNo;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    static TestQuestion create(TestSession session, int questionNo, LevelWord answer, List<LevelWord> options) {
        if (options.size() != OPTION_COUNT) {
            throw new IllegalArgumentException("보기는 " + OPTION_COUNT + "개여야 합니다: " + options.size());
        }
        int correctIndex = -1;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).getId().equals(answer.getId())) {
                if (correctIndex >= 0) {
                    throw new IllegalArgumentException("정답이 보기에 중복으로 들어 있습니다.");
                }
                correctIndex = i;
            }
        }
        if (correctIndex < 0) {
            throw new IllegalArgumentException("정답이 보기에 없습니다.");
        }

        TestQuestion q = new TestQuestion();
        q.session = session;
        q.questionNo = questionNo;
        q.answer = answer;
        q.option1 = options.get(0);
        q.option2 = options.get(1);
        q.option3 = options.get(2);
        q.option4 = options.get(3);
        q.correctOptionNo = correctIndex + 1;
        return q;
    }

    /** 보기 1~4 순서대로 */
    public List<LevelWord> options() {
        return List.of(option1, option2, option3, option4);
    }

    /** 사용자가 고른 보기. 시간 초과(미입력)면 null */
    public LevelWord selectedOption() {
        return selectedOptionNo == null ? null : options().get(selectedOptionNo - 1);
    }

    /** 채점. selectedOptionNo 가 null 이면 시간 초과(오답) */
    void answer(Integer selectedOptionNo) {
        if (selectedOptionNo != null && (selectedOptionNo < 1 || selectedOptionNo > OPTION_COUNT)) {
            throw new CustomException(ErrorCode.TEST_INVALID_OPTION,
                    "문제 " + questionNo + "의 보기 번호가 올바르지 않습니다: " + selectedOptionNo);
        }
        this.selectedOptionNo = selectedOptionNo;
        this.isCorrect = selectedOptionNo != null && selectedOptionNo.equals(correctOptionNo);
    }

}

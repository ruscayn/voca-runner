package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ruscayn.vocarunner.exam.domain.Questions;

@Embeddable
public record QuestionsEmbeddable(
        @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "option_ids", nullable = false) int[] optionIds,
        @Column(name = "correct_option_no", nullable = false) byte[] answerOptionNo
) {

    static QuestionsEmbeddable from(Questions questions) {
        return new QuestionsEmbeddable(questions.toOptionIdArray(), questions.toAnswerOptionNoArray());
    }

    Questions toDomain() {
        return Questions.of(optionIds, answerOptionNo);
    }

}

package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.ruscayn.vocarunner.exam.domain.AnswerKey;

@Embeddable
public record AnswerKeyEmbeddable(
        @Column(name = "correct_option_no", nullable = false) byte[] answerOptionNo
) {

    static AnswerKeyEmbeddable from(AnswerKey answerKey) {
        return new AnswerKeyEmbeddable(answerKey.toAnswerOptionNoArray());
    }

    AnswerKey toDomain() {
        return AnswerKey.of(answerOptionNo);
    }

}

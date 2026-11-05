package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.ruscayn.vocarunner.exam.domain.GradingResult;

@Embeddable
public record GradingResultEmbeddable(
        @Column(name = "correct_count") Integer correctCount,
        @Column(name = "score") Integer score
) {

    static GradingResultEmbeddable from(GradingResult gradingResult) {
        if (gradingResult == null) {
            return null;
        }
        return new GradingResultEmbeddable(gradingResult.correctCount(), gradingResult.score());
    }

    GradingResult toDomain() {
        return new GradingResult(correctCount, score);
    }

}

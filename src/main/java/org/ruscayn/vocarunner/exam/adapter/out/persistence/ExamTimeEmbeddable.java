package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import org.ruscayn.vocarunner.exam.domain.ExamTime;

@Embeddable
public record ExamTimeEmbeddable(
        @Column(name = "started_at", nullable = false) Instant startedAt,
        @Column(name = "deadline", nullable = false) Instant deadline,
        @Column(name = "submitted_at") Instant submittedAt
) {

    static ExamTimeEmbeddable from(ExamTime time) {
        return new ExamTimeEmbeddable(time.startedAt(), time.deadline(), time.submittedAt());
    }

    ExamTime toDomain() {
        return new ExamTime(startedAt, deadline, submittedAt);
    }

}

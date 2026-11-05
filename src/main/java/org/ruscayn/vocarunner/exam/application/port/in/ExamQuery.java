package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.Optional;
import java.util.UUID;

public interface ExamQuery {

    boolean isInProgress(UUID examId);

    boolean hasResult(UUID examId);

    ExamResult getResult(UUID examId);

    Optional<ExamResult> findResult(UUID examId);

}

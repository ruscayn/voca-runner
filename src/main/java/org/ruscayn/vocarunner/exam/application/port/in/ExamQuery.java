package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamQuery {

    Optional<ExamResult> findResult(UUID examId);

    boolean hasResult(UUID examId);

    ExamResult getResult(UUID examId);

    boolean isInProgress(UUID examId);

    List<LevelGroupSummary> getDetailLevelGroups();

}

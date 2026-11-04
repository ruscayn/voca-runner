package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamUseCase {

    UUID start(StartExamCommand command);

    ExamQuestionsResult getQuestions(UUID examId, String deviceId);

    SubmitExamResult submit(SubmitExamCommand command);

    Optional<ExamResult> findResult(UUID examId);

    boolean hasResult(UUID examId);

    ExamResult getResult(UUID examId);

    void abortIfInProgress(UUID examId, String deviceId);

    boolean isInProgress(UUID examId);

    int cleanup();

    List<LevelGroupSummary> getDetailLevelGroups();
}

package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.UUID;

public interface ExamUseCase {

    ExamQuestionsResult deliverQuestions(UUID examId, String deviceId);

    SubmitExamResult submit(SubmitExamCommand command);

    void abortIfInProgress(UUID examId, String deviceId);

}

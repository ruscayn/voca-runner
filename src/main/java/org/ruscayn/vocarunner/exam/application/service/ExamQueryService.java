package org.ruscayn.vocarunner.exam.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamResult;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.GradingResult;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ExamQueryService implements ExamQuery {

    private final ExamPersistencePort examPersistencePort;
    private final WordCatalog wordCatalog;
    private final ExamProperties examProperties;

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamResult> findResult(UUID examId) {
        return examPersistencePort.findById(examId)
                .filter(Exam::isCompleted)
                .map(this::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasResult(UUID examId) {
        return examPersistencePort.findById(examId)
                .filter(Exam::isCompleted)
                .isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public ExamResult getResult(UUID examId) {
        Exam exam = examPersistencePort.findById(examId).orElseThrow(ExamNotFoundException::new);
        exam.requireCompleted();
        return toResult(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInProgress(UUID examId) {
        Instant now = Instant.now();
        return examPersistencePort.findById(examId)
                .map(exam -> exam.isInProgress(now))
                .orElse(false);
    }

    private ExamResult toResult(Exam exam) {
        GradingResult gradingResult = exam.getGradingResult();
        return new ExamResult(exam.getId(), exam.getLevel().name(), exam.getLevel().groupName(),
                exam.getQuestionCount(), gradingResult.correctCount(), gradingResult.score(),
                gradingResult.retentionDeadline(examProperties.resultRetention()),
                examProperties.resultRetention().toMinutes(), toWrongItems(exam));
    }

    private List<ExamResult.WrongItem> toWrongItems(Exam exam) {
        return exam.wrongQuestionNos().stream()
                .map(no -> toWrongItem(exam, no))
                .toList();
    }

    private ExamResult.WrongItem toWrongItem(Exam exam, int no) {
        CatalogWord answer = wordCatalog.get(exam.answerLevelWordId(no));
        Long selectedId = exam.selectedLevelWordId(no);
        String selectedText = selectedId == null ? null : wordCatalog.get(selectedId).optionText();
        return new ExamResult.WrongItem(no, answer.headword(), answer.headwordSub(),
                selectedText, answer.optionText(), answer.headword());
    }

}

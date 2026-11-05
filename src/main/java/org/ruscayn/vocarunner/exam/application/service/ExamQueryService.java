package org.ruscayn.vocarunner.exam.application.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamResult;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.word.application.port.in.WordCatalogQuery;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ExamQueryService implements ExamQuery {

    private final ExamPersistencePort examPersistencePort;
    private final WordCatalogQuery wordCatalogQuery;
    private final ExamProperties examProperties;

    @Override
    @Transactional(readOnly = true)
    public ExamResult getResult(UUID examId) {
        Exam exam = examPersistencePort.findById(examId).orElseThrow(ExamNotFoundException::new);
        exam.requireCompleted();
        return toResult(exam);
    }

    private ExamResult toResult(Exam exam) {
        return ExamResult.of(exam, wordCatalogQuery.getWordCatalog(), examProperties.resultRetention());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ExamResult> findResult(UUID examId) {
        return examPersistencePort.findById(examId)
                .filter(Exam::isCompleted)
                .map(this::toResult);
    }

}

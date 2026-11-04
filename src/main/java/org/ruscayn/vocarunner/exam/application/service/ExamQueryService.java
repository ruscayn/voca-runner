package org.ruscayn.vocarunner.exam.application.service;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamResult;
import org.ruscayn.vocarunner.exam.application.port.in.LevelGroupSummary;
import org.ruscayn.vocarunner.exam.application.port.in.LevelSummary;
import org.ruscayn.vocarunner.exam.application.port.out.ExamPersistencePort;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
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
        if (!exam.isCompleted()) {
            throw new CustomException(ErrorCode.EXAM_NOT_COMPLETED);
        }
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

    @Override
    public List<LevelGroupSummary> getDetailLevelGroups() {
        Map<String, List<LevelSummary>> byGroup = getDetailLevels().stream()
                .collect(Collectors.groupingBy(summary -> summary.level().groupName(),
                        LinkedHashMap::new, Collectors.toList()));
        return byGroup.entrySet().stream()
                .map(entry -> new LevelGroupSummary(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<LevelSummary> getDetailLevels() {
        return Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(this::toSummary)
                .toList();
    }

    private LevelSummary toSummary(Level level) {
        int wordCount = wordCatalog.countByLevel(level);
        int questionCount = QuestionGenerator.questionCount(wordCount, examProperties.maxQuestionCount());
        long allowedMinutes = Exam.allowedTime(
                questionCount, Exam.DEFAULT_TIME_PER_QUESTION_SEC, examProperties.grace()).toMinutes();
        return new LevelSummary(level, wordCount, questionCount, allowedMinutes);
    }

    private ExamResult toResult(Exam exam) {
        return new ExamResult(exam.getId(), exam.getLevel().name(), exam.getLevel().groupName(),
                exam.getQuestionCount(), exam.getCorrectCount(), exam.getScore(),
                exam.getSubmittedAt().plus(examProperties.resultRetention()),
                examProperties.resultRetention().toMinutes(), toWrongItems(exam));
    }

    private List<ExamResult.WrongItem> toWrongItems(Exam exam) {
        return IntStream.rangeClosed(1, exam.getQuestionCount())
                .filter(no -> !exam.isCorrect(no))
                .mapToObj(no -> toWrongItem(exam, no))
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

package org.ruscayn.vocarunner.exam.application.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverview;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverviewGroup;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverviewQuery;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

@UseCase
@RequiredArgsConstructor
public class ExamLevelOverviewQueryService implements ExamLevelOverviewQuery {

    private final WordCatalog wordCatalog;
    private final ExamProperties examProperties;

    @Override
    public List<ExamLevelOverviewGroup> getExamLevelOverviews() {
        Map<String, List<ExamLevelOverview>> byGroup = getDetailLevelOverviews().stream()
                .collect(Collectors.groupingBy(overview -> overview.level().getGroupName(),
                        LinkedHashMap::new, Collectors.toList()));
        return byGroup.entrySet().stream()
                .map(entry -> new ExamLevelOverviewGroup(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<ExamLevelOverview> getDetailLevelOverviews() {
        return Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(this::toOverview)
                .toList();
    }

    private ExamLevelOverview toOverview(Level level) {
        int wordCount = wordCatalog.countByLevel(level);
        int questionCount = QuestionGenerator.questionCount(wordCount, examProperties.maxQuestionCount());
        long allowedMinutes = Exam.calculateAllowedTime(questionCount, examProperties.grace()).toMinutes();
        return new ExamLevelOverview(level, wordCount, questionCount, allowedMinutes);
    }

    @Override
    public ExamLevelOverview getExamLevelOverview(Level level) {
        if (level.isGroup()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "묶음 레벨 시험은 아직 볼 수 없습니다: " + level);
        }
        return toOverview(level);
    }

}

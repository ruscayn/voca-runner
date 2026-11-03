package org.ruscayn.vocarunner.exam.application.service;

import org.ruscayn.vocarunner.exam.application.port.in.LevelGroupSummary;
import org.ruscayn.vocarunner.exam.application.port.in.LevelSummary;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LevelApplicationService {

    private final WordCatalog wordCatalog;
    private final ExamProperties examProperties;

    /** 세부 레벨(A1~C2) 목록과 레벨별 단어 수·문항 수·허용 시간 (단어 출제 목록 사용) */
    public List<LevelSummary> getDetailLevels() {
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

    /** 세부 레벨을 CEFR 묶음별로 나눈 목록 (선언 순서 유지) */
    public List<LevelGroupSummary> getDetailLevelGroups() {
        Map<String, List<LevelSummary>> byGroup = getDetailLevels().stream()
                .collect(Collectors.groupingBy(summary -> summary.level().groupName(),
                        LinkedHashMap::new, Collectors.toList()));
        return byGroup.entrySet().stream()
                .map(entry -> new LevelGroupSummary(entry.getKey(), entry.getValue()))
                .toList();
    }

}

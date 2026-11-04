package org.ruscayn.vocarunner.word.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class WordExposureRules {

    public static final WordExposureRules NONE = new WordExposureRules(List.of());

    private final Set<Long> excludedWords = new HashSet<>();
    private final Set<LevelWordKey> excludedLevelWords = new HashSet<>();
    private final Set<MeaningKey> excludedMeanings = new HashSet<>();

    public WordExposureRules(Collection<Rule> rules) {
        rules.stream()
                .filter(rule -> rule.action() != ExposureAction.KEEP)
                .forEach(this::add);
    }

    private void add(Rule rule) {
        switch (rule.action()) {
            case EXCLUDE_WORD -> excludedWords.add(rule.wordId());
            case EXCLUDE_LEVEL -> excludedLevelWords.add(new LevelWordKey(rule.wordId(), rule.level()));
            case EXCLUDE_MEANING -> excludedMeanings.add(new MeaningKey(rule.wordId(), rule.meaningSeq()));
            default -> throw new IllegalStateException("처리하지 않은 노출 판정: " + rule.action());
        }
    }

    /** 보일 뜻이 하나도 남지 않으면 제외하고 원래 뜻을 그대로 둔다 (제외된 레벨 단어는 시험에 쓰지 않아 뜻이 보이지 않는다) */
    public Applied apply(long wordId, Level level, List<Meaning> optionMeanings) {
        List<String> visible = optionMeanings.stream()
                .filter(m -> !excludedMeanings.contains(new MeaningKey(wordId, m.seq())))
                .map(Meaning::text)
                .toList();
        boolean excluded = excludedWords.contains(wordId)
                || excludedLevelWords.contains(new LevelWordKey(wordId, level))
                || visible.isEmpty();
        return new Applied(visible.isEmpty() ? optionMeanings.stream().map(Meaning::text).toList() : visible, excluded);
    }

    public record Rule(long wordId, Integer meaningSeq, Level level, ExposureAction action) {

        public Rule {
            Objects.requireNonNull(action, "action");
            if (level != null && level.isGroup()) {
                throw new IllegalArgumentException("세부 레벨만 가능합니다: " + level);
            }
            if (!action.isAllowed(meaningSeq, level)) {
                throw new IllegalArgumentException("처리 %s 에 맞지 않는 대상: wordId=%d, meaningSeq=%s, level=%s"
                        .formatted(action, wordId, meaningSeq, level));
            }
        }

    }

    public record Meaning(int seq, String text) {

    }

    public record Applied(List<String> optionMeanings, boolean excluded) {

    }

    private record LevelWordKey(long wordId, Level level) {

    }

    private record MeaningKey(long wordId, int seq) {

    }

}

package org.ruscayn.vocarunner.word.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class WordExposureRules {

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
    public CatalogWord apply(LevelWord levelWord) {
        List<String> exposedMeanings = findExposedMeanings(levelWord);
        if (exposedMeanings.isEmpty()) {
            return levelWord.toCatalogWord(levelWord.optionMeaningTexts(), true);
        }
        return levelWord.toCatalogWord(exposedMeanings, isExcluded(levelWord));
    }

    private List<String> findExposedMeanings(LevelWord levelWord) {
        return levelWord.optionMeanings().stream()
                .filter(meaning -> !excludedMeanings.contains(new MeaningKey(levelWord.wordId(), meaning.seq())))
                .map(WordMeaning::text)
                .toList();
    }

    private boolean isExcluded(LevelWord levelWord) {
        return excludedWords.contains(levelWord.wordId())
                || excludedLevelWords.contains(new LevelWordKey(levelWord.wordId(), levelWord.level()));
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

    private record LevelWordKey(long wordId, Level level) {

    }

    private record MeaningKey(long wordId, int seq) {

    }

}

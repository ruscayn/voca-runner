package org.ruscayn.vocarunner.word.domain;

import java.util.List;

public record CatalogWord(long levelWordId, long wordId, String headword, String headwordSub,
                          Level level, List<String> optionMeanings, boolean excluded) {

    public CatalogWord {
        requireDetailLevel(level);
        requireHeadword(levelWordId, headword);
        requireOptionMeanings(levelWordId, optionMeanings);
        if (headwordSub != null && headwordSub.isBlank()) {
            headwordSub = null;
        }
        optionMeanings = List.copyOf(optionMeanings);
    }

    private static void requireDetailLevel(Level level) {
        if (level == null || level.isGroup()) {
            throw new IllegalArgumentException("세부 레벨만 가능합니다: " + level);
        }
    }

    private static void requireHeadword(long levelWordId, String headword) {
        if (headword == null || headword.isBlank()) {
            throw new IllegalArgumentException("표제어가 비어 있습니다: levelWordId=" + levelWordId);
        }
    }

    private static void requireOptionMeanings(long levelWordId, List<String> optionMeanings) {
        if (optionMeanings == null || optionMeanings.isEmpty()) {
            throw new IllegalArgumentException("보기 뜻이 없습니다: levelWordId=" + levelWordId);
        }
    }

    public String optionText() {
        return String.join(", ", optionMeanings);
    }

    public boolean isAnswerable() {
        return !excluded;
    }

}

package org.ruscayn.vocarunner.word.domain;

import java.util.List;

public record LevelWord(long levelWordId, long wordId, String headword, String headwordSub, Level level,
                        List<WordMeaning> optionMeanings) {

    public LevelWord {
        optionMeanings = List.copyOf(optionMeanings);
    }

    List<String> optionMeaningTexts() {
        return optionMeanings.stream().map(WordMeaning::text).toList();
    }

    CatalogWord toCatalogWord(List<String> optionMeaningTexts, boolean excluded) {
        return new CatalogWord(levelWordId, wordId, headword, headwordSub, level, optionMeaningTexts, excluded);
    }

}

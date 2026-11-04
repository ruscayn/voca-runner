package org.ruscayn.vocarunner.word.adapter.out.persistence;

import java.util.List;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;
import org.springframework.stereotype.Component;

@Component
class CatalogWordMapper {

    CatalogWord toCatalogWord(LevelWordJpaEntity levelWord, WordExposureRules rules) {
        long wordId = levelWord.getWord().getId();
        WordExposureRules.Applied applied = rules.apply(wordId, levelWord.getLevel(), toOptionMeanings(levelWord));
        return new CatalogWord(levelWord.getId(), wordId, levelWord.headword(),
                levelWord.getWord().getHeadwordSub(), levelWord.getLevel(),
                applied.optionMeanings(), applied.excluded());
    }

    private static List<WordExposureRules.Meaning> toOptionMeanings(LevelWordJpaEntity levelWord) {
        return levelWord.optionMeanings().stream()
                .map(meaning -> new WordExposureRules.Meaning(meaning.getSeq(), meaning.getMeaningKo()))
                .toList();
    }
}

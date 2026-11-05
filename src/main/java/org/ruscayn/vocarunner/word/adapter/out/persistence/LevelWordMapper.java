package org.ruscayn.vocarunner.word.adapter.out.persistence;

import java.util.List;
import org.ruscayn.vocarunner.word.domain.LevelWord;
import org.ruscayn.vocarunner.word.domain.WordMeaning;
import org.springframework.stereotype.Component;

@Component
class LevelWordMapper {

    LevelWord toLevelWord(LevelWordJpaEntity levelWord) {
        return new LevelWord(levelWord.getId(), levelWord.getWord().getId(), levelWord.getHeadword(),
                levelWord.getWord().getHeadwordSub(), levelWord.getLevel(), toOptionMeanings(levelWord));
    }

    private static List<WordMeaning> toOptionMeanings(LevelWordJpaEntity levelWord) {
        return levelWord.getOptionMeanings().stream()
                .map(meaning -> new WordMeaning(meaning.getSeq(), meaning.getMeaningKo()))
                .toList();
    }

}

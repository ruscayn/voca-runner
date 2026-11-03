package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;
import org.springframework.stereotype.Component;

/**
 * 여러 표에서 읽은 행(레벨 단어 · 단어 · 보기 뜻 + 단어 노출 규칙)을 모아 출제 목록 단어로 조립한다.
 * 한 행만으로 만들 수 없어 DB 저장용 클래스가 아니라 따로 둔다 (책의 AccountMapper 와 같은 자리).
 */
@Component
class CatalogWordMapper {

    CatalogWord toCatalogWord(LevelWordJpaEntity levelWord, WordExposureRules rules) {
        long wordId = levelWord.getWord().getId();
        WordExposureRules.Applied applied = rules.apply(wordId, levelWord.getLevel(),
                levelWord.optionMeanings().stream()
                        .map(meaning -> new WordExposureRules.Meaning(meaning.getSeq(), meaning.getMeaningKo()))
                        .toList());
        return new CatalogWord(
                levelWord.getId(),
                wordId,
                levelWord.headword(),
                levelWord.getWord().getHeadwordSub(),
                levelWord.getLevel(),
                applied.optionMeanings(),
                applied.excluded());
    }
}

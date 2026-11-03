package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 단어 출제 목록용 데이터를 DB 에서 읽는다. 서버 시작 시 1번만 호출.
 */
@Component
@RequiredArgsConstructor
public class CatalogWordLoader {

    private final JpaLevelWordRepository jpaLevelWordRepository;
    private final JpaConfusableGroupWordRepository jpaConfusableGroupWordRepository;
    private final JpaWordExposureRepository jpaWordExposureRepository;

    /** level_word 전체를 단어·보기 뜻과 함께 읽고, 단어 노출을 적용해 출제 목록 단어로 바꾼다 */
    @Transactional(readOnly = true)
    public List<CatalogWord> loadAll() {
        WordExposureRules rules = new WordExposureRules(
                jpaWordExposureRepository.findAll().stream().map(WordExposure::toRule).toList());
        return jpaLevelWordRepository.findAllFetch().stream()
                .map(lw -> toCatalogWord(lw, rules))
                .toList();
    }

    private static CatalogWord toCatalogWord(LevelWord lw, WordExposureRules rules) {
        long wordId = lw.getWord().getId();
        WordExposureRules.Applied applied = rules.apply(wordId, lw.getLevel(), lw.optionMeanings().stream()
                .map(m -> new WordExposureRules.Meaning(m.getSeq(), m.getMeaningKo()))
                .toList());
        return new CatalogWord(
                lw.getId(),
                wordId,
                lw.headword(),
                lw.getWord().getHeadwordSub(),
                lw.getLevel(),
                applied.optionMeanings(),
                applied.excluded());
    }

    /** 헷갈리는 단어 묶음 전체. 묶음 하나 = 그 묶음에 속한 word_id 목록 */
    @Transactional(readOnly = true)
    public Collection<List<Long>> loadConfusableGroups() {
        Map<Long, List<Long>> byGroup = new HashMap<>();
        for (ConfusableGroupWord.Id id : jpaConfusableGroupWordRepository.findAllIds()) {
            byGroup.computeIfAbsent(id.groupId(), k -> new ArrayList<>()).add(id.wordId());
        }
        return byGroup.values();
    }

}

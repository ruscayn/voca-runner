package org.ruscayn.vocarunner.infrastructure.persistence.vocabulary;

import org.ruscayn.vocarunner.domain.model.vocabulary.CatalogWord;
import org.ruscayn.vocarunner.domain.model.vocabulary.ConfusableGroupWord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 단어 캐시용 데이터를 DB 에서 읽는다. 서버 시작 시 1번만 호출.
 */
@Component
@RequiredArgsConstructor
public class CatalogWordLoader {

    private final JpaLevelWordRepository jpaLevelWordRepository;
    private final JpaConfusableGroupWordRepository jpaConfusableGroupWordRepository;

    /** level_word 전체를 단어·표시 뜻과 함께 읽어 값 객체로 변환 */
    @Transactional(readOnly = true)
    public List<CatalogWord> loadAll() {
        return jpaLevelWordRepository.findAllFetch().stream()
                .map(lw -> new CatalogWord(
                        lw.getId(),
                        lw.getWord().getId(),
                        lw.headword(),
                        lw.getWord().getHeadwordSub(),
                        lw.getLevel(),
                        lw.displayMeaningTexts(),
                        lw.isTestExcluded()))
                .toList();
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

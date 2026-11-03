package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.ruscayn.vocarunner.word.application.port.out.WordPersistencePort;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 단어 쪽 영속성 어댑터. 단어 출제 목록용 데이터를 DB 에서 읽는다. 서버 시작 시 1번만 호출.
 */
@Component
@RequiredArgsConstructor
public class WordPersistenceAdapter implements WordPersistencePort {

    private final SpringDataLevelWordRepository levelWordRepository;
    private final SpringDataConfusableGroupWordRepository confusableGroupWordRepository;
    private final SpringDataWordExposureRepository wordExposureRepository;
    private final CatalogWordMapper catalogWordMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CatalogWord> loadCatalogWords() {
        WordExposureRules rules = new WordExposureRules(
                wordExposureRepository.findAll().stream().map(WordExposureJpaEntity::toRule).toList());
        return levelWordRepository.findAllFetch().stream()
                .map(levelWord -> catalogWordMapper.toCatalogWord(levelWord, rules))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<List<Long>> loadConfusableGroups() {
        Map<Long, List<Long>> byGroup = new HashMap<>();
        for (ConfusableGroupWordJpaEntity.Id id : confusableGroupWordRepository.findAllIds()) {
            byGroup.computeIfAbsent(id.groupId(), k -> new ArrayList<>()).add(id.wordId());
        }
        return byGroup.values();
    }

}

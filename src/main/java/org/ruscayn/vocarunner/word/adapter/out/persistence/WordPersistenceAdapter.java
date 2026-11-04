package org.ruscayn.vocarunner.word.adapter.out.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.PersistenceAdapter;
import org.ruscayn.vocarunner.word.application.port.out.WordPersistencePort;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;
import org.springframework.transaction.annotation.Transactional;

@PersistenceAdapter
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

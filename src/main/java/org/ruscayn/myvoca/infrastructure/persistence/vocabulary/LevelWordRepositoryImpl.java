package org.ruscayn.myvoca.infrastructure.persistence.vocabulary;

import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;
import org.ruscayn.myvoca.domain.repository.LevelWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LevelWordRepositoryImpl implements LevelWordRepository {

    private final JpaLevelWordRepository jpaLevelWordRepository;

    @Override
    public LevelWord getReference(long levelWordId) {
        return jpaLevelWordRepository.getReferenceById(levelWordId);
    }

}

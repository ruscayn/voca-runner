package org.ruscayn.myvoca.infrastructure.persistence.vocabulary;

import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;
import org.ruscayn.myvoca.domain.repository.LevelWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LevelWordRepositoryImpl implements LevelWordRepository {

    private final JpaLevelWordRepository jpaLevelWordRepository;

    @Override
    public List<LevelWord> findAllByLevels(Collection<Level> levels) {
        return jpaLevelWordRepository.findAllByLevelsFetch(levels);
    }

    @Override
    public long countByLevel(Level level) {
        return jpaLevelWordRepository.countByLevel(level);
    }

}

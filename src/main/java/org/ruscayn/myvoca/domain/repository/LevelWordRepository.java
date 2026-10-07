package org.ruscayn.myvoca.domain.repository;

import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;

import java.util.Collection;
import java.util.List;

public interface LevelWordRepository {

    /** 세부 레벨들의 level_word 전체 (단어, 표시 뜻 함께 조회) */
    List<LevelWord> findAllByLevels(Collection<Level> levels);

    long countByLevel(Level level);

}

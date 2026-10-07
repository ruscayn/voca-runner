package org.ruscayn.myvoca.infrastructure.persistence.vocabulary;

import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface JpaLevelWordRepository extends JpaRepository<LevelWord, Long> {

    @Query("""
            select lw from LevelWord lw
              join fetch lw.word
              join fetch lw.meaning1
              left join fetch lw.meaning2
             where lw.level in :levels
            """)
    List<LevelWord> findAllByLevelsFetch(@Param("levels") Collection<Level> levels);

    long countByLevel(Level level);

}

package org.ruscayn.vocarunner.infrastructure.persistence.vocabulary;

import org.ruscayn.vocarunner.domain.model.vocabulary.LevelWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaLevelWordRepository extends JpaRepository<LevelWord, Long> {

    /** 단어 캐시 로딩용: level_word 전체 (단어, 표시 뜻 함께 조회) */
    @Query("""
            select lw from LevelWord lw
              join fetch lw.word
              join fetch lw.meaning1
              left join fetch lw.meaning2
            """)
    List<LevelWord> findAllFetch();

}

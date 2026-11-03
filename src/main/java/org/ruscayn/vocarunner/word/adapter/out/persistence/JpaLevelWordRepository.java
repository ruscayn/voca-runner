package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaLevelWordRepository extends JpaRepository<LevelWord, Long> {

    /** 단어 출제 목록 로딩용: level_word 전체 (단어, 보기 뜻 함께 조회) */
    @Query("""
            select lw from LevelWord lw
              join fetch lw.word
              join fetch lw.meaning1
              left join fetch lw.meaning2
            """)
    List<LevelWord> findAllFetch();

}

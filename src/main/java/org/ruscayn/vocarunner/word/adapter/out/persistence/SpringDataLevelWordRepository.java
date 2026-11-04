package org.ruscayn.vocarunner.word.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataLevelWordRepository extends JpaRepository<LevelWordJpaEntity, Long> {

    /** 단어 출제 목록 로딩용: level_word 전체 (단어, 보기 뜻 함께 조회) */
    @Query("""
            select lw from LevelWordJpaEntity lw
              join fetch lw.word
              join fetch lw.meaning1
              left join fetch lw.meaning2
            """)
    List<LevelWordJpaEntity> findAllFetch();

}

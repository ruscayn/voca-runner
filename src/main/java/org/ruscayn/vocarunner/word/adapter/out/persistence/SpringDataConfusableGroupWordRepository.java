package org.ruscayn.vocarunner.word.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataConfusableGroupWordRepository
        extends JpaRepository<ConfusableGroupWordJpaEntity, ConfusableGroupWordJpaEntity.Id> {

    /** 단어 출제 목록 로딩용: 묶음 아이디·단어 아이디만 (Entity 연관 로딩 없이) */
    @Query("select gw.id from ConfusableGroupWordJpaEntity gw")
    List<ConfusableGroupWordJpaEntity.Id> findAllIds();

}

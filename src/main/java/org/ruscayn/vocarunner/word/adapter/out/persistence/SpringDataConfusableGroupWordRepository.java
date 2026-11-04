package org.ruscayn.vocarunner.word.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataConfusableGroupWordRepository
        extends JpaRepository<ConfusableGroupWordJpaEntity, ConfusableGroupWordJpaEntity.Id> {

    @Query("select gw.id from ConfusableGroupWordJpaEntity gw")
    List<ConfusableGroupWordJpaEntity.Id> findAllIds();

}

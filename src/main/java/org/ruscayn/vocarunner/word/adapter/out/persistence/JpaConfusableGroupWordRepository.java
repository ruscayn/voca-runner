package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaConfusableGroupWordRepository extends JpaRepository<ConfusableGroupWord, ConfusableGroupWord.Id> {

    /** 단어 캐시 로딩용: 묶음 번호·단어 번호만 (Entity 연관 로딩 없이) */
    @Query("select gw.id from ConfusableGroupWord gw")
    List<ConfusableGroupWord.Id> findAllIds();

}

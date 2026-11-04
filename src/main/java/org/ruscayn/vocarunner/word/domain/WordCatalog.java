package org.ruscayn.vocarunner.word.domain;

import java.util.List;
import java.util.Set;

public interface WordCatalog {

    List<CatalogWord> findAnswerPool(Level level);

    List<CatalogWord> findOptionPool(Level level);

    /** 시험 제외 행도 찾는다 */
    CatalogWord get(long levelWordId);

    int countByLevel(Level level);

    Set<Long> findConfusableWordIds(long wordId);

}

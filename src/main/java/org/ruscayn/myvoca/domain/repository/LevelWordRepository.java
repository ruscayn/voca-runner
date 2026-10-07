package org.ruscayn.myvoca.domain.repository;

import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;

/**
 * level_word Entity 저장소.
 * 단어 조회는 WordCatalog(메모리)를 사용하고, 여기서는 test_question 저장용 참조만 제공한다.
 */
public interface LevelWordRepository {

    /** DB 조회 없이 id 만 가진 참조(프록시)를 반환. test_question FK 저장용 */
    LevelWord getReference(long levelWordId);

}

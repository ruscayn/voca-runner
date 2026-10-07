package org.ruscayn.myvoca.domain.model.vocabulary;

import org.ruscayn.myvoca.domain.model.level.Level;

import java.util.List;

/**
 * 출제 단어 목록 (단어 캐시).
 *
 * <p>구현체는 서버 시작 시 전체 단어를 한 번 읽어 메모리에 보관한다.
 * 문제 생성·결과 표시에서는 DB 를 조회하지 않고 이 목록만 사용한다.
 */
public interface WordCatalog {

    /**
     * 레벨의 출제 단어 목록 (불변).
     * 묶음 레벨(A/B/C)이면 소속 세부 레벨 단어를 합쳐서 반환한다. (같은 단어 중복 정리는 하지 않음)
     */
    List<CatalogWord> findByLevel(Level level);

    /** level_word_id 로 조회. 없으면 예외 */
    CatalogWord get(long levelWordId);

    /** 레벨의 출제 단어 수 */
    int countByLevel(Level level);

}

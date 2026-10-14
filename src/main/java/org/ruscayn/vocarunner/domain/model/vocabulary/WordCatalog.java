package org.ruscayn.vocarunner.domain.model.vocabulary;

import org.ruscayn.vocarunner.domain.model.level.Level;

import java.util.List;
import java.util.Set;

/**
 * 출제 단어 목록 (단어 캐시).
 *
 * <p>구현체는 서버 시작 시 전체 단어를 한 번 읽어 메모리에 보관한다.
 * 문제 생성·결과 표시에서는 DB 를 조회하지 않고 이 목록만 사용한다.
 *
 * <p>묶음 레벨(A/B/C)은 소속 세부 레벨 단어를 합치고, 두 레벨에 모두 있는 단어는 낮은 레벨 1행만 남긴다.
 */
public interface WordCatalog {

    /**
     * 정답 문제 후보 (불변). 시험 제외 행을 먼저 빼고, 그다음 단어당 1행(낮은 레벨)만 남긴다.
     * <p>순서가 반대면 "낮은 레벨은 제외, 높은 레벨은 출제"인 단어가 아예 출제되지 않으므로 이 순서를 지킨다.
     */
    List<CatalogWord> findAnswerPool(Level level);

    /** 오답 보기 후보 (불변). 시험 제외 행 포함, 단어당 1행(낮은 레벨) */
    List<CatalogWord> findOptionPool(Level level);

    /** level_word_id 로 조회 (시험 제외 행 포함). 없으면 예외 */
    CatalogWord get(long levelWordId);

    /** 레벨의 출제 가능 단어 수 (= 정답 문제 후보 수). 화면 표시용 */
    int countByLevel(Level level);

    /**
     * 이 단어와 헷갈리는 단어의 word_id (불변). 같은 묶음의 다른 단어 전체이며, 없으면 빈 집합.
     * <p>양방향이다: A 의 결과에 B 가 있으면 B 의 결과에도 A 가 있다.
     * 한 문제의 보기에는 서로 헷갈리는 단어를 함께 내지 않는다.
     */
    Set<Long> findConfusableWordIds(long wordId);

}

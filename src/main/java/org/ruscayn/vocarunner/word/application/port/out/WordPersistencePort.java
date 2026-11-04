package org.ruscayn.vocarunner.word.application.port.out;

import java.util.Collection;
import java.util.List;
import org.ruscayn.vocarunner.word.domain.CatalogWord;

/**
 * 단어 쪽 DB로 나가는 포트. 앱 시작 때 단어 출제 목록을 만들려고 한 번 읽는다.
 */
public interface WordPersistencePort {

    /** 레벨 단어 전체를 보기 뜻과 함께 읽고, 단어 노출을 적용해 출제 목록 단어로 돌려준다 */
    List<CatalogWord> loadCatalogWords();

    /** 헷갈리는 묶음 전체. 묶음 하나 = 그 묶음에 속한 단어 아이디 목록 */
    Collection<List<Long>> loadConfusableGroups();
}

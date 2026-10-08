package org.ruscayn.vocarunner.infrastructure.cache;

import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.vocabulary.CatalogWord;
import org.ruscayn.vocarunner.domain.model.vocabulary.WordCatalog;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;

import java.util.*;

/**
 * 메모리 단어 캐시.
 *
 * <p>생성 시 받은 단어로 레벨별 목록과 id 맵을 만들어 두고 이후 변경하지 않는다.
 * 모두 불변이라 여러 스레드가 동시에 읽어도 안전하다.
 * DB 와 무관하므로 테스트에서는 단어 목록을 직접 넣어 생성하면 된다.
 */
public class InMemoryWordCatalog implements WordCatalog {

    /** 세부·묶음 레벨 모두 미리 계산 */
    private final Map<Level, List<CatalogWord>> byLevel;
    private final Map<Long, CatalogWord> byId;

    public InMemoryWordCatalog(Collection<CatalogWord> words) {
        Map<Long, CatalogWord> ids = new HashMap<>(words.size() * 2);
        Map<Level, List<CatalogWord>> detail = new EnumMap<>(Level.class);
        for (CatalogWord word : words) {
            if (ids.putIfAbsent(word.levelWordId(), word) != null) {
                throw new IllegalArgumentException("level_word_id 중복: " + word.levelWordId());
            }
            detail.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
        }

        Map<Level, List<CatalogWord>> levels = new EnumMap<>(Level.class);
        for (Level level : Level.values()) {
            List<CatalogWord> list = new ArrayList<>();
            for (Level detailLevel : level.detailLevels()) {
                list.addAll(detail.getOrDefault(detailLevel, List.of()));
            }
            levels.put(level, List.copyOf(list));
        }

        this.byLevel = Collections.unmodifiableMap(levels);
        this.byId = Map.copyOf(ids);
    }

    @Override
    public List<CatalogWord> findByLevel(Level level) {
        return byLevel.get(level);
    }

    @Override
    public CatalogWord get(long levelWordId) {
        CatalogWord word = byId.get(levelWordId);
        if (word == null) {
            throw new CustomException(ErrorCode.ENTITY_NOT_FOUND, "단어를 찾을 수 없습니다: " + levelWordId);
        }
        return word;
    }

    @Override
    public int countByLevel(Level level) {
        return byLevel.get(level).size();
    }

    /** 전체 단어 수 (로그용) */
    public int size() {
        return byId.size();
    }

}

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
 * <p>생성 시 받은 단어로 레벨별 후보 목록과 id 맵을 만들어 두고 이후 변경하지 않는다.
 * 모두 불변이라 여러 스레드가 동시에 읽어도 안전하다.
 * DB 와 무관하므로 테스트에서는 단어 목록을 직접 넣어 생성하면 된다.
 */
public class InMemoryWordCatalog implements WordCatalog {

    /** 세부·묶음 레벨 모두 미리 계산 */
    private final Map<Level, List<CatalogWord>> answerPool;
    private final Map<Level, List<CatalogWord>> optionPool;
    private final Map<Long, CatalogWord> byId;
    /** word_id → 헷갈리는 단어 word_id (양방향). 묶음 DB 의 한 행을 양쪽 방향으로 펼친 것 */
    private final Map<Long, Set<Long>> confusableByWordId;
    private final int excludedCount;
    private final int confusableGroupCount;

    /** 헷갈리는 단어 묶음 없이 생성 (테스트용) */
    public InMemoryWordCatalog(Collection<CatalogWord> words) {
        this(words, List.of());
    }

    /**
     * @param words            level_word 전체
     * @param confusableGroups 헷갈리는 단어 묶음. 묶음 하나 = word_id 목록 (같은 묶음 단어는 보기로 함께 내지 않음)
     */
    public InMemoryWordCatalog(Collection<CatalogWord> words, Collection<? extends Collection<Long>> confusableGroups) {
        Map<Long, CatalogWord> ids = new HashMap<>(words.size() * 2);
        Map<Level, List<CatalogWord>> detail = new EnumMap<>(Level.class);
        int excluded = 0;
        for (CatalogWord word : words) {
            if (ids.putIfAbsent(word.levelWordId(), word) != null) {
                throw new IllegalArgumentException("level_word_id 중복: " + word.levelWordId());
            }
            detail.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
            if (word.testExcluded()) {
                excluded++;
            }
        }

        Map<Level, List<CatalogWord>> answers = new EnumMap<>(Level.class);
        Map<Level, List<CatalogWord>> options = new EnumMap<>(Level.class);
        for (Level level : Level.values()) {
            List<CatalogWord> all = new ArrayList<>();
            for (Level detailLevel : level.detailLevels()) {
                all.addAll(detail.getOrDefault(detailLevel, List.of()));
            }
            // 정답 후보 : 시험 제외를 먼저 빼고 중복 정리 (순서 중요)
            answers.put(level, keepLowestLevelPerWord(all.stream().filter(CatalogWord::isAnswerable).toList()));
            options.put(level, keepLowestLevelPerWord(all));
        }

        this.answerPool = Collections.unmodifiableMap(answers);
        this.optionPool = Collections.unmodifiableMap(options);
        this.byId = Map.copyOf(ids);
        this.confusableByWordId = toConfusableMap(confusableGroups);
        this.excludedCount = excluded;
        this.confusableGroupCount = confusableGroups.size();
    }

    /** 단어당 1행만 남긴다. 여러 레벨에 있으면 낮은 레벨. 처음 나온 순서 유지 */
    private static List<CatalogWord> keepLowestLevelPerWord(List<CatalogWord> candidates) {
        Map<Long, CatalogWord> byWord = new LinkedHashMap<>();
        for (CatalogWord word : candidates) {
            byWord.merge(word.wordId(), word,
                    (current, next) -> next.level().isLowerThan(current.level()) ? next : current);
        }
        return List.copyOf(byWord.values());
    }

    /**
     * 묶음을 단어별 map 으로 펼친다. 묶음 {a, b, c} → a:{b,c}, b:{a,c}, c:{a,b}.
     * 한 단어가 여러 묶음에 있으면 합친다 (sir:{teacher} + sir:{Mr.} → sir:{teacher, Mr.})
     */
    private static Map<Long, Set<Long>> toConfusableMap(Collection<? extends Collection<Long>> groups) {
        Map<Long, Set<Long>> map = new HashMap<>();
        for (Collection<Long> group : groups) {
            for (Long wordId : group) {
                for (Long other : group) {
                    if (!wordId.equals(other)) {
                        map.computeIfAbsent(wordId, k -> new HashSet<>()).add(other);
                    }
                }
            }
        }
        Map<Long, Set<Long>> result = new HashMap<>(map.size() * 2);
        map.forEach((wordId, others) -> result.put(wordId, Set.copyOf(others)));
        return Map.copyOf(result);
    }

    @Override
    public List<CatalogWord> findAnswerPool(Level level) {
        return answerPool.get(level);
    }

    @Override
    public List<CatalogWord> findOptionPool(Level level) {
        return optionPool.get(level);
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
        return answerPool.get(level).size();
    }

    @Override
    public Set<Long> findConfusableWordIds(long wordId) {
        return confusableByWordId.getOrDefault(wordId, Set.of());
    }

    /** 전체 level_word 행 수 (로그용) */
    public int size() {
        return byId.size();
    }

    /** 시험 제외 행 수 (로그용) */
    public int excludedCount() {
        return excludedCount;
    }

    /** 헷갈리는 단어 묶음 수 (로그용) */
    public int confusableGroupCount() {
        return confusableGroupCount;
    }

    /** 헷갈리는 짝이 있는 단어 수 (로그용) */
    public int confusableWordCount() {
        return confusableByWordId.size();
    }

}

package org.ruscayn.vocarunner.word.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;

public class InMemoryWordCatalog implements WordCatalog {

    private final Map<Level, List<CatalogWord>> answerPool;
    private final Map<Level, List<CatalogWord>> optionPool;
    private final Map<Long, CatalogWord> byId;
    private final Map<Long, Set<Long>> confusableByWordId;
    private final int excludedCount;
    private final int confusableGroupCount;

    public InMemoryWordCatalog(Collection<CatalogWord> words) {
        this(words, List.of());
    }

    public InMemoryWordCatalog(Collection<CatalogWord> words, Collection<? extends Collection<Long>> confusableGroups) {
        this.byId = indexByLevelWordId(words);
        this.answerPool = toAnswerPool(groupByDetailLevel(words));
        this.optionPool = this.answerPool;
        this.confusableByWordId = toConfusableMap(confusableGroups);
        this.excludedCount = (int) words.stream().filter(CatalogWord::excluded).count();
        this.confusableGroupCount = confusableGroups.size();
    }

    private static Map<Long, CatalogWord> indexByLevelWordId(Collection<CatalogWord> words) {
        Map<Long, CatalogWord> ids = new HashMap<>(words.size() * 2);
        for (CatalogWord word : words) {
            putUnique(ids, word);
        }
        return Map.copyOf(ids);
    }

    private static void putUnique(Map<Long, CatalogWord> ids, CatalogWord word) {
        if (ids.putIfAbsent(word.levelWordId(), word) != null) {
            throw new IllegalArgumentException("level_word_id 중복: " + word.levelWordId());
        }
    }

    private static Map<Level, List<CatalogWord>> groupByDetailLevel(Collection<CatalogWord> words) {
        Map<Level, List<CatalogWord>> byDetailLevel = new EnumMap<>(Level.class);
        for (CatalogWord word : words) {
            byDetailLevel.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
        }
        return byDetailLevel;
    }

    /**
     * 시험 제외를 먼저 빼고 단어당 한 행(낮은 레벨)만 남긴다. 순서가 반대면 '낮은 레벨은 제외, 높은 레벨은 출제'인
     * 단어가 아예 나오지 않는다. 오답 보기 후보도 이 목록을 쓴다 : 시험 제외 단어가 오답이면 너무 쉽거나,
     * supermarket · team 처럼 정답과 헷갈린다
     */
    private static Map<Level, List<CatalogWord>> toAnswerPool(Map<Level, List<CatalogWord>> byDetailLevel) {
        Map<Level, List<CatalogWord>> pool = new EnumMap<>(Level.class);
        for (Level level : Level.values()) {
            List<CatalogWord> answerable = wordsIn(level, byDetailLevel).stream()
                    .filter(CatalogWord::isAnswerable)
                    .toList();
            pool.put(level, lowestLevelPerWord(answerable));
        }
        return Collections.unmodifiableMap(pool);
    }

    private static List<CatalogWord> wordsIn(Level level, Map<Level, List<CatalogWord>> byDetailLevel) {
        List<CatalogWord> words = new ArrayList<>();
        for (Level detailLevel : level.detailLevels()) {
            words.addAll(byDetailLevel.getOrDefault(detailLevel, List.of()));
        }
        return words;
    }

    private static List<CatalogWord> lowestLevelPerWord(List<CatalogWord> candidates) {
        Map<Long, CatalogWord> byWord = new LinkedHashMap<>();
        for (CatalogWord word : candidates) {
            byWord.merge(word.wordId(), word,
                    (current, next) -> next.level().isLowerThan(current.level()) ? next : current);
        }
        return List.copyOf(byWord.values());
    }

    private static Map<Long, Set<Long>> toConfusableMap(Collection<? extends Collection<Long>> groups) {
        Map<Long, Set<Long>> map = new HashMap<>();
        for (Collection<Long> group : groups) {
            addConfusableWordIds(map, group);
        }
        Map<Long, Set<Long>> result = new HashMap<>(map.size() * 2);
        map.forEach((wordId, others) -> result.put(wordId, Set.copyOf(others)));
        return Map.copyOf(result);
    }

    private static void addConfusableWordIds(Map<Long, Set<Long>> map, Collection<Long> group) {
        for (Long wordId : group) {
            group.stream()
                    .filter(other -> !other.equals(wordId))
                    .forEach(other -> map.computeIfAbsent(wordId, k -> new HashSet<>()).add(other));
        }
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

    public int size() {
        return byId.size();
    }

    public int excludedCount() {
        return excludedCount;
    }

    public int confusableGroupCount() {
        return confusableGroupCount;
    }

    public int confusableWordCount() {
        return confusableByWordId.size();
    }

}

package org.ruscayn.vocarunner.exam.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

/**
 * 보기 규칙
 * <ul>
 *   <li>보기 4개는 정답과 같은 세부 레벨에서 고른다</li>
 *   <li>보기끼리 보기 뜻이 하나라도 같으면 안 된다</li>
 *   <li>헷갈리는 단어 묶음에 함께 속한 단어는 같은 문항의 보기로 내지 않는다 (예: sir 문항에 teacher. 오답끼리도)</li>
 * </ul>
 */
class OptionPicker {

    private static final int INCORRECT_OPTION_COUNT = Question.OPTION_COUNT - 1;
    private static final int RANDOM_TRY_LIMIT = 200;

    private final WordCatalog wordCatalog;
    private final Map<Level, List<CatalogWord>> optionPoolByLevel;
    private final RandomGenerator random;

    OptionPicker(WordCatalog wordCatalog, List<CatalogWord> optionPool, RandomGenerator random) {
        this.wordCatalog = wordCatalog;
        this.optionPoolByLevel = groupByDetailLevel(optionPool);
        this.random = random;
    }

    private static Map<Level, List<CatalogWord>> groupByDetailLevel(List<CatalogWord> optionPool) {
        Map<Level, List<CatalogWord>> byLevel = new EnumMap<>(Level.class);
        for (CatalogWord word : optionPool) {
            byLevel.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
        }
        return byLevel;
    }

    List<CatalogWord> pick(CatalogWord answer) {
        List<CatalogWord> sameLevel = optionPoolByLevel.getOrDefault(answer.level(), List.of());
        List<CatalogWord> options = new ArrayList<>(Question.OPTION_COUNT);
        options.add(answer);
        addRandomly(sameLevel, options);
        addInShuffledOrder(sameLevel, options);
        requireFull(options, answer);
        Collections.shuffle(options, random);
        return options;
    }

    private void addRandomly(List<CatalogWord> sameLevel, List<CatalogWord> options) {
        for (int i = 0; !sameLevel.isEmpty() && i < RANDOM_TRY_LIMIT && !isFull(options); i++) {
            tryAdd(sameLevel.get(random.nextInt(sameLevel.size())), options);
        }
    }

    private void addInShuffledOrder(List<CatalogWord> sameLevel, List<CatalogWord> options) {
        if (isFull(options)) {
            return;
        }
        List<CatalogWord> shuffled = new ArrayList<>(sameLevel);
        Collections.shuffle(shuffled, random);
        for (int i = 0; i < shuffled.size() && !isFull(options); i++) {
            tryAdd(shuffled.get(i), options);
        }
    }

    private static void requireFull(List<CatalogWord> options, CatalogWord answer) {
        if (!isFull(options)) {
            throw new CustomException(ErrorCode.EXAM_NOT_ENOUGH_WORDS,
                    "'" + answer.headword() + "' 의 오답 보기 " + INCORRECT_OPTION_COUNT + "개를 만들 수 없습니다.");
        }
    }

    private static boolean isFull(List<CatalogWord> options) {
        return options.size() >= Question.OPTION_COUNT;
    }

    private void tryAdd(CatalogWord candidate, List<CatalogWord> options) {
        if (options.stream().noneMatch(chosen -> conflicts(chosen, candidate))) {
            options.add(candidate);
        }
    }

    private boolean conflicts(CatalogWord chosen, CatalogWord candidate) {
        return chosen.wordId() == candidate.wordId()
                || wordCatalog.findConfusableWordIds(chosen.wordId()).contains(candidate.wordId())
                || !Collections.disjoint(chosen.optionMeanings(), candidate.optionMeanings());
    }

}

package org.ruscayn.vocarunner.exam.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

/**
 * 출제 규칙
 * <ul>
 *   <li>한 시험에서 같은 단어는 한 번만</li>
 *   <li>묶음 레벨은 소속 세부 레벨 단어를 함께 쓰고, 두 레벨에 모두 있는 단어는 낮은 레벨(뜻)을 쓴다</li>
 *   <li>시험 제외 단어는 정답으로도 오답 보기로도 내지 않는다</li>
 *   <li>보기 4개는 정답과 같은 세부 레벨에서 고른다</li>
 *   <li>보기끼리 보기 뜻이 하나라도 같으면 안 된다</li>
 *   <li>헷갈리는 단어 묶음에 함께 속한 단어는 같은 문항의 보기로 내지 않는다 (예: sir 문항에 teacher. 오답끼리도)</li>
 * </ul>
 */
public class QuestionGenerator {

    public static final int ALL_WORDS = 0;

    private static final int INCORRECT_OPTION_COUNT = Question.OPTION_COUNT - 1;
    private static final int RANDOM_TRY_LIMIT = 200;

    /** 여러 요청(스레드)이 함께 쓰므로 부를 때마다 그 스레드의 난수를 쓴다 */
    private final Supplier<RandomGenerator> randomSupplier;

    public QuestionGenerator() {
        this(ThreadLocalRandom::current);
    }

    public QuestionGenerator(Supplier<RandomGenerator> randomSupplier) {
        this.randomSupplier = randomSupplier;
    }

    public List<Question> generate(WordCatalog wordCatalog, Level level, int maxCount) {
        List<CatalogWord> answerPool = wordCatalog.findAnswerPool(level);
        int count = requireQuestionCount(level, answerPool.size(), maxCount);
        Map<Level, List<CatalogWord>> optionPoolByLevel = groupByDetailLevel(wordCatalog.findOptionPool(level));
        RandomGenerator random = randomSupplier.get();
        List<CatalogWord> answers = new ArrayList<>(answerPool);
        shuffle(answers, random);
        return toQuestions(wordCatalog, answers.subList(0, count), optionPoolByLevel, random);
    }

    private static int requireQuestionCount(Level level, int answerPoolSize, int maxCount) {
        int count = questionCount(answerPoolSize, maxCount);
        if (count == 0) {
            throw new CustomException(ErrorCode.EXAM_NOT_ENOUGH_WORDS, "레벨 " + level + " 에 출제 가능한 단어가 없습니다.");
        }
        return count;
    }

    private static Map<Level, List<CatalogWord>> groupByDetailLevel(List<CatalogWord> optionPool) {
        Map<Level, List<CatalogWord>> byLevel = new EnumMap<>(Level.class);
        for (CatalogWord word : optionPool) {
            byLevel.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
        }
        return byLevel;
    }

    private List<Question> toQuestions(WordCatalog wordCatalog, List<CatalogWord> answers,
                                       Map<Level, List<CatalogWord>> optionPoolByLevel, RandomGenerator random) {
        List<Question> questions = new ArrayList<>(answers.size());
        for (CatalogWord answer : answers) {
            List<CatalogWord> sameLevel = optionPoolByLevel.getOrDefault(answer.level(), List.of());
            questions.add(new Question(answer, pickOptions(wordCatalog, answer, sameLevel, random)));
        }
        return questions;
    }

    public static int questionCount(int answerPoolSize, int maxCount) {
        if (maxCount == ALL_WORDS) {
            return answerPoolSize;
        }
        return Math.min(answerPoolSize, maxCount);
    }

    private List<CatalogWord> pickOptions(WordCatalog wordCatalog, CatalogWord answer, List<CatalogWord> sameLevel,
                                          RandomGenerator random) {
        List<CatalogWord> options = new ArrayList<>(Question.OPTION_COUNT);
        options.add(answer);
        addRandomly(wordCatalog, sameLevel, options, random);
        addInShuffledOrder(wordCatalog, sameLevel, options, random);
        requireFull(options, answer);
        shuffle(options, random);
        return options;
    }

    private void addRandomly(WordCatalog wordCatalog, List<CatalogWord> sameLevel, List<CatalogWord> options,
                             RandomGenerator random) {
        for (int i = 0; !sameLevel.isEmpty() && i < RANDOM_TRY_LIMIT && !isFull(options); i++) {
            tryAdd(wordCatalog, sameLevel.get(random.nextInt(sameLevel.size())), options);
        }
    }

    private void addInShuffledOrder(WordCatalog wordCatalog, List<CatalogWord> sameLevel, List<CatalogWord> options,
                                    RandomGenerator random) {
        if (isFull(options)) {
            return;
        }
        List<CatalogWord> shuffled = new ArrayList<>(sameLevel);
        shuffle(shuffled, random);
        for (int i = 0; i < shuffled.size() && !isFull(options); i++) {
            tryAdd(wordCatalog, shuffled.get(i), options);
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

    private void tryAdd(WordCatalog wordCatalog, CatalogWord candidate, List<CatalogWord> options) {
        if (options.stream().noneMatch(chosen -> conflicts(wordCatalog, chosen, candidate))) {
            options.add(candidate);
        }
    }

    private static boolean conflicts(WordCatalog wordCatalog, CatalogWord chosen, CatalogWord candidate) {
        return chosen.wordId() == candidate.wordId()
                || wordCatalog.findConfusableWordIds(chosen.wordId()).contains(candidate.wordId())
                || !Collections.disjoint(chosen.optionMeanings(), candidate.optionMeanings());
    }

    private <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }

}

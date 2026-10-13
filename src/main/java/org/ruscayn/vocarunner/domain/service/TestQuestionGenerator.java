package org.ruscayn.vocarunner.domain.service;

import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.test.GeneratedQuestion;
import org.ruscayn.vocarunner.domain.model.vocabulary.CatalogWord;
import org.ruscayn.vocarunner.domain.model.vocabulary.WordCatalog;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * 시험 문제 생성 도메인 서비스. 단어는 WordCatalog(메모리)에서만 가져온다.
 *
 * <p>규칙
 * <ul>
 *   <li>한 세션에서 같은 단어는 한 번만 출제</li>
 *   <li>묶음 레벨(A 등)은 소속 세부 레벨 단어를 함께 사용. 두 레벨에 모두 있는 단어는 낮은 레벨(뜻) 사용</li>
 *   <li>시험 제외 단어는 정답 문제로 내지 않고, 오답 보기로만 사용</li>
 *   <li>보기 4개는 정답과 같은 세부 레벨 단어에서 선택</li>
 *   <li>보기 간 표시 뜻(한국어)이 하나라도 겹치면 안 됨</li>
 * </ul>
 */
@Service
public class TestQuestionGenerator {

    private static final int DISTRACTOR_COUNT = GeneratedQuestion.OPTION_COUNT - 1;
    /** 무작위 뽑기 시도 횟수. 초과하면 전체를 섞어 순서대로 검사 */
    private static final int RANDOM_TRY_LIMIT = 200;

    private final WordCatalog wordCatalog;
    /** 싱글톤 빈에서 여러 스레드가 쓰므로 호출마다 스레드 전용 난수를 사용 */
    private final Supplier<RandomGenerator> randomSupplier;

    @Autowired
    public TestQuestionGenerator(WordCatalog wordCatalog) {
        this(wordCatalog, ThreadLocalRandom::current);
    }

    /** 테스트에서 시드 고정용 */
    public TestQuestionGenerator(WordCatalog wordCatalog, Supplier<RandomGenerator> randomSupplier) {
        this.wordCatalog = wordCatalog;
        this.randomSupplier = randomSupplier;
    }

    /**
     * 문제를 생성한다. 출제 가능 단어가 maxCount 보다 적으면 그 레벨 단어 전체를 출제한다.
     *
     * @param level    사용자가 고른 레벨 (세부 또는 묶음)
     * @param maxCount 최대 문제 수 (설정값)
     */
    public List<GeneratedQuestion> generate(Level level, int maxCount) {
        List<CatalogWord> answerPool = wordCatalog.findAnswerPool(level);
        int count = questionCount(answerPool.size(), maxCount);
        if (count == 0) {
            throw new CustomException(ErrorCode.TEST_NOT_ENOUGH_WORDS, "레벨 " + level + " 에 출제 가능한 단어가 없습니다.");
        }

        // 오답 보기 후보 (시험 제외 단어 포함), 세부 레벨별
        Map<Level, List<CatalogWord>> optionPoolByLevel = new EnumMap<>(Level.class);
        for (CatalogWord word : wordCatalog.findOptionPool(level)) {
            optionPoolByLevel.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
        }

        RandomGenerator random = randomSupplier.get();
        List<CatalogWord> answers = new ArrayList<>(answerPool);
        shuffle(answers, random);

        List<GeneratedQuestion> result = new ArrayList<>(count);
        for (CatalogWord answer : answers.subList(0, count)) {
            List<CatalogWord> sameLevel = optionPoolByLevel.getOrDefault(answer.level(), List.of());
            result.add(new GeneratedQuestion(answer, pickOptions(answer, sameLevel, random)));
        }
        return result;
    }

    /** 실제 문제 수 = min(출제 가능 단어 수, 최대 문제 수). 메인 화면 안내도 같은 규칙을 쓴다 */
    public static int questionCount(int answerPoolSize, int maxCount) {
        return Math.min(answerPoolSize, maxCount);
    }

    /** 정답 + 오답 3개를 섞어서 반환 */
    private List<CatalogWord> pickOptions(CatalogWord answer, List<CatalogWord> sameLevel, RandomGenerator random) {
        List<CatalogWord> options = new ArrayList<>(GeneratedQuestion.OPTION_COUNT);
        options.add(answer);
        Set<Long> usedWordIds = new HashSet<>();
        usedWordIds.add(answer.wordId());
        Set<String> usedMeanings = new HashSet<>(answer.meanings());

        // 1차: 무작위로 뽑아 조건 검사
        for (int i = 0; !sameLevel.isEmpty() && i < RANDOM_TRY_LIMIT && options.size() < GeneratedQuestion.OPTION_COUNT; i++) {
            tryAdd(sameLevel.get(random.nextInt(sameLevel.size())), options, usedWordIds, usedMeanings);
        }
        // 2차: 전체를 섞어 순서대로 검사
        if (options.size() < GeneratedQuestion.OPTION_COUNT) {
            List<CatalogWord> shuffled = new ArrayList<>(sameLevel);
            shuffle(shuffled, random);
            for (CatalogWord candidate : shuffled) {
                if (options.size() >= GeneratedQuestion.OPTION_COUNT) {
                    break;
                }
                tryAdd(candidate, options, usedWordIds, usedMeanings);
            }
        }
        if (options.size() < GeneratedQuestion.OPTION_COUNT) {
            throw new CustomException(ErrorCode.TEST_NOT_ENOUGH_WORDS,
                    "'" + answer.headword() + "' 의 오답 보기 " + DISTRACTOR_COUNT + "개를 만들 수 없습니다.");
        }

        shuffle(options, random);
        return options;
    }

    private void tryAdd(CatalogWord candidate, List<CatalogWord> options,
                        Set<Long> usedWordIds, Set<String> usedMeanings) {
        if (usedWordIds.contains(candidate.wordId())) {
            return;
        }
        for (String meaning : candidate.meanings()) {
            if (usedMeanings.contains(meaning)) {
                return;
            }
        }
        options.add(candidate);
        usedWordIds.add(candidate.wordId());
        usedMeanings.addAll(candidate.meanings());
    }

    /** Fisher–Yates */
    private <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }

}

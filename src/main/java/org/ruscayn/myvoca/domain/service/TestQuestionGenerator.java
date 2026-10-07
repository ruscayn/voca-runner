package org.ruscayn.myvoca.domain.service;

import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.test.GeneratedQuestion;
import org.ruscayn.myvoca.domain.model.vocabulary.CatalogWord;
import org.ruscayn.myvoca.domain.model.vocabulary.WordCatalog;
import org.ruscayn.myvoca.shared.exception.CustomException;
import org.ruscayn.myvoca.shared.exception.ErrorCode;
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
     * 문제를 생성한다.
     *
     * @param level 사용자가 고른 레벨 (세부 또는 묶음)
     * @param count 문제 수
     */
    public List<GeneratedQuestion> generate(Level level, int count) {
        List<CatalogWord> pool = dedupeByWord(wordCatalog.findByLevel(level));
        if (pool.size() < count) {
            throw new CustomException(ErrorCode.TEST_NOT_ENOUGH_WORDS,
                    "레벨 " + level + " 단어 " + pool.size() + "개로 " + count + "문제를 만들 수 없습니다.");
        }

        Map<Level, List<CatalogWord>> poolByLevel = new EnumMap<>(Level.class);
        for (CatalogWord word : pool) {
            poolByLevel.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
        }

        RandomGenerator random = randomSupplier.get();
        List<CatalogWord> answers = new ArrayList<>(pool);
        shuffle(answers, random);

        List<GeneratedQuestion> result = new ArrayList<>(count);
        for (CatalogWord answer : answers.subList(0, count)) {
            result.add(new GeneratedQuestion(answer, pickOptions(answer, poolByLevel.get(answer.level()), random)));
        }
        return result;
    }

    /** 단어당 하나만 남긴다. 여러 레벨에 있으면 낮은 레벨 */
    private List<CatalogWord> dedupeByWord(List<CatalogWord> candidates) {
        Map<Long, CatalogWord> byWord = new LinkedHashMap<>();
        for (CatalogWord word : candidates) {
            byWord.merge(word.wordId(), word,
                    (current, next) -> next.level().isLowerThan(current.level()) ? next : current);
        }
        return new ArrayList<>(byWord.values());
    }

    /** 정답 + 오답 3개를 섞어서 반환 */
    private List<CatalogWord> pickOptions(CatalogWord answer, List<CatalogWord> sameLevel, RandomGenerator random) {
        List<CatalogWord> options = new ArrayList<>(GeneratedQuestion.OPTION_COUNT);
        options.add(answer);
        Set<Long> usedWordIds = new HashSet<>();
        usedWordIds.add(answer.wordId());
        Set<String> usedMeanings = new HashSet<>(answer.meanings());

        // 1차: 무작위로 뽑아 조건 검사
        for (int i = 0; i < RANDOM_TRY_LIMIT && options.size() < GeneratedQuestion.OPTION_COUNT; i++) {
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

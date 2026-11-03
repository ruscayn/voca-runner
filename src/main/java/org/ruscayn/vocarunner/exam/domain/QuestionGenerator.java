package org.ruscayn.vocarunner.exam.domain;

import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * 문항 생성기 : 시험 문항을 만드는 도메인 서비스. 단어는 WordCatalog(메모리)에서만 가져온다.
 *
 * <p>규칙
 * <ul>
 *   <li>한 시험에서 같은 단어는 한 번만 출제</li>
 *   <li>묶음 레벨(A 등)은 소속 세부 레벨 단어를 함께 사용. 두 레벨에 모두 있는 단어는 낮은 레벨(뜻) 사용</li>
 *   <li>시험 제외 단어는 정답 문제로도, 오답 보기로도 내지 않음</li>
 *   <li>보기 4개는 정답과 같은 세부 레벨 단어에서 선택</li>
 *   <li>보기 사이에 보기 뜻(글이 똑같은 것)이 하나라도 겹치면 안 됨</li>
 *   <li>헷갈리는 단어 묶음(confusable_group)에 함께 속한 단어는 같은 문제의 보기로 내지 않음
 *       (예: sir 문제에 teacher 를 오답으로 내지 않음. 오답끼리도 마찬가지)</li>
 * </ul>
 */
@Service
public class QuestionGenerator {

    /** 최대 문제 수에 이 값을 주면 레벨 단어 전체를 출제 (개발·베타 테스트용) */
    public static final int ALL_WORDS = 0;

    private static final int INCORRECT_OPTION_COUNT = Question.OPTION_COUNT - 1;
    /** 무작위 뽑기 시도 횟수. 초과하면 전체를 섞어 순서대로 검사 */
    private static final int RANDOM_TRY_LIMIT = 200;

    private final WordCatalog wordCatalog;
    /** 싱글톤 빈에서 여러 스레드가 쓰므로 호출마다 스레드 전용 난수를 사용 */
    private final Supplier<RandomGenerator> randomSupplier;

    @Autowired
    public QuestionGenerator(WordCatalog wordCatalog) {
        this(wordCatalog, ThreadLocalRandom::current);
    }

    /** 테스트에서 시드 고정용 */
    public QuestionGenerator(WordCatalog wordCatalog, Supplier<RandomGenerator> randomSupplier) {
        this.wordCatalog = wordCatalog;
        this.randomSupplier = randomSupplier;
    }

    /**
     * 문제를 생성한다. 정답 후보가 maxCount 보다 적으면 그 레벨 단어 전체를 출제한다.
     *
     * @param level    사용자가 고른 레벨 (세부 또는 묶음)
     * @param maxCount 최대 문제 수 (설정값). {@link #ALL_WORDS}(0)이면 레벨 단어 전체
     */
    public List<Question> generate(Level level, int maxCount) {
        List<CatalogWord> answerPool = wordCatalog.findAnswerPool(level);
        int count = questionCount(answerPool.size(), maxCount);
        if (count == 0) {
            throw new CustomException(ErrorCode.EXAM_NOT_ENOUGH_WORDS, "레벨 " + level + " 에 출제 가능한 단어가 없습니다.");
        }

        // 오답 보기 후보 (시험 제외 단어 제외), 세부 레벨별
        Map<Level, List<CatalogWord>> optionPoolByLevel = new EnumMap<>(Level.class);
        for (CatalogWord word : wordCatalog.findOptionPool(level)) {
            optionPoolByLevel.computeIfAbsent(word.level(), k -> new ArrayList<>()).add(word);
        }

        RandomGenerator random = randomSupplier.get();
        List<CatalogWord> answers = new ArrayList<>(answerPool);
        shuffle(answers, random);

        List<Question> result = new ArrayList<>(count);
        for (CatalogWord answer : answers.subList(0, count)) {
            List<CatalogWord> sameLevel = optionPoolByLevel.getOrDefault(answer.level(), List.of());
            result.add(new Question(answer, pickOptions(answer, sameLevel, random)));
        }
        return result;
    }

    /**
     * 실제 문제 수 = min(정답 후보 수, 최대 문항 수). 메인 화면 안내도 같은 규칙을 쓴다.
     * 최대 문제 수가 {@link #ALL_WORDS}(0)이면 레벨 단어 전체를 출제한다.
     */
    public static int questionCount(int answerPoolSize, int maxCount) {
        if (maxCount == ALL_WORDS) {
            return answerPoolSize;
        }
        return Math.min(answerPoolSize, maxCount);
    }

    /** 정답 + 오답 3개를 섞어서 반환 */
    private List<CatalogWord> pickOptions(CatalogWord answer, List<CatalogWord> sameLevel, RandomGenerator random) {
        List<CatalogWord> options = new ArrayList<>(Question.OPTION_COUNT);
        options.add(answer);
        // 이미 고른 단어 + 그 단어들과 헷갈리는 단어 → 보기로 낼 수 없는 word_id
        Set<Long> blockedWordIds = new HashSet<>();
        blockedWordIds.add(answer.wordId());
        blockedWordIds.addAll(wordCatalog.findConfusableWordIds(answer.wordId()));
        Set<String> usedMeanings = new HashSet<>(answer.optionMeanings());

        // 1차: 무작위로 뽑아 조건 검사
        for (int i = 0; !sameLevel.isEmpty() && i < RANDOM_TRY_LIMIT && options.size() < Question.OPTION_COUNT; i++) {
            tryAdd(sameLevel.get(random.nextInt(sameLevel.size())), options, blockedWordIds, usedMeanings);
        }
        // 2차: 전체를 섞어 순서대로 검사
        if (options.size() < Question.OPTION_COUNT) {
            List<CatalogWord> shuffled = new ArrayList<>(sameLevel);
            shuffle(shuffled, random);
            for (CatalogWord candidate : shuffled) {
                if (options.size() >= Question.OPTION_COUNT) {
                    break;
                }
                tryAdd(candidate, options, blockedWordIds, usedMeanings);
            }
        }
        if (options.size() < Question.OPTION_COUNT) {
            throw new CustomException(ErrorCode.EXAM_NOT_ENOUGH_WORDS,
                    "'" + answer.headword() + "' 의 오답 보기 " + INCORRECT_OPTION_COUNT + "개를 만들 수 없습니다.");
        }

        shuffle(options, random);
        return options;
    }

    /**
     * 조건을 통과하면 보기에 추가한다.
     * 탈락: 이미 고른 단어 / 고른 단어와 헷갈리는 단어(묶음) / 고른 보기와 보기 뜻이 하나라도 같은 단어
     */
    private void tryAdd(CatalogWord candidate, List<CatalogWord> options,
                        Set<Long> blockedWordIds, Set<String> usedMeanings) {
        if (blockedWordIds.contains(candidate.wordId())) {
            return;
        }
        for (String meaning : candidate.optionMeanings()) {
            if (usedMeanings.contains(meaning)) {
                return;
            }
        }
        options.add(candidate);
        blockedWordIds.add(candidate.wordId());
        blockedWordIds.addAll(wordCatalog.findConfusableWordIds(candidate.wordId()));
        usedMeanings.addAll(candidate.optionMeanings());
    }

    /** Fisher–Yates */
    private <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }

}

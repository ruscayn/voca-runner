package org.ruscayn.myvoca.domain.service;

import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.test.TestQuestion;
import org.ruscayn.myvoca.domain.model.test.TestSession;
import org.ruscayn.myvoca.domain.model.vocabulary.LevelWord;
import org.ruscayn.myvoca.shared.exception.CustomException;
import org.ruscayn.myvoca.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * 시험 문제 생성 도메인 서비스.
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

    private static final int DISTRACTOR_COUNT = TestQuestion.OPTION_COUNT - 1;
    /** 무작위 뽑기 시도 횟수. 초과하면 전체를 섞어 순서대로 검사 */
    private static final int RANDOM_TRY_LIMIT = 200;

    /** 싱글톤 빈에서 여러 스레드가 쓰므로 호출마다 스레드 전용 난수를 사용 */
    private final Supplier<RandomGenerator> randomSupplier;

    public TestQuestionGenerator() {
        this(ThreadLocalRandom::current);
    }

    /** 테스트에서 시드 고정용 */
    public TestQuestionGenerator(Supplier<RandomGenerator> randomSupplier) {
        this.randomSupplier = randomSupplier;
    }

    /**
     * 세션에 문제를 생성해 추가한다.
     *
     * @param session    문제를 추가할 세션 (questionCount, level 사용)
     * @param candidates 세션 레벨의 세부 레벨들에 속한 level_word 전체
     */
    public void generate(TestSession session, List<LevelWord> candidates) {
        List<LevelWord> pool = dedupeByWord(candidates, session.getLevel());
        int count = session.getQuestionCount();
        if (pool.size() < count) {
            throw new CustomException(ErrorCode.TEST_NOT_ENOUGH_WORDS,
                    "레벨 " + session.getLevel() + " 단어 " + pool.size() + "개로 " + count + "문제를 만들 수 없습니다.");
        }

        Map<Level, List<LevelWord>> poolByLevel = new EnumMap<>(Level.class);
        for (LevelWord lw : pool) {
            poolByLevel.computeIfAbsent(lw.getLevel(), k -> new ArrayList<>()).add(lw);
        }

        RandomGenerator random = randomSupplier.get();
        List<LevelWord> answers = new ArrayList<>(pool);
        shuffle(answers, random);

        for (LevelWord answer : answers.subList(0, count)) {
            List<LevelWord> options = pickOptions(answer, poolByLevel.get(answer.getLevel()), random);
            session.addQuestion(answer, options);
        }
    }

    /** 단어당 하나만 남긴다. 여러 레벨에 있으면 낮은 레벨 */
    private List<LevelWord> dedupeByWord(List<LevelWord> candidates, Level sessionLevel) {
        Set<Level> allowed = EnumSet.copyOf(sessionLevel.detailLevels());
        Map<Long, LevelWord> byWord = new LinkedHashMap<>();
        for (LevelWord lw : candidates) {
            if (!allowed.contains(lw.getLevel())) {
                continue;
            }
            byWord.merge(lw.getWord().getId(), lw,
                    (current, next) -> next.getLevel().isLowerThan(current.getLevel()) ? next : current);
        }
        return new ArrayList<>(byWord.values());
    }

    /** 정답 + 오답 3개를 섞어서 반환 */
    private List<LevelWord> pickOptions(LevelWord answer, List<LevelWord> sameLevel, RandomGenerator random) {
        List<LevelWord> options = new ArrayList<>(TestQuestion.OPTION_COUNT);
        options.add(answer);
        Set<Long> usedWordIds = new HashSet<>();
        usedWordIds.add(answer.getWord().getId());
        Set<String> usedMeanings = new HashSet<>(answer.displayMeaningTexts());

        // 1차: 무작위로 뽑아 조건 검사
        for (int i = 0; i < RANDOM_TRY_LIMIT && options.size() < TestQuestion.OPTION_COUNT; i++) {
            tryAdd(sameLevel.get(random.nextInt(sameLevel.size())), options, usedWordIds, usedMeanings);
        }
        // 2차: 전체를 섞어 순서대로 검사
        if (options.size() < TestQuestion.OPTION_COUNT) {
            List<LevelWord> shuffled = new ArrayList<>(sameLevel);
            shuffle(shuffled, random);
            for (LevelWord candidate : shuffled) {
                if (options.size() >= TestQuestion.OPTION_COUNT) {
                    break;
                }
                tryAdd(candidate, options, usedWordIds, usedMeanings);
            }
        }
        if (options.size() < TestQuestion.OPTION_COUNT) {
            throw new CustomException(ErrorCode.TEST_NOT_ENOUGH_WORDS,
                    "'" + answer.headword() + "' 의 오답 보기 " + DISTRACTOR_COUNT + "개를 만들 수 없습니다.");
        }

        shuffle(options, random);
        return options;
    }

    private void tryAdd(LevelWord candidate, List<LevelWord> options,
                        Set<Long> usedWordIds, Set<String> usedMeanings) {
        if (usedWordIds.contains(candidate.getWord().getId())) {
            return;
        }
        List<String> meanings = candidate.displayMeaningTexts();
        for (String meaning : meanings) {
            if (usedMeanings.contains(meaning)) {
                return;
            }
        }
        options.add(candidate);
        usedWordIds.add(candidate.getWord().getId());
        usedMeanings.addAll(meanings);
    }

    /** Fisher–Yates */
    private <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }

}

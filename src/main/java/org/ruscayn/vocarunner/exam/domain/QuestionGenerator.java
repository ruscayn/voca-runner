package org.ruscayn.vocarunner.exam.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

/** 한 시험에서 같은 단어는 한 번만 낸다 (정답 후보 규칙은 {@link WordCatalog}, 보기 규칙은 {@link OptionGenerator}) */
@RequiredArgsConstructor
public class QuestionGenerator {

    public static final int ALL_WORDS = 0;

    /** 여러 요청(스레드)이 함께 쓰므로 부를 때마다 그 스레드의 난수를 쓴다 */
    private final Supplier<RandomGenerator> randomSupplier;

    public QuestionGenerator() {
        this(ThreadLocalRandom::current);
    }

    public List<Question> generate(WordCatalog wordCatalog, Level level, int maxCount) {
        List<CatalogWord> answerPool = wordCatalog.findAnswerPool(level);
        int count = requireQuestionCount(level, answerPool.size(), maxCount);
        RandomGenerator random = randomSupplier.get();
        OptionGenerator optionGenerator = new OptionGenerator(wordCatalog, answerPool, random);
        return shuffled(answerPool, random).subList(0, count).stream()
                .map(answer -> new Question(answer, optionGenerator.generate(answer)))
                .toList();
    }

    private static int requireQuestionCount(Level level, int answerPoolSize, int maxCount) {
        int count = questionCount(answerPoolSize, maxCount);
        if (count == 0) {
            throw new CustomException(ErrorCode.EXAM_NOT_ENOUGH_WORDS, "레벨 " + level + " 에 출제 가능한 단어가 없습니다.");
        }
        return count;
    }

    public static int questionCount(int answerPoolSize, int maxCount) {
        if (maxCount == ALL_WORDS) {
            return answerPoolSize;
        }
        return Math.min(answerPoolSize, maxCount);
    }

    private static List<CatalogWord> shuffled(List<CatalogWord> words, RandomGenerator random) {
        List<CatalogWord> copy = new ArrayList<>(words);
        Collections.shuffle(copy, random);
        return copy;
    }

}

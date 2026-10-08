package org.ruscayn.vocarunner.domain.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.test.GeneratedQuestion;
import org.ruscayn.vocarunner.domain.model.vocabulary.CatalogWord;
import org.ruscayn.vocarunner.infrastructure.cache.InMemoryWordCatalog;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.word;
import static org.ruscayn.vocarunner.support.TestWords.words;

@DisplayName("TestQuestionGenerator : 문제 생성 규칙")
class TestQuestionGeneratorTest {

    private static TestQuestionGenerator generator(List<CatalogWord> catalogWords, long seed) {
        return new TestQuestionGenerator(new InMemoryWordCatalog(catalogWords), () -> new Random(seed));
    }

    /** 모든 문제에 공통으로 지켜야 하는 규칙 */
    private static void assertQuestionRules(List<GeneratedQuestion> questions) {
        Set<Long> answerWordIds = new HashSet<>();
        for (GeneratedQuestion q : questions) {
            // 한 시험에서 같은 단어는 한 번만 출제
            assertThat(answerWordIds.add(q.answer().wordId()))
                    .as("정답 단어 중복: %s", q.answer().headword()).isTrue();

            // 보기 4개, 정답 1번 포함, 보기 단어 중복 없음
            assertThat(q.options()).hasSize(GeneratedQuestion.OPTION_COUNT).contains(q.answer());
            assertThat(q.options().stream().map(CatalogWord::wordId).distinct().count())
                    .as("보기 단어 중복").isEqualTo(GeneratedQuestion.OPTION_COUNT);

            // 보기는 정답과 같은 세부 레벨
            assertThat(q.options()).allMatch(o -> o.level() == q.answer().level());

            // 보기 간 표시 뜻이 하나도 겹치지 않음
            Set<String> meanings = new HashSet<>();
            for (CatalogWord option : q.options()) {
                for (String meaning : option.meanings()) {
                    assertThat(meanings.add(meaning)).as("보기 뜻 중복: %s", meaning).isTrue();
                }
            }
        }
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 7, 42, 100, 2026, 9999})
    @DisplayName("세부 레벨 : 요청한 수만큼, 규칙을 지켜 생성")
    void detailLevel(long seed) {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.B1, 1, 60));
        catalog.addAll(words(Level.B2, 1001, 60));    // 다른 레벨 단어는 섞이면 안 됨

        List<GeneratedQuestion> questions = generator(catalog, seed).generate(Level.B1, 50);

        assertThat(questions).hasSize(50);
        assertThat(questions).allMatch(q -> q.answer().level() == Level.B1);
        assertQuestionRules(questions);
    }

    @Test
    @DisplayName("단어 수와 문제 수가 같으면 모든 단어가 한 번씩 출제")
    void usesAllWords() {
        List<CatalogWord> catalog = words(Level.A1, 1, 20);

        List<GeneratedQuestion> questions = generator(catalog, 1).generate(Level.A1, 20);

        assertThat(questions).extracting(q -> q.answer().wordId())
                .containsExactlyInAnyOrderElementsOf(catalog.stream().map(CatalogWord::wordId).toList());
        assertQuestionRules(questions);
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 5, 42, 777})
    @DisplayName("뜻이 일부 겹치는 단어가 많아도 보기 간 뜻은 겹치지 않는다")
    void overlappingMeanings(long seed) {
        // 단어 i 의 뜻 : 공통 뜻(5종 중 하나) + 고유 뜻 → 같은 공통 뜻 단어가 보기에 함께 나오면 안 됨
        List<CatalogWord> catalog = new ArrayList<>();
        for (long id = 1; id <= 40; id++) {
            catalog.add(word(id, id, Level.C1, "공통" + (id % 5), "고유" + id));
        }

        List<GeneratedQuestion> questions = generator(catalog, seed).generate(Level.C1, 40);

        assertQuestionRules(questions);
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 3, 42, 2026})
    @DisplayName("묶음 레벨 : 두 레벨에 모두 있는 단어는 낮은 레벨(뜻)로만 출제")
    void groupLevelUsesLowerLevel(long seed) {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 30));
        catalog.addAll(words(Level.A2, 101, 30));
        // word 1~10 은 A2 에도 있음 (levelWordId 는 다르고 뜻도 다름)
        Set<Long> a2DuplicateIds = new HashSet<>();
        for (long wordId = 1; wordId <= 10; wordId++) {
            long levelWordId = 500 + wordId;
            catalog.add(word(levelWordId, wordId, Level.A2, "A2뜻" + wordId));
            a2DuplicateIds.add(levelWordId);
        }

        // 고유 단어 = A1 30 + A2 30 = 60개 (중복 10개는 한 번만)
        List<GeneratedQuestion> questions = generator(catalog, seed).generate(Level.A, 60);

        assertThat(questions).hasSize(60);
        assertQuestionRules(questions);
        assertThat(questions)
                .flatExtracting(GeneratedQuestion::options)
                .extracting(CatalogWord::levelWordId)
                .as("A2 쪽 중복 단어는 정답·보기로 쓰이지 않음")
                .doesNotContainAnyElementsOf(a2DuplicateIds);
    }

    @Test
    @DisplayName("묶음 레벨 : 중복 정리 후 단어가 부족하면 생성하지 않는다")
    void groupLevelCountsAfterDedupe() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 10));
        // A2 는 A1 과 같은 단어 10개뿐 → 고유 단어 10개
        for (long wordId = 1; wordId <= 10; wordId++) {
            catalog.add(word(100 + wordId, wordId, Level.A2, "A2뜻" + wordId));
        }

        assertThatThrownBy(() -> generator(catalog, 1).generate(Level.A, 11))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEST_NOT_ENOUGH_WORDS);
    }

    @Test
    @DisplayName("단어가 문제 수보다 적으면 TEST_NOT_ENOUGH_WORDS")
    void notEnoughWords() {
        List<CatalogWord> catalog = words(Level.A1, 1, 10);

        assertThatThrownBy(() -> generator(catalog, 1).generate(Level.A1, 11))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEST_NOT_ENOUGH_WORDS);
    }

    @Test
    @DisplayName("뜻이 모두 같아 오답 보기를 만들 수 없으면 TEST_NOT_ENOUGH_WORDS")
    void cannotMakeDistractors() {
        List<CatalogWord> catalog = new ArrayList<>();
        for (long id = 1; id <= 10; id++) {
            catalog.add(word(id, id, Level.B2, "같은 뜻"));
        }

        assertThatThrownBy(() -> generator(catalog, 1).generate(Level.B2, 1))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEST_NOT_ENOUGH_WORDS);
    }

    @Test
    @DisplayName("같은 시드면 같은 문제가 나온다 (재현 가능)")
    void sameSeedSameResult() {
        List<CatalogWord> catalog = words(Level.B1, 1, 40);

        List<GeneratedQuestion> first = generator(catalog, 42).generate(Level.B1, 30);
        List<GeneratedQuestion> second = generator(catalog, 42).generate(Level.B1, 30);

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("정답 위치가 한 번호에 몰리지 않는다")
    void answerPositionSpread() {
        List<CatalogWord> catalog = words(Level.B1, 1, 400);

        List<GeneratedQuestion> questions = generator(catalog, 42).generate(Level.B1, 400);

        int[] counts = new int[GeneratedQuestion.OPTION_COUNT + 1];
        questions.forEach(q -> counts[q.correctOptionNo()]++);
        // 400문항 → 번호당 평균 100. 무작위 편차를 고려해 넉넉하게 60~140
        for (int no = 1; no <= GeneratedQuestion.OPTION_COUNT; no++) {
            assertThat(counts[no]).as("정답 %d번 개수", no).isBetween(60, 140);
        }
    }

}

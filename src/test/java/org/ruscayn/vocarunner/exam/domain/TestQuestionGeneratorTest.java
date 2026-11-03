package org.ruscayn.vocarunner.exam.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.InMemoryWordCatalog;
import org.ruscayn.vocarunner.word.domain.Level;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.excluded;
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

    @Test
    @DisplayName("최대 문제 수 0 : 레벨 단어 전체를 출제")
    void zeroMeansAllWords() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 30));
        catalog.addAll(words(Level.A2, 101, 30));    // 다른 레벨 단어는 섞이면 안 됨

        List<GeneratedQuestion> questions = generator(catalog, 1).generate(Level.A1, TestQuestionGenerator.ALL_WORDS);

        assertThat(questions).hasSize(30);
        assertThat(questions).allMatch(q -> q.answer().level() == Level.A1);
        assertQuestionRules(questions);
    }

    @Test
    @DisplayName("문제 수 계산 : min(단어 수, 최대 문제 수), 0 이면 단어 수 전체")
    void questionCountRule() {
        assertThat(TestQuestionGenerator.questionCount(997, 500)).isEqualTo(500);
        assertThat(TestQuestionGenerator.questionCount(300, 500)).isEqualTo(300);
        assertThat(TestQuestionGenerator.questionCount(997, TestQuestionGenerator.ALL_WORDS)).isEqualTo(997);
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
    @DisplayName("묶음 레벨 : 중복 정리 후 단어 수가 문항 수보다 적으면 고유 단어 전체를 출제")
    void groupLevelCountsAfterDedupe() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 10));
        // A2 는 A1 과 같은 단어 10개뿐 → 고유 단어 10개
        for (long wordId = 1; wordId <= 10; wordId++) {
            catalog.add(word(100 + wordId, wordId, Level.A2, "A2뜻" + wordId));
        }

        List<GeneratedQuestion> questions = generator(catalog, 1).generate(Level.A, 11);

        assertThat(questions).hasSize(10);
        assertQuestionRules(questions);
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 3, 42, 2026})
    @DisplayName("시험 제외 단어는 정답으로도, 오답 보기로도 나오지 않는다")
    void excludedNeverUsed(long seed) {
        // 출제 20개 + 시험 제외 40개 → 제외 단어가 더 많아도 보기는 출제 단어로만 채움
        List<CatalogWord> catalog = new ArrayList<>(words(Level.B1, 1, 20));
        Set<Long> excludedIds = new HashSet<>();
        for (long id = 101; id <= 140; id++) {
            catalog.add(excluded(id, id, Level.B1));
            excludedIds.add(id);
        }

        List<GeneratedQuestion> questions = generator(catalog, seed).generate(Level.B1, 20);

        assertThat(questions).hasSize(20);
        assertQuestionRules(questions);
        assertThat(questions).extracting(q -> q.answer().levelWordId())
                .as("정답에 시험 제외 단어 없음")
                .doesNotContainAnyElementsOf(excludedIds);
        assertThat(questions).flatExtracting(GeneratedQuestion::options)
                .extracting(CatalogWord::levelWordId)
                .as("오답 보기에도 시험 제외 단어 없음")
                .doesNotContainAnyElementsOf(excludedIds);
    }

    @Test
    @DisplayName("출제 단어 수는 시험 제외 단어를 빼고 센다 (문항 수가 더 많으면 출제 가능 단어만 출제)")
    void excludedNotCountedAsAnswerPool() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 10));
        for (long id = 101; id <= 130; id++) {
            catalog.add(excluded(id, id, Level.A1));
        }

        assertThat(new InMemoryWordCatalog(catalog).countByLevel(Level.A1)).isEqualTo(10);

        List<GeneratedQuestion> questions = generator(catalog, 1).generate(Level.A1, 11);

        assertThat(questions).hasSize(10);
        assertThat(questions).allMatch(q -> !q.answer().testExcluded());
    }

    @Test
    @DisplayName("레벨에 출제 가능 단어가 하나도 없으면 TEST_NOT_ENOUGH_WORDS")
    void noAnswerableWords() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 10));
        for (long id = 101; id <= 110; id++) {
            catalog.add(excluded(id, id, Level.A2));
        }

        assertThatThrownBy(() -> generator(catalog, 1).generate(Level.A2, 5))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEST_NOT_ENOUGH_WORDS);
    }

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 3, 42, 2026})
    @DisplayName("묶음 레벨 : 낮은 레벨이 시험 제외면 높은 레벨 행으로 출제")
    void groupLevelExcludedLowerUsesHigher(long seed) {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 30));
        catalog.addAll(words(Level.A2, 101, 30));
        // word 900 : A1 은 시험 제외, A2 는 출제
        catalog.add(excluded(900, 900, Level.A1, "A1뜻900"));
        catalog.add(word(901, 900, Level.A2, "A2뜻900"));

        // 출제 가능 = A1 30 + A2 30 + word 900(A2) = 61
        assertThat(new InMemoryWordCatalog(catalog).countByLevel(Level.A)).isEqualTo(61);

        List<GeneratedQuestion> questions = generator(catalog, seed).generate(Level.A, 61);

        assertQuestionRules(questions);
        assertThat(questions).extracting(q -> q.answer().levelWordId())
                .as("word 900 은 A2 행으로 출제")
                .contains(901L)
                .doesNotContain(900L);
    }

    @Test
    @DisplayName("묶음 레벨 출제 단어 수는 같은 단어를 한 번만 센다")
    void groupLevelCountDedupe() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.B1, 1, 10));
        catalog.addAll(words(Level.B2, 101, 5));
        for (long wordId = 1; wordId <= 3; wordId++) {      // B1 단어 3개가 B2 에도 있음
            catalog.add(word(500 + wordId, wordId, Level.B2, "B2뜻" + wordId));
        }

        InMemoryWordCatalog wordCatalog = new InMemoryWordCatalog(catalog);

        assertThat(wordCatalog.countByLevel(Level.B1)).isEqualTo(10);
        assertThat(wordCatalog.countByLevel(Level.B2)).isEqualTo(8);
        assertThat(wordCatalog.countByLevel(Level.B)).isEqualTo(15);
    }

    @Test
    @DisplayName("단어가 문제 수보다 적으면 그 레벨 단어 전체를 한 번씩 출제")
    void fewerWordsThanCount() {
        List<CatalogWord> catalog = words(Level.A1, 1, 10);

        List<GeneratedQuestion> questions = generator(catalog, 1).generate(Level.A1, 10000);

        assertThat(questions).extracting(q -> q.answer().wordId())
                .containsExactlyInAnyOrderElementsOf(catalog.stream().map(CatalogWord::wordId).toList());
        assertQuestionRules(questions);
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

    @ParameterizedTest(name = "seed {0}")
    @ValueSource(longs = {1, 2, 3, 7, 42, 100, 2026, 9999})
    @DisplayName("헷갈리는 단어 묶음 : 같은 묶음 단어는 한 문제의 보기로 함께 나오지 않는다 (정답·오답 모두)")
    void confusableNotTogether(long seed) {
        List<CatalogWord> catalog = words(Level.A1, 1, 60);
        // 2개 묶음 15개 (1,2) (3,4) ... (29,30) + 3개 묶음 5개 (31,32,33) ... (43,44,45)
        List<List<Long>> groups = new ArrayList<>();
        for (long id = 1; id <= 29; id += 2) {
            groups.add(List.of(id, id + 1));
        }
        for (long id = 31; id <= 43; id += 3) {
            groups.add(List.of(id, id + 1, id + 2));
        }
        InMemoryWordCatalog wordCatalog = new InMemoryWordCatalog(catalog, groups);

        List<GeneratedQuestion> questions =
                new TestQuestionGenerator(wordCatalog, () -> new Random(seed)).generate(Level.A1, 60);

        assertThat(questions).hasSize(60);
        assertQuestionRules(questions);
        for (GeneratedQuestion q : questions) {
            for (CatalogWord a : q.options()) {
                for (CatalogWord b : q.options()) {
                    assertThat(wordCatalog.findConfusableWordIds(a.wordId()))
                            .as("문제 %s : 헷갈리는 보기 %s, %s", q.answer().headword(), a.headword(), b.headword())
                            .doesNotContain(b.wordId());
                }
            }
        }
    }

    @Test
    @DisplayName("헷갈리는 단어 묶음 : 묶음이 다르면 함께 나올 수 있다 (sir-teacher, sir-Mr. 이어도 teacher-Mr. 는 가능)")
    void confusableNotTransitive() {
        // 1=sir, 2=teacher, 3=Mr. / 묶음 {1,2}, {1,3}
        // 단어 6개 (sir 문제는 teacher·Mr. 를 빼고 4,5,6 으로 오답을 채울 수 있음)
        // teacher 문제 : sir 는 절대 안 나오고, Mr. 는 teacher 와 묶음이 아니므로 나올 수 있음
        List<CatalogWord> catalog = words(Level.A1, 1, 6);
        InMemoryWordCatalog wordCatalog = new InMemoryWordCatalog(catalog, List.of(List.of(1L, 2L), List.of(1L, 3L)));

        boolean mrAppeared = false;
        for (long seed = 1; seed <= 30; seed++) {
            long s = seed;
            List<GeneratedQuestion> questions =
                    new TestQuestionGenerator(wordCatalog, () -> new Random(s)).generate(Level.A1, 6);

            GeneratedQuestion teacher = questions.stream()
                    .filter(q -> q.answer().wordId() == 2L).findFirst().orElseThrow();
            List<Long> optionIds = teacher.options().stream().map(CatalogWord::wordId).toList();
            assertThat(optionIds).as("teacher 문제에 sir 없음").doesNotContain(1L);
            mrAppeared |= optionIds.contains(3L);
        }
        assertThat(mrAppeared).as("teacher 문제에 Mr. 가 나올 수 있음").isTrue();
    }

    @Test
    @DisplayName("헷갈리는 단어 묶음 때문에 오답 보기를 만들 수 없으면 TEST_NOT_ENOUGH_WORDS")
    void confusableLeavesNotEnoughDistractors() {
        // 단어 4개 중 1·2 가 같은 묶음 → 1 문제의 오답 후보는 3, 4 뿐
        List<CatalogWord> catalog = words(Level.A1, 1, 4);
        InMemoryWordCatalog wordCatalog = new InMemoryWordCatalog(catalog, List.of(List.of(1L, 2L)));

        assertThatThrownBy(() -> new TestQuestionGenerator(wordCatalog, () -> new Random(1)).generate(Level.A1, 4))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TEST_NOT_ENOUGH_WORDS);
    }

    @Test
    @DisplayName("헷갈리는 단어 map : 양방향, 여러 묶음은 합침, 묶음 없는 단어는 빈 집합")
    void confusableMap() {
        InMemoryWordCatalog wordCatalog = new InMemoryWordCatalog(words(Level.A1, 1, 10),
                List.of(List.of(1L, 2L), List.of(1L, 3L), List.of(4L, 5L, 6L)));

        assertThat(wordCatalog.findConfusableWordIds(1L)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(wordCatalog.findConfusableWordIds(2L)).containsExactly(1L);
        assertThat(wordCatalog.findConfusableWordIds(3L)).containsExactly(1L);
        assertThat(wordCatalog.findConfusableWordIds(5L)).containsExactlyInAnyOrder(4L, 6L);
        assertThat(wordCatalog.findConfusableWordIds(7L)).isEmpty();
        assertThat(wordCatalog.confusableGroupCount()).isEqualTo(3);
        assertThat(wordCatalog.confusableWordCount()).isEqualTo(6);
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

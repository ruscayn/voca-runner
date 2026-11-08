package org.ruscayn.vocarunner.word.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.word.application.port.out.WordPersistencePort;
import org.ruscayn.vocarunner.word.domain.ExposureAction;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.LevelWord;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.ruscayn.vocarunner.word.domain.WordExposureRules.Rule;
import org.ruscayn.vocarunner.word.domain.WordMeaning;

@DisplayName("LoadWordCatalogService : 단어 출제 목록 적재")
class LoadWordCatalogServiceTest {

    /**
     * <pre>
     * 1 lark : B2 · C2 (C2 레벨 제외)
     * 2 sex  : B1 성별 · 성관계 (뜻 2 뜻 제외)
     * 3 gook : B2 (단어 제외)
     * 4 bus  : A1, 혼동 단어 묶음 {lark, bus}
     * </pre>
     */
    private static final WordPersistencePort WORDS = new WordPersistencePort() {

        @Override
        public List<Rule> loadExposureRules() {
            return List.of(
                    new Rule(1, null, Level.C2, ExposureAction.EXCLUDE_LEVEL),
                    new Rule(2, 2, null, ExposureAction.EXCLUDE_MEANING),
                    new Rule(3, null, null, ExposureAction.EXCLUDE_WORD));
        }

        @Override
        public List<LevelWord> loadLevelWords() {
            return List.of(
                    levelWord(101, 1, Level.B2, "종달새"), levelWord(102, 1, Level.C2, "종달새"),
                    levelWord(201, 2, Level.B1, "성별", "성관계"),
                    levelWord(301, 3, Level.B2, "끈적한 것"),
                    levelWord(401, 4, Level.A1, "버스"));
        }

        @Override
        public Collection<List<Long>> loadConfusableGroups() {
            return List.of(List.of(1L, 4L));
        }

    };

    private static LevelWord levelWord(long levelWordId, long wordId, Level level, String... meanings) {
        List<WordMeaning> optionMeanings = IntStream.range(0, meanings.length)
                .mapToObj(i -> new WordMeaning(i + 1, meanings[i]))
                .toList();
        return new LevelWord(levelWordId, wordId, "word" + wordId, null, level, optionMeanings);
    }

    @Test
    @DisplayName("읽은 레벨 단어마다 단어 노출을 적용해 출제 목록을 만들고 교체한다")
    void load() {
        AtomicReference<WordCatalog> replaced = new AtomicReference<>();

        new LoadWordCatalogService(WORDS, replaced::set).load();

        WordCatalog wordCatalog = replaced.get();

        assertThat(wordCatalog.getWordCount()).isEqualTo(5);
        assertThat(wordCatalog.get(101).excluded()).isFalse();
        assertThat(wordCatalog.get(102).excluded()).as("레벨 제외").isTrue();
        assertThat(wordCatalog.get(201).optionMeanings()).as("뜻 제외").containsExactly("성별");
        assertThat(wordCatalog.get(301).excluded()).as("단어 제외").isTrue();
        assertThat(wordCatalog.findAnswerPool(Level.B2)).extracting(word -> word.levelWordId()).containsExactly(101L);
        assertThat(wordCatalog.findConfusableWordIds(1)).containsExactly(4L);
    }

}

package org.ruscayn.vocarunner.word.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.word.domain.WordExposureRules.Rule;

@DisplayName("WordExposureRules : 단어 노출 적용")
class WordExposureRulesTest {

    private static final long LARK = 1;
    private static final long SEX = 2;
    private static final long GOOK = 3;

    private static final LevelWord LARK_B2 = levelWord(101, LARK, Level.B2, "종달새", "장난");
    private static final LevelWord LARK_C2 = levelWord(102, LARK, Level.C2, "종달새", "장난");
    private static final LevelWord SEX_B1 = levelWord(201, SEX, Level.B1, "성별", "성관계");

    /** 보기 뜻의 순번은 1부터 차례대로 */
    private static LevelWord levelWord(long levelWordId, long wordId, Level level, String... meanings) {
        List<WordMeaning> optionMeanings = IntStream.range(0, meanings.length)
                .mapToObj(i -> new WordMeaning(i + 1, meanings[i]))
                .toList();
        return new LevelWord(levelWordId, wordId, "word" + wordId, null, level, optionMeanings);
    }

    private static WordExposureRules rules(Rule... rules) {
        return new WordExposureRules(List.of(rules));
    }

    @Test
    @DisplayName("판정이 없으면 보기 그대로, 출제")
    void noRule() {
        CatalogWord word = rules().apply(LARK_B2);

        assertThat(word.optionMeanings()).containsExactly("종달새", "장난");
        assertThat(word.excluded()).isFalse();
    }

    @Test
    @DisplayName("레벨 단어의 값을 그대로 출제 목록 단어로 옮긴다")
    void toCatalogWord() {
        LevelWord levelWord = new LevelWord(7, 3, "for the sake of", "for one's sake", Level.B2,
                List.of(new WordMeaning(1, "~을 위해")));

        assertThat(rules().apply(levelWord))
                .isEqualTo(new CatalogWord(7, 3, "for the sake of", "for one's sake", Level.B2, List.of("~을 위해"),
                        false));
    }

    @Test
    @DisplayName("레벨 제외는 그 레벨 단어만 뺀다 (lark C2 제외, B2 출제)")
    void excludeLevel() {
        WordExposureRules rules = rules(new Rule(LARK, null, Level.C2, ExposureAction.EXCLUDE_LEVEL));

        assertThat(rules.apply(LARK_C2).excluded()).isTrue();
        assertThat(rules.apply(LARK_B2).excluded()).isFalse();
    }

    @Test
    @DisplayName("단어 제외는 그 단어의 모든 레벨 단어를 뺀다")
    void excludeWord() {
        WordExposureRules rules = rules(new Rule(GOOK, null, null, ExposureAction.EXCLUDE_WORD));

        for (Level level : List.of(Level.B2, Level.C1)) {
            assertThat(rules.apply(levelWord(300, GOOK, level, "끈적한 것")).excluded()).isTrue();
        }
        assertThat(rules.apply(LARK_B2).excluded()).as("다른 단어").isFalse();
    }

    @Test
    @DisplayName("뜻 제외는 그 뜻만 보기에서 빼고 레벨 단어는 출제")
    void excludeMeaning() {
        WordExposureRules rules = rules(new Rule(SEX, 2, null, ExposureAction.EXCLUDE_MEANING));

        CatalogWord word = rules.apply(SEX_B1);

        assertThat(word.optionMeanings()).containsExactly("성별");
        assertThat(word.excluded()).isFalse();
    }

    @Test
    @DisplayName("뜻 제외로 보일 뜻이 남지 않으면 그 레벨 단어를 뺀다 (뜻은 원래대로 둠)")
    void excludeAllMeanings() {
        WordExposureRules rules = rules(
                new Rule(SEX, 1, null, ExposureAction.EXCLUDE_MEANING),
                new Rule(SEX, 2, null, ExposureAction.EXCLUDE_MEANING));

        CatalogWord word = rules.apply(SEX_B1);

        assertThat(word.excluded()).isTrue();
        assertThat(word.optionMeanings()).containsExactly("성별", "성관계");
    }

    @Test
    @DisplayName("뜻 제외한 뜻이 그 레벨 단어의 보기 뜻에 없으면 그대로")
    void excludeMeaningNotShown() {
        WordExposureRules rules = rules(new Rule(SEX, 3, null, ExposureAction.EXCLUDE_MEANING));

        assertThat(rules.apply(SEX_B1).optionMeanings()).containsExactly("성별", "성관계");
    }

    @Test
    @DisplayName("유지는 아무것도 하지 않는다")
    void keep() {
        WordExposureRules rules = rules(
                new Rule(LARK, null, Level.B2, ExposureAction.KEEP),
                new Rule(SEX, 2, null, ExposureAction.KEEP),
                new Rule(GOOK, null, null, ExposureAction.KEEP));

        assertThat(rules.apply(LARK_B2)).isEqualTo(rules().apply(LARK_B2));
        assertThat(rules.apply(SEX_B1)).isEqualTo(rules().apply(SEX_B1));
        assertThat(rules.apply(SEX_B1).optionMeanings()).containsExactly("성별", "성관계");
    }

    @Test
    @DisplayName("처리에 맞지 않는 대상은 만들 수 없다")
    void invalidTarget() {
        assertThatThrownBy(() -> new Rule(LARK, null, null, ExposureAction.EXCLUDE_LEVEL))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule(LARK, 1, null, ExposureAction.EXCLUDE_WORD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule(LARK, null, Level.B2, ExposureAction.EXCLUDE_MEANING))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule(LARK, 1, Level.B2, ExposureAction.KEEP))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Rule(LARK, null, Level.B, ExposureAction.EXCLUDE_LEVEL))
                .as("묶음 레벨").isInstanceOf(IllegalArgumentException.class);
    }

}

package org.ruscayn.vocarunner.word.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CatalogWord : 출제 목록 단어")
class CatalogWordTest {

    private static CatalogWord catalogWord(String headword, String headwordSub, Level level, List<String> meanings) {
        return new CatalogWord(1, 1, headword, headwordSub, level, meanings, false);
    }

    @Test
    @DisplayName("세부 레벨만 만들 수 있다")
    void detailLevelOnly() {
        assertThatThrownBy(() -> catalogWord("bus", null, Level.A, List.of("버스")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalogWord("bus", null, null, List.of("버스")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "[{0}]")
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("표제어가 비면 만들 수 없다")
    void blankHeadword(String headword) {
        assertThatThrownBy(() -> catalogWord(headword, null, Level.A1, List.of("버스")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("보기 뜻이 없으면 만들 수 없다")
    void noOptionMeanings() {
        assertThatThrownBy(() -> catalogWord("bus", null, Level.A1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalogWord("bus", null, Level.A1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("표제어 보조가 빈 글이면 없는 것으로 둔다")
    void blankHeadwordSub() {
        assertThat(catalogWord("bus", "  ", Level.A1, List.of("버스")).headwordSub()).isNull();
    }

    @Test
    @DisplayName("보기 글은 보기 뜻을 쉼표로 잇고, 보기 뜻은 만들 때 복사해 둔다")
    void optionText() {
        List<String> meanings = new ArrayList<>(List.of("버스", "승합차"));

        CatalogWord word = catalogWord("bus", null, Level.A1, meanings);
        meanings.add("나중에 더한 뜻");

        assertThat(word.optionText()).isEqualTo("버스, 승합차");
    }

    @Test
    @DisplayName("시험 제외가 아니면 정답 가능")
    void answerable() {
        assertThat(new CatalogWord(1, 1, "bus", null, Level.A1, List.of("버스"), false).isAnswerable()).isTrue();
        assertThat(new CatalogWord(1, 1, "bus", null, Level.A1, List.of("버스"), true).isAnswerable()).isFalse();
    }

}

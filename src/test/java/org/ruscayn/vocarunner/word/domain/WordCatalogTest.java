package org.ruscayn.vocarunner.word.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.excluded;
import static org.ruscayn.vocarunner.support.TestWords.word;
import static org.ruscayn.vocarunner.support.TestWords.words;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;

@DisplayName("WordCatalog : 단어 출제 목록 (정답 후보 · 찾기 · 혼동 단어 묶음)")
class WordCatalogTest {

    @Test
    @DisplayName("정답 후보 수는 시험 제외를 빼고 센다")
    void answerPoolWithoutExcluded() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 10));
        for (long id = 101; id <= 130; id++) {
            catalog.add(excluded(id, id, Level.A1));
        }

        WordCatalog wordCatalog = new WordCatalog(catalog, List.of());

        assertThat(wordCatalog.countByLevel(Level.A1)).isEqualTo(10);
        assertThat(wordCatalog.findAnswerPool(Level.A1)).noneMatch(CatalogWord::excluded);
        assertThat(wordCatalog.getWordCount()).isEqualTo(40);
        assertThat(wordCatalog.getExcludedCount()).isEqualTo(30);
    }

    @Test
    @DisplayName("묶음 레벨 정답 후보 : 두 레벨에 모두 있는 단어는 낮은 레벨 행만 남긴다")
    void groupLevelKeepsLowerLevel() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 3));
        catalog.add(word(501, 1, Level.A2, "A2뜻1"));

        WordCatalog wordCatalog = new WordCatalog(catalog, List.of());

        assertThat(wordCatalog.findAnswerPool(Level.A)).extracting(CatalogWord::levelWordId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("묶음 레벨 정답 후보 : 시험 제외를 먼저 빼서, 낮은 레벨이 제외면 높은 레벨 행을 남긴다")
    void groupLevelExcludesFirst() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.A1, 1, 30));
        catalog.addAll(words(Level.A2, 101, 30));
        catalog.add(excluded(900, 900, Level.A1, "A1뜻900"));
        catalog.add(word(901, 900, Level.A2, "A2뜻900"));

        WordCatalog wordCatalog = new WordCatalog(catalog, List.of());

        assertThat(wordCatalog.countByLevel(Level.A)).isEqualTo(61);
        assertThat(wordCatalog.findAnswerPool(Level.A)).extracting(CatalogWord::levelWordId)
                .contains(901L)
                .doesNotContain(900L);
    }

    @Test
    @DisplayName("묶음 레벨 정답 후보 수는 같은 단어를 한 번만 센다")
    void groupLevelCountDedupe() {
        List<CatalogWord> catalog = new ArrayList<>(words(Level.B1, 1, 10));
        catalog.addAll(words(Level.B2, 101, 5));
        for (long wordId = 1; wordId <= 3; wordId++) {      // B1 단어 3개가 B2 에도 있음
            catalog.add(word(500 + wordId, wordId, Level.B2, "B2뜻" + wordId));
        }

        WordCatalog wordCatalog = new WordCatalog(catalog, List.of());

        assertThat(wordCatalog.countByLevel(Level.B1)).isEqualTo(10);
        assertThat(wordCatalog.countByLevel(Level.B2)).isEqualTo(8);
        assertThat(wordCatalog.countByLevel(Level.B)).isEqualTo(15);
    }

    @Test
    @DisplayName("레벨 단어 아이디로 찾을 때는 시험 제외 행도 찾는다 (이미 만든 시험이 가리킬 수 있음)")
    void getFindsExcluded() {
        WordCatalog wordCatalog = new WordCatalog(List.of(word(1, 1, Level.A1), excluded(2, 2, Level.A1)), List.of());

        assertThat(wordCatalog.get(2).excluded()).isTrue();
    }

    @Test
    @DisplayName("없는 레벨 단어 아이디면 ENTITY_NOT_FOUND")
    void getUnknown() {
        WordCatalog wordCatalog = new WordCatalog(words(Level.A1, 1, 3), List.of());

        assertThatThrownBy(() -> wordCatalog.get(99))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
    }

    @Test
    @DisplayName("레벨 단어 아이디가 겹치면 만들 수 없다")
    void duplicateLevelWordId() {
        List<CatalogWord> catalog = List.of(word(1, 1, Level.A1), word(1, 2, Level.A2));

        assertThatThrownBy(() -> new WordCatalog(catalog, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("혼동 단어 : 양방향, 여러 묶음은 합침, 묶음 없는 단어는 빈 집합")
    void confusableWordIds() {
        WordCatalog wordCatalog = new WordCatalog(words(Level.A1, 1, 10),
                List.of(List.of(1L, 2L), List.of(1L, 3L), List.of(4L, 5L, 6L)));

        assertThat(wordCatalog.findConfusableWordIds(1L)).containsExactlyInAnyOrder(2L, 3L);
        assertThat(wordCatalog.findConfusableWordIds(2L)).containsExactly(1L);
        assertThat(wordCatalog.findConfusableWordIds(3L)).containsExactly(1L);
        assertThat(wordCatalog.findConfusableWordIds(5L)).containsExactlyInAnyOrder(4L, 6L);
        assertThat(wordCatalog.findConfusableWordIds(7L)).isEmpty();
        assertThat(wordCatalog.getConfusableGroups().getGroupCount()).isEqualTo(3);
        assertThat(wordCatalog.getConfusableGroups().getWordCount()).isEqualTo(6);
    }

}

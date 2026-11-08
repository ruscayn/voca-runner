package org.ruscayn.vocarunner.word.adapter.out.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.support.TestWords;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

@DisplayName("WordCatalogMemoryAdapter : 단어 출제 목록 두기")
class WordCatalogMemoryAdapterTest {

    private final WordCatalogMemoryAdapter adapter = new WordCatalogMemoryAdapter();

    @Test
    @DisplayName("적재 전에 꺼내면 예외")
    void getBeforeReplace() {
        assertThatThrownBy(adapter::get).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("교체하면 그다음부터 새 목록을 꺼낸다")
    void replace() {
        WordCatalog first = new WordCatalog(TestWords.words(Level.A1, 1, 2), List.of());
        WordCatalog second = new WordCatalog(TestWords.words(Level.A1, 1, 3), List.of());

        adapter.replace(first);
        adapter.replace(second);

        assertThat(adapter.get()).isSameAs(second);
    }

}

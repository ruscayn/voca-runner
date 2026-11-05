package org.ruscayn.vocarunner.word.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Level : 레벨 코드")
class LevelTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "A1, Basic", "A2, Basic", "A, Basic",
            "B1, Independent", "B2, Independent", "B, Independent",
            "C1, Proficient", "C2, Proficient", "C, Proficient"
    })
    @DisplayName("CEFR 묶음 이름")
    void groupName(Level level, String expected) {
        assertThat(level.getGroupName()).isEqualTo(expected);
    }

    @Test
    @DisplayName("묶음 레벨은 소속 세부 레벨 2개, 세부 레벨은 자기 자신")
    void detailLevels() {
        assertThat(Level.A.getDetailLevels()).containsExactly(Level.A1, Level.A2);
        assertThat(Level.B.getDetailLevels()).containsExactly(Level.B1, Level.B2);
        assertThat(Level.C.getDetailLevels()).containsExactly(Level.C1, Level.C2);
        assertThat(Level.B2.getDetailLevels()).containsExactly(Level.B2);
    }

    @Test
    @DisplayName("묶음 여부와 높낮이 비교")
    void groupAndOrder() {
        assertThat(Level.A.isGroup()).isTrue();
        assertThat(Level.A1.isGroup()).isFalse();
        assertThat(Level.A1.isLowerThan(Level.A2)).isTrue();
        assertThat(Level.C2.isLowerThan(Level.B1)).isFalse();
    }

}

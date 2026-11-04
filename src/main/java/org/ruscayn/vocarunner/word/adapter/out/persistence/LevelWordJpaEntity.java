package org.ruscayn.vocarunner.word.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ruscayn.vocarunner.word.domain.Level;

@Getter
@Entity
@Table(name = "level_word")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelWordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "level_word_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_code", nullable = false, length = 2)
    private Level level;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private WordJpaEntity word;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meaning1_id", nullable = false)
    private WordMeaningJpaEntity meaning1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning2_id")
    private WordMeaningJpaEntity meaning2;

    public List<WordMeaningJpaEntity> optionMeanings() {
        List<WordMeaningJpaEntity> result = new ArrayList<>(2);
        result.add(meaning1);
        if (meaning2 != null) {
            result.add(meaning2);
        }
        return Collections.unmodifiableList(result);
    }

    public String headword() {
        return word.getHeadword();
    }

}

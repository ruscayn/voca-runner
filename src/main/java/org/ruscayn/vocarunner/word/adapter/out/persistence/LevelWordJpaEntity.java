package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.ruscayn.vocarunner.common.adapter.out.persistence.BaseEntity;
import org.ruscayn.vocarunner.word.domain.Level;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 레벨 단어 (level_word) = 한 단어의 한 세부 레벨. 한 레벨에 같은 단어는 1번.
 * 시험 보기에는 meaning1, meaning2(없을 수 있음) 를 보여준다.
 * 원본에 있는 레벨마다 레벨 단어가 있고, 시험에 낼지는 단어 노출({@link WordExposureJpaEntity})로 정한다.
 */
@Getter
@Entity
@Table(name = "level_word")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelWordJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "level_word_id")
    private Long id;

    /** 세부 레벨 A1~C2 */
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

    /** 보기에 보여줄 뜻 목록 (1~2개) */
    public List<WordMeaningJpaEntity> optionMeanings() {
        List<WordMeaningJpaEntity> result = new ArrayList<>(2);
        result.add(meaning1);
        if (meaning2 != null) {
            result.add(meaning2);
        }
        return Collections.unmodifiableList(result);
    }

    /** 보기에 보여줄 한국어 뜻 텍스트 (보기 간 중복 검사용) */
    public List<String> optionMeaningTexts() {
        return optionMeanings().stream().map(WordMeaningJpaEntity::getMeaningKo).toList();
    }

    public String headword() {
        return word.getHeadword();
    }

}

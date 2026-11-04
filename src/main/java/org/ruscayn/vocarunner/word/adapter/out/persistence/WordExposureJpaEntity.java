package org.ruscayn.vocarunner.word.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ruscayn.vocarunner.word.domain.ExposureAction;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;

/** 뜻은 meaning_id 가 아니라 단어 안 순번(seq)으로 가리킨다 (meaning_id 는 단어 데이터를 다시 적재할 때마다 새로 매겨진다) */
@Entity
@Table(name = "word_exposure")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordExposureJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_exposure_id")
    private Long id;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "meaning_seq")
    private Integer meaningSeq;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_code", length = 2)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ExposureAction action;

    public WordExposureRules.Rule toRule() {
        return new WordExposureRules.Rule(wordId, meaningSeq, level, action);
    }

}

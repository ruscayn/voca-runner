package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.ruscayn.vocarunner.common.adapter.out.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 단어의 한국어 뜻 (word_meaning). 단어당 최대 5개, seq 1~5.
 */
@Getter
@Entity
@Table(name = "word_meaning")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordMeaning extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meaning_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "seq", nullable = false)
    private Integer seq;

    /** 품사 (noun, verb ...) */
    @Column(name = "pos", nullable = false, length = 20)
    private String pos;

    @Column(name = "meaning_ko", nullable = false, length = 50)
    private String meaningKo;

}

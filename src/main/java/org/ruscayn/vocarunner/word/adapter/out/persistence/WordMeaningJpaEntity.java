package org.ruscayn.vocarunner.word.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "word_meaning")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordMeaningJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meaning_id")
    private Long id;

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "meaning_ko", nullable = false, length = 50)
    private String meaningKo;

}

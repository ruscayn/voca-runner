package org.ruscayn.vocarunner.word.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ruscayn.vocarunner.common.adapter.out.persistence.BaseEntity;

@Getter
@Entity
@Table(name = "word")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id")
    private Long id;

    /** 대소문자를 구분한다 (May / may 는 다른 단어) */
    @Column(name = "headword", nullable = false, unique = true, length = 50)
    private String headword;

    @Column(name = "loanword", length = 50)
    private String loanword;

    @Column(name = "headword_sub", length = 100)
    private String headwordSub;

    @OneToMany(mappedBy = "word")
    @OrderBy("seq ASC")
    private List<WordMeaningJpaEntity> meanings = new ArrayList<>();

}

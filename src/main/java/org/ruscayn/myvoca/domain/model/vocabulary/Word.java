package org.ruscayn.myvoca.domain.model.vocabulary;

import org.ruscayn.myvoca.shared.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 표제어 (word). 데이터는 CSV 로 적재하며 앱에서는 조회만 한다.
 */
@Getter
@Entity
@Table(name = "word")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Word extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id")
    private Long id;

    /** 대소문자 구분 (May / may) */
    @Column(name = "headword", nullable = false, unique = true, length = 50)
    private String headword;

    @OneToMany(mappedBy = "word")
    @OrderBy("seq ASC")
    private List<WordMeaning> meanings = new ArrayList<>();

}

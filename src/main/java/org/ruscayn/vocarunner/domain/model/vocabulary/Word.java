package org.ruscayn.vocarunner.domain.model.vocabulary;

import org.ruscayn.vocarunner.shared.common.BaseEntity;
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

    /** 외래어 표기 (예: elevator → 엘리베이터). 화면에는 아직 쓰지 않음. 없으면 null */
    @Column(name = "loanword", length = 50)
    private String loanword;

    /** 시험 화면에 표제어 아래 줄바꿈으로 함께 표시할 형태 (예: for the sake of → for one's sake). 없으면 null */
    @Column(name = "headword_sub", length = 100)
    private String headwordSub;

    @OneToMany(mappedBy = "word")
    @OrderBy("seq ASC")
    private List<WordMeaning> meanings = new ArrayList<>();

}

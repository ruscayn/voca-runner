package org.ruscayn.vocarunner.domain.model.vocabulary;

import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.shared.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 레벨별 출제 단어 (level_word). 한 레벨에 같은 단어는 1번.
 * 시험 보기에는 meaning1, meaning2(없을 수 있음) 를 보여준다.
 * test_excluded 인 행은 시험에 쓰지 않는다 (정답 문제·오답 보기 모두).
 */
@Getter
@Entity
@Table(name = "level_word")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LevelWord extends BaseEntity {

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
    private Word word;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meaning1_id", nullable = false)
    private WordMeaning meaning1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meaning2_id")
    private WordMeaning meaning2;

    /**
     * 이 레벨에서 시험 제외 (예: bus 전체, inch A2 만).
     * true 면 정답 문제로도, 오답 보기로도 내지 않는다.
     */
    @Column(name = "test_excluded", nullable = false)
    private boolean testExcluded;

    /** 보기에 보여줄 뜻 목록 (1~2개) */
    public List<WordMeaning> displayMeanings() {
        List<WordMeaning> result = new ArrayList<>(2);
        result.add(meaning1);
        if (meaning2 != null) {
            result.add(meaning2);
        }
        return Collections.unmodifiableList(result);
    }

    /** 보기에 보여줄 한국어 뜻 텍스트 (보기 간 중복 검사용) */
    public List<String> displayMeaningTexts() {
        return displayMeanings().stream().map(WordMeaning::getMeaningKo).toList();
    }

    public String headword() {
        return word.getHeadword();
    }

}

package org.ruscayn.vocarunner.word.adapter.out.persistence;

import org.ruscayn.vocarunner.common.adapter.out.persistence.BaseEntity;
import org.ruscayn.vocarunner.word.domain.ExposureAction;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * 단어 노출 - 지금 판정 (word_exposure). 데이터는 CSV 로 적재하며 앱에서는 조회만 한다.
 *
 * <p>대상은 처리에 따라 단어 전체 / 뜻(단어 안 뜻 순번) / 레벨 단어 중 하나 (DB CHECK 제약으로 보장).
 * 뜻은 meaning_id 가 아니라 단어 안 순번(seq)으로 가리킨다 (meaning_id 는 재적재마다 다시 매겨짐).
 * 유형·이력 표(word_exposure_type, word_exposure_history)는 앱이 읽지 않아 Entity 를 두지 않는다.
 */
@Getter
@Entity
@Table(name = "word_exposure")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordExposure extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_exposure_id")
    private Long id;

    @Column(name = "word_id", nullable = false)
    private Long wordId;

    /** 뜻 제외·유지의 대상 뜻 순번 (word_meaning.seq). 그 밖에는 null */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "meaning_seq")
    private Integer meaningSeq;

    /** 레벨 제외·유지의 대상 레벨. 그 밖에는 null */
    @Enumerated(EnumType.STRING)
    @Column(name = "level_code", length = 2)
    private Level level;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private ExposureAction action;

    /** 이슈 유형 코드 (word_exposure_type) */
    @Column(name = "issue_type", nullable = false, length = 30)
    private String issueType;

    /** 강도 HIGH·MEDIUM·LOW (민감 단어만) */
    @Column(name = "severity", length = 10)
    private String severity;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "decided_at", nullable = false)
    private LocalDate decidedDate;

    @Column(name = "decided_by", nullable = false, length = 30)
    private String decidedBy;

    /** 캐시 적용 규칙으로 변환 */
    public WordExposureRules.Rule toRule() {
        return new WordExposureRules.Rule(wordId, meaningSeq, level, action);
    }

}

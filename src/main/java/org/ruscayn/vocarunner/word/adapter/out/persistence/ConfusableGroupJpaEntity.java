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
import org.ruscayn.vocarunner.common.adapter.out.persistence.BaseEntity;

/**
 * 헷갈리는 단어 묶음 (confusable_group).
 *
 * <p>같은 묶음의 단어끼리는 한 문제의 보기로 함께 내지 않는다 (예: {sir, teacher} "선생님").
 * 묶음은 레벨과 무관하게 단어 단위이고, 한 단어가 여러 묶음에 들어갈 수 있다.
 * 묶음에 속한 단어는 {@link ConfusableGroupWordJpaEntity}.
 */
@Getter
@Entity
@Table(name = "confusable_group")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfusableGroupJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "confusable_group_id")
    private Long id;

    /** 헷갈리는 이유 (예: 선생님, 완전히) */
    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

}

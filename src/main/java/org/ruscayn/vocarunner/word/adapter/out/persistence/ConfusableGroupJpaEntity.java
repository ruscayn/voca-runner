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

@Getter
@Entity
@Table(name = "confusable_group")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfusableGroupJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "confusable_group_id")
    private Long id;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

}

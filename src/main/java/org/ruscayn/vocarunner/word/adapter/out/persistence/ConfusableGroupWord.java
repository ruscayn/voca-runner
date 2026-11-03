package org.ruscayn.vocarunner.word.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;

/**
 * 헷갈리는 단어 묶음에 속한 단어 (confusable_group_word). PK = (묶음, 단어).
 * 행 자체를 수정하는 일은 없어 updated_at 은 두지 않는다 (넣고 빼기만 함).
 */
@Getter
@Entity
@Table(name = "confusable_group_word")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfusableGroupWord {

    @EmbeddedId
    private Id id;

    @MapsId("groupId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confusable_group_id", nullable = false)
    private ConfusableGroup group;

    @MapsId("wordId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id", nullable = false)
    private Word word;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** 복합 키 (confusable_group_id, word_id) */
    @Embeddable
    public record Id(
            @Column(name = "confusable_group_id") Long groupId,
            @Column(name = "word_id") Long wordId) implements Serializable {
    }

}

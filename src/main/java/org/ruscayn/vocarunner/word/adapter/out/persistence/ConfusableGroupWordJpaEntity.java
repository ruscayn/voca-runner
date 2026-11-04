package org.ruscayn.vocarunner.word.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "confusable_group_word")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfusableGroupWordJpaEntity {

    @EmbeddedId
    private Id id;

    @Embeddable
    public record Id(
            @Column(name = "confusable_group_id") Long groupId,
            @Column(name = "word_id") Long wordId) implements Serializable {
    }

}

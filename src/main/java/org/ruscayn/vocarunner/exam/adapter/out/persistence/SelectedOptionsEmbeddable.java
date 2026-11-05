package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.ruscayn.vocarunner.exam.domain.SelectedOptions;

@Embeddable
public record SelectedOptionsEmbeddable(
        @Column(name = "selected_option_no", nullable = false) byte[] selectedOptionNo
) {

    static SelectedOptionsEmbeddable from(SelectedOptions selectedOptions) {
        return new SelectedOptionsEmbeddable(selectedOptions.toSelectedOptionNoArray());
    }

    SelectedOptions toDomain() {
        return SelectedOptions.of(selectedOptionNo);
    }

}

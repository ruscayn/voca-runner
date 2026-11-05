package org.ruscayn.vocarunner.exam.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.ruscayn.vocarunner.exam.domain.ExamPaper;
import org.ruscayn.vocarunner.exam.domain.SelectedOptions;

@Embeddable
public record ExamPaperEmbeddable(
        @JdbcTypeCode(SqlTypes.ARRAY) @Column(name = "option_ids", nullable = false) int[] optionIds,
        @Column(name = "selected_option_no", nullable = false) byte[] selectedOptionNo
) {

    static ExamPaperEmbeddable from(ExamPaper paper) {
        return new ExamPaperEmbeddable(paper.toOptionIdArray(), paper.getSelectedOptions().toSelectedOptionNoArray());
    }

    ExamPaper toDomain() {
        return ExamPaper.of(optionIds, SelectedOptions.of(selectedOptionNo));
    }

}

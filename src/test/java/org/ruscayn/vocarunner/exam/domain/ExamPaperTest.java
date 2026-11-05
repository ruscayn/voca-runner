package org.ruscayn.vocarunner.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.answerOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.incorrectOptionNo;
import static org.ruscayn.vocarunner.support.TestWords.questions;
import static org.ruscayn.vocarunner.support.TestWords.selected;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExamPaper · AnswerKey : 시험지와 정답지, 정답지로 시험지 채점")
class ExamPaperTest {

    @Test
    @DisplayName("시험지는 문항마다 보기 4개를 차례대로 담고, 처음에는 선택한 보기가 없다")
    void from() {
        List<Question> questions = questions(3);

        ExamPaper paper = ExamPaper.from(questions);

        assertThat(paper.getQuestionCount()).isEqualTo(3);
        assertThat(paper.optionIds(2))
                .containsExactlyElementsOf(questions.get(1).options().stream().map(o -> o.levelWordId()).toList());
        assertThat(paper.selectedOptionId(1)).isNull();
    }

    @Test
    @DisplayName("선택한 보기를 기록하면 새 시험지가 되고, 선택한 보기의 아이디를 돌려준다")
    void record() {
        ExamPaper paper = ExamPaper.from(questions(3));

        ExamPaper marked = paper.record(selected(Map.of(2, incorrectOptionNo(2))));

        assertThat(marked.selectedOptionId(2)).isEqualTo(marked.optionIds(2).get(incorrectOptionNo(2) - 1));
        assertThat(paper.selectedOptionId(2)).isNull();
    }

    @Test
    @DisplayName("정답지는 선택한 보기 번호가 정답 번호와 같은 문항만 맞힌 것으로 센다 (시간 초과 · 미응답은 틀림)")
    void grade() {
        List<Question> questions = questions(4);
        AnswerKey answerKey = AnswerKey.from(questions);
        Map<Integer, Integer> marks = new HashMap<>(Map.of(1, answerOptionNo(1), 2, incorrectOptionNo(2)));
        marks.put(3, null);

        ExamPaper paper = ExamPaper.from(questions).record(selected(marks));

        assertThat(answerKey.countCorrect(paper)).isEqualTo(1);
        assertThat(answerKey.isCorrect(1, paper)).isTrue();
        assertThat(answerKey.findWrongQuestionNos(paper)).containsExactly(2, 3, 4);
    }

    @Test
    @DisplayName("저장된 값으로 만들 때 보기 수가 문항 수 × 4 가 아니면 거절한다")
    void ofWrongLength() {
        assertThatThrownBy(() -> ExamPaper.of(new int[5], SelectedOptions.of(new byte[2])))
                .isInstanceOf(IllegalArgumentException.class);
    }

}

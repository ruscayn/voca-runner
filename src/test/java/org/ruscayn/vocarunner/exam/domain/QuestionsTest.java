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

@DisplayName("Questions : 문항 목록 (보기 4개 + 정답 번호), 선택한 보기 채점")
class QuestionsTest {

    @Test
    @DisplayName("문항 목록은 문항마다 보기 4개를 차례대로 담고, 제시어는 정답 보기의 단어다")
    void from() {
        List<Question> questions = questions(3);

        Questions list = Questions.from(questions);

        assertThat(list.getQuestionCount()).isEqualTo(3);
        assertThat(list.optionIds(2))
                .containsExactlyElementsOf(questions.get(1).options().stream().map(o -> o.levelWordId()).toList());
        assertThat(list.answerOptionId(2)).isEqualTo(questions.get(1).answer().levelWordId());
    }

    @Test
    @DisplayName("선택한 보기 번호가 정답 번호와 같은 문항만 맞힌 것으로 센다 (시간 초과 · 미응답은 틀림)")
    void grade() {
        Map<Integer, Integer> marks = new HashMap<>(Map.of(1, answerOptionNo(1), 2, incorrectOptionNo(2)));
        marks.put(3, null);
        Questions questions = Questions.from(questions(4));

        SelectedOptions selected = SelectedOptions.record(selected(marks), 4);

        assertThat(questions.countCorrect(selected)).isEqualTo(1);
        assertThat(questions.findWrongAnswers(selected)).extracting(WrongAnswer::no).containsExactly(2, 3, 4);
    }

    @Test
    @DisplayName("틀린 문항마다 정답 · 선택한 보기의 아이디를 주고, 선택하지 않았으면 시간 초과다")
    void wrongAnswers() {
        Questions questions = Questions.from(questions(3));

        SelectedOptions selected = SelectedOptions.record(selected(Map.of(2, incorrectOptionNo(2))), 3);

        List<WrongAnswer> wrongAnswers = questions.findWrongAnswers(selected);
        assertThat(wrongAnswers).extracting(WrongAnswer::no).containsExactly(1, 2, 3);
        WrongAnswer incorrect = wrongAnswers.get(1);
        assertThat(incorrect.answerOptionId()).isEqualTo(questions.answerOptionId(2));
        assertThat(incorrect.selectedOptionId()).isEqualTo(questions.optionIds(2).get(incorrectOptionNo(2) - 1));
        assertThat(incorrect.isTimeout()).isFalse();
        assertThat(wrongAnswers.get(0).selectedOptionId()).isNull();
        assertThat(wrongAnswers.get(0).isTimeout()).isTrue();
    }

    @Test
    @DisplayName("없는 문제 번호를 조회하면 예외")
    void questionNoRange() {
        Questions questions = Questions.from(questions(3));

        assertThatThrownBy(() -> questions.answerOptionId(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> questions.optionIds(4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("저장된 값으로 만들 때 보기 수가 문항 수 × 4 가 아니면 거절한다")
    void ofWrongLength() {
        assertThatThrownBy(() -> Questions.of(new int[5], new byte[2]))
                .isInstanceOf(IllegalArgumentException.class);
    }

}

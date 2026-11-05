package org.ruscayn.vocarunner.exam.domain;

import java.util.List;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/** 문항 n(1부터)의 정답 보기 번호(1~4) = answerOptionNo[n-1]. 정답지로 시험지를 채점한다 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnswerKey {

    private final byte[] answerOptionNo;

    static AnswerKey from(List<Question> questions) {
        byte[] answerOptionNo = new byte[questions.size()];
        for (int q = 0; q < questions.size(); q++) {
            answerOptionNo[q] = (byte) questions.get(q).answerOptionNo();
        }
        return new AnswerKey(answerOptionNo);
    }

    public static AnswerKey of(byte[] answerOptionNo) {
        return new AnswerKey(answerOptionNo.clone());
    }

    int getQuestionCount() {
        return answerOptionNo.length;
    }

    int answerOptionNo(int no) {
        return answerOptionNo[Question.index(no, getQuestionCount())];
    }

    int countCorrect(ExamPaper paper) {
        return (int) IntStream.rangeClosed(1, getQuestionCount()).filter(no -> isCorrect(no, paper)).count();
    }

    boolean isCorrect(int no, ExamPaper paper) {
        return paper.isSelected(no, answerOptionNo(no));
    }

    List<Integer> findWrongQuestionNos(ExamPaper paper) {
        return IntStream.rangeClosed(1, getQuestionCount())
                .filter(no -> !isCorrect(no, paper))
                .boxed()
                .toList();
    }

    public byte[] toAnswerOptionNoArray() {
        return answerOptionNo.clone();
    }

}

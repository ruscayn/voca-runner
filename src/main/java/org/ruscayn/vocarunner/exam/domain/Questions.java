package org.ruscayn.vocarunner.exam.domain;

import java.util.List;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/** 문항 n(1부터)의 보기 i(1~4) = optionIds[(n-1)*4 + (i-1)] (레벨 단어 아이디), 정답 보기 번호 = answerOptionNo[n-1] */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Questions {

    private static final int OPTION_COUNT = Question.OPTION_COUNT;

    private final int[] optionIds;
    private final byte[] answerOptionNo;

    static Questions from(List<Question> questions) {
        return new Questions(toOptionIds(questions), toAnswerOptionNo(questions));
    }

    private static int[] toOptionIds(List<Question> questions) {
        return questions.stream()
                .flatMap(question -> question.options().stream())
                .mapToInt(option -> Math.toIntExact(option.levelWordId()))
                .toArray();
    }

    private static byte[] toAnswerOptionNo(List<Question> questions) {
        byte[] answerOptionNo = new byte[questions.size()];
        for (int q = 0; q < questions.size(); q++) {
            answerOptionNo[q] = (byte) questions.get(q).answerOptionNo();
        }
        return answerOptionNo;
    }

    public static Questions of(int[] optionIds, byte[] answerOptionNo) {
        if (optionIds.length != answerOptionNo.length * OPTION_COUNT) {
            throw new IllegalArgumentException("보기 수가 문항 수 × " + OPTION_COUNT + " 가 아닙니다: " + optionIds.length);
        }
        return new Questions(optionIds.clone(), answerOptionNo.clone());
    }

    public int getQuestionCount() {
        return answerOptionNo.length;
    }

    public List<Long> optionIds(int no) {
        return IntStream.rangeClosed(1, OPTION_COUNT)
                .mapToObj(optionNo -> optionId(no, optionNo))
                .toList();
    }

    private long optionId(int no, int optionNo) {
        return optionIds[Question.index(no, getQuestionCount()) * OPTION_COUNT + optionNo - 1];
    }

    /** 제시어 단어 = 정답 보기의 단어 */
    public long answerOptionId(int no) {
        return optionId(no, answerOptionNo(no));
    }

    private int answerOptionNo(int no) {
        return answerOptionNo[Question.index(no, getQuestionCount())];
    }

    int countCorrect(SelectedOptions selected) {
        return (int) IntStream.rangeClosed(1, getQuestionCount()).filter(no -> isCorrect(no, selected)).count();
    }

    private boolean isCorrect(int no, SelectedOptions selected) {
        return selected.isSelected(no) && selected.optionNo(no) == answerOptionNo(no);
    }

    List<Integer> findWrongQuestionNos(SelectedOptions selected) {
        return IntStream.rangeClosed(1, getQuestionCount())
                .filter(no -> !isCorrect(no, selected))
                .boxed()
                .toList();
    }

    Long selectedOptionId(int no, SelectedOptions selected) {
        if (!selected.isSelected(no)) {
            return null;
        }
        return optionId(no, selected.optionNo(no));
    }

    public int[] toOptionIdArray() {
        return optionIds.clone();
    }

    public byte[] toAnswerOptionNoArray() {
        return answerOptionNo.clone();
    }

}

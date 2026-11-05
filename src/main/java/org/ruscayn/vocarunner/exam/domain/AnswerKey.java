package org.ruscayn.vocarunner.exam.domain;

import java.util.List;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/** 문항 n(1부터)의 보기 i(1~4) = optionIds[(n-1)*4 + (i-1)] (레벨 단어 아이디), 정답 보기 번호 = answerOptionNo[n-1] */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnswerKey {

    private static final int OPTION_COUNT = Question.OPTION_COUNT;

    private final int[] optionIds;
    private final byte[] answerOptionNo;

    static AnswerKey from(List<Question> questions) {
        return new AnswerKey(toOptionIds(questions), toAnswerOptionNo(questions));
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

    public static AnswerKey of(int[] optionIds, byte[] answerOptionNo) {
        if (optionIds.length != answerOptionNo.length * OPTION_COUNT) {
            throw new IllegalArgumentException("보기 수가 문항 수 × " + OPTION_COUNT + " 가 아닙니다: " + optionIds.length);
        }
        return new AnswerKey(optionIds.clone(), answerOptionNo.clone());
    }

    int getQuestionCount() {
        return answerOptionNo.length;
    }

    long answerOptionId(int no) {
        return optionId(no, answerOptionNo[index(no)]);
    }

    private int index(int no) {
        return Question.index(no, getQuestionCount());
    }

    long optionId(int no, int optionNo) {
        return optionIds[index(no) * OPTION_COUNT + optionNo - 1];
    }

    List<Long> optionIds(int no) {
        return IntStream.rangeClosed(1, OPTION_COUNT)
                .mapToObj(optionNo -> optionId(no, optionNo))
                .toList();
    }

    boolean isAnswer(int no, int optionNo) {
        return answerOptionNo[index(no)] == optionNo;
    }

    public int[] toOptionIdArray() {
        return optionIds.clone();
    }

    public byte[] toAnswerOptionNoArray() {
        return answerOptionNo.clone();
    }

}

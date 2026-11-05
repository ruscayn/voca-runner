package org.ruscayn.vocarunner.exam.domain;

import java.util.List;
import java.util.stream.IntStream;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 문항 n(1부터)의 보기 i(1~4) = optionIds[(n-1)*4 + (i-1)] (레벨 단어 아이디) */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExamPaper {

    private static final int OPTION_COUNT = Question.OPTION_COUNT;

    private final int[] optionIds;
    @Getter
    private final SelectedOptions selectedOptions;

    static ExamPaper from(List<Question> questions) {
        return new ExamPaper(toOptionIds(questions), SelectedOptions.none(questions.size()));
    }

    private static int[] toOptionIds(List<Question> questions) {
        return questions.stream()
                .flatMap(question -> question.options().stream())
                .mapToInt(option -> Math.toIntExact(option.levelWordId()))
                .toArray();
    }

    public static ExamPaper of(int[] optionIds, SelectedOptions selectedOptions) {
        if (optionIds.length != selectedOptions.getQuestionCount() * OPTION_COUNT) {
            throw new IllegalArgumentException("보기 수가 문항 수 × " + OPTION_COUNT + " 가 아닙니다: " + optionIds.length);
        }
        return new ExamPaper(optionIds.clone(), selectedOptions);
    }

    ExamPaper record(List<SelectedOption> selected) {
        return new ExamPaper(optionIds, SelectedOptions.record(selected, getQuestionCount()));
    }

    public int getQuestionCount() {
        return selectedOptions.getQuestionCount();
    }

    long optionId(int no, int optionNo) {
        return optionIds[Question.index(no, getQuestionCount()) * OPTION_COUNT + optionNo - 1];
    }

    public List<Long> optionIds(int no) {
        return IntStream.rangeClosed(1, OPTION_COUNT)
                .mapToObj(optionNo -> optionId(no, optionNo))
                .toList();
    }

    public Long selectedOptionId(int no) {
        if (!selectedOptions.isSelected(no)) {
            return null;
        }
        return optionId(no, selectedOptions.optionNo(no));
    }

    boolean isSelected(int no, int optionNo) {
        return selectedOptions.isSelected(no) && selectedOptions.optionNo(no) == optionNo;
    }

    public int[] toOptionIdArray() {
        return optionIds.clone();
    }

}

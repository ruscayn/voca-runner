package org.ruscayn.vocarunner.exam.domain;

import java.util.List;
import java.util.stream.IntStream;
import org.ruscayn.vocarunner.word.domain.CatalogWord;

public record Question(CatalogWord answer, List<CatalogWord> options) {

    public static final int OPTION_COUNT = 4;

    public Question {
        if (options.size() != OPTION_COUNT) {
            throw new IllegalArgumentException("보기는 " + OPTION_COUNT + "개여야 합니다: " + options.size());
        }
        long answerCount = options.stream().filter(o -> o.levelWordId() == answer.levelWordId()).count();
        if (answerCount != 1) {
            throw new IllegalArgumentException("정답은 보기에 정확히 1번 있어야 합니다: " + answerCount);
        }
        options = List.copyOf(options);
    }

    static int index(int no, int questionCount) {
        if (no < 1 || no > questionCount) {
            throw new IllegalArgumentException("문제 번호 범위 밖: " + no);
        }
        return no - 1;
    }

    public int answerOptionNo() {
        return IntStream.range(0, options.size())
                .filter(i -> options.get(i).levelWordId() == answer.levelWordId())
                .findFirst()
                .orElseThrow(IllegalStateException::new) + 1;
    }

}

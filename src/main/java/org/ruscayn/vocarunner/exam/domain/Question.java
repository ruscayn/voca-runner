package org.ruscayn.vocarunner.exam.domain;

import java.util.List;
import org.ruscayn.vocarunner.word.domain.CatalogWord;

/**
 * 생성된 문제 1개. options 는 화면에 보여줄 순서(섞인 순서)이며 answer 를 정확히 1번 포함한다.
 */
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

    /** 정답 보기 번호 1~4 */
    public int answerOptionNo() {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).levelWordId() == answer.levelWordId()) {
                return i + 1;
            }
        }
        throw new IllegalStateException();
    }

}

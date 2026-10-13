package org.ruscayn.vocarunner.support;

import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.test.GeneratedQuestion;
import org.ruscayn.vocarunner.domain.model.vocabulary.CatalogWord;

import java.util.ArrayList;
import java.util.List;

/** 테스트용 단어·문제 생성 도구 */
public final class TestWords {

    private TestWords() {
    }

    /** 단어 1개. 뜻은 겹치지 않게 "뜻{wordId}" */
    public static CatalogWord word(long levelWordId, long wordId, Level level) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of("뜻" + wordId), false);
    }

    public static CatalogWord word(long levelWordId, long wordId, Level level, String... meanings) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of(meanings), false);
    }

    /** 시험 제외 단어 1개. 뜻은 "뜻{wordId}" */
    public static CatalogWord excluded(long levelWordId, long wordId, Level level) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of("뜻" + wordId), true);
    }

    /** 시험 제외 단어 1개 (뜻 지정) */
    public static CatalogWord excluded(long levelWordId, long wordId, Level level, String... meanings) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of(meanings), true);
    }

    /**
     * 같은 레벨 단어 count 개. levelWordId 와 wordId 는 startId 부터 1씩 증가
     */
    public static List<CatalogWord> words(Level level, long startId, int count) {
        List<CatalogWord> list = new ArrayList<>(count);
        for (long id = startId; id < startId + count; id++) {
            list.add(word(id, id, level));
        }
        return list;
    }

    /**
     * 문제 count 개. 문제 n 의 정답은 (n-1) % 4 + 1 번 보기에 둔다 (1,2,3,4,1,...)
     * 단어 id : 문제 n 의 보기 i = n * 10 + i
     */
    public static List<GeneratedQuestion> questions(int count) {
        List<GeneratedQuestion> list = new ArrayList<>(count);
        for (int n = 1; n <= count; n++) {
            List<CatalogWord> options = new ArrayList<>(GeneratedQuestion.OPTION_COUNT);
            for (int i = 1; i <= GeneratedQuestion.OPTION_COUNT; i++) {
                long id = n * 10L + i;
                options.add(word(id, id, Level.A1));
            }
            CatalogWord answer = options.get(correctOptionNo(n) - 1);
            list.add(new GeneratedQuestion(answer, options));
        }
        return list;
    }

    /** questions() 로 만든 문제 n 의 정답 보기 번호 */
    public static int correctOptionNo(int questionNo) {
        return (questionNo - 1) % GeneratedQuestion.OPTION_COUNT + 1;
    }

    /** questions() 로 만든 문제 n 의 오답 보기 번호 하나 */
    public static int wrongOptionNo(int questionNo) {
        return correctOptionNo(questionNo) % GeneratedQuestion.OPTION_COUNT + 1;
    }

}

package org.ruscayn.vocarunner.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.ruscayn.vocarunner.exam.domain.Question;
import org.ruscayn.vocarunner.exam.domain.SelectedOption;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.InMemoryWordCatalog;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

/** 테스트용 단어·문항 만들기 도구 */
public final class TestWords {

    private TestWords() {
    }

    /** 단어 1개. 뜻은 겹치지 않게 "뜻{wordId}" */
    public static CatalogWord word(long levelWordId, long wordId, Level level) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of("뜻" + wordId), false);
    }

    public static CatalogWord word(long levelWordId, long wordId, Level level, String... optionMeanings) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of(optionMeanings), false);
    }

    /** 시험 제외 단어 1개. 뜻은 "뜻{wordId}" */
    public static CatalogWord excluded(long levelWordId, long wordId, Level level) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of("뜻" + wordId), true);
    }

    /** 시험 제외 단어 1개 (뜻 지정) */
    public static CatalogWord excluded(long levelWordId, long wordId, Level level, String... optionMeanings) {
        return new CatalogWord(levelWordId, wordId, "word" + wordId, null, level, List.of(optionMeanings), true);
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
    public static List<Question> questions(int count) {
        List<Question> list = new ArrayList<>(count);
        for (int n = 1; n <= count; n++) {
            List<CatalogWord> options = new ArrayList<>(Question.OPTION_COUNT);
            for (int i = 1; i <= Question.OPTION_COUNT; i++) {
                long id = n * 10L + i;
                options.add(word(id, id, Level.A1));
            }
            CatalogWord answer = options.get(answerOptionNo(n) - 1);
            list.add(new Question(answer, options));
        }
        return list;
    }

    /** 문제들의 보기 단어로 만든 출제 목록 (결과에서 아이디를 글로 바꿀 때) */
    public static WordCatalog catalogOf(List<Question> questions) {
        return new InMemoryWordCatalog(questions.stream().flatMap(q -> q.options().stream()).toList(), List.of());
    }

    /** questions() 로 만든 문제 n 의 정답 보기 번호 */
    public static int answerOptionNo(int questionNo) {
        return (questionNo - 1) % Question.OPTION_COUNT + 1;
    }

    /** questions() 로 만든 문제 n 의 오답 보기 번호 하나 */
    public static int incorrectOptionNo(int questionNo) {
        return answerOptionNo(questionNo) % Question.OPTION_COUNT + 1;
    }

    /** 문항 번호 → 고른 보기 번호(null = 시간 초과)를 고른 보기 목록으로 */
    public static List<SelectedOption> selected(Map<Integer, Integer> optionNoByQuestionNo) {
        return optionNoByQuestionNo.entrySet().stream()
                .map(entry -> new SelectedOption(entry.getKey(), entry.getValue()))
                .toList();
    }

}

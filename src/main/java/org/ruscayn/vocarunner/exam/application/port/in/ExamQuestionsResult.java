package org.ruscayn.vocarunner.exam.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.Questions;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

/**
 * @param remainingSec 마감까지 남은 초. 기기 시계 차이를 피하려고 시각이 아닌 남은 시간으로 준다
 */
public record ExamQuestionsResult(
        UUID examId,
        String level,
        String groupName,
        String subject,
        int questionCount,
        int timePerQuestionSec,
        long allowedMinutes,
        long remainingSec,
        List<QuestionItem> questions
) {

    public static ExamQuestionsResult of(Exam exam, WordCatalog wordCatalog, Instant now) {
        return new ExamQuestionsResult(exam.getId(), exam.getLevel().name(), exam.getLevel().getGroupName(),
                exam.getSubject().name(), exam.getQuestionCount(), exam.getTimePerQuestionSec(),
                exam.getTime().allowedMinutes(), exam.getTime().remainingSec(now), questionItems(exam, wordCatalog));
    }

    private static List<QuestionItem> questionItems(Exam exam, WordCatalog wordCatalog) {
        return IntStream.rangeClosed(1, exam.getQuestionCount())
                .mapToObj(no -> QuestionItem.of(exam.getQuestions(), no, wordCatalog))
                .toList();
    }

    public record QuestionItem(int no, String prompt, String promptSub, List<Option> options) {

        static QuestionItem of(Questions questions, int no, WordCatalog wordCatalog) {
            CatalogWord answer = wordCatalog.get(questions.answerOptionId(no));
            return new QuestionItem(no, answer.headword(), answer.headwordSub(),
                    options(questions.optionIds(no), wordCatalog));
        }

        private static List<Option> options(List<Long> optionIds, WordCatalog wordCatalog) {
            return IntStream.range(0, optionIds.size())
                    .mapToObj(i -> new Option(i + 1, wordCatalog.get(optionIds.get(i)).optionText()))
                    .toList();
        }

    }

    public record Option(int no, String text) {

    }

}

package org.ruscayn.vocarunner.exam.application.port.in;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.exam.domain.GradingResult;
import org.ruscayn.vocarunner.exam.domain.WrongAnswer;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

public record ExamResult(
        UUID examId,
        String level,
        String groupName,
        int questionCount,
        int correctCount,
        int score,
        Instant retentionDeadline,
        long retentionMinutes,
        List<WrongItem> wrongItems
) {

    public static ExamResult of(Exam exam, WordCatalog wordCatalog, Duration resultRetention) {
        GradingResult gradingResult = exam.getGradingResult();
        return new ExamResult(exam.getId(), exam.getLevel().name(), exam.getLevel().getGroupName(),
                exam.getQuestionCount(), gradingResult.correctCount(), gradingResult.score(),
                exam.getTime().retentionDeadline(resultRetention), resultRetention.toMinutes(),
                wrongItems(exam, wordCatalog));
    }

    private static List<WrongItem> wrongItems(Exam exam, WordCatalog wordCatalog) {
        return exam.getWrongAnswers().stream()
                .map(wrongAnswer -> WrongItem.of(wrongAnswer, wordCatalog))
                .toList();
    }

    public String retentionText() {
        return retentionMinutes % 60 == 0 ? (retentionMinutes / 60) + "시간" : retentionMinutes + "분";
    }

    public long timeoutCount() {
        return wrongItems.stream().filter(WrongItem::timeout).count();
    }

    public long incorrectCount() {
        return wrongItems.size() - timeoutCount();
    }

    public record WrongItem(int no, String prompt, String promptSub, String selectedText, String answerText,
                            String headword, boolean timeout) {

        static WrongItem of(WrongAnswer wrongAnswer, WordCatalog wordCatalog) {
            CatalogWord answer = wordCatalog.get(wrongAnswer.answerOptionId());
            return new WrongItem(wrongAnswer.no(), answer.headword(), answer.headwordSub(),
                    selectedText(wrongAnswer, wordCatalog), answer.optionText(), answer.headword(),
                    wrongAnswer.isTimeout());
        }

        private static String selectedText(WrongAnswer wrongAnswer, WordCatalog wordCatalog) {
            if (wrongAnswer.isTimeout()) {
                return null;
            }
            return wordCatalog.get(wrongAnswer.selectedOptionId()).optionText();
        }

    }

}

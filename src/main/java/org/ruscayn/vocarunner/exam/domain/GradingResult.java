package org.ruscayn.vocarunner.exam.domain;

import java.time.Duration;
import java.time.Instant;

public record GradingResult(int correctCount, int score, Instant submittedAt) {

    static GradingResult of(int correctCount, int questionCount, Instant submittedAt) {
        return new GradingResult(correctCount, toScore(correctCount, questionCount), submittedAt);
    }

    /** 반올림이 아니라 버림 : 틀린 문제가 있으면 100점이 되지 않게 (반올림이면 997문항 중 995개를 맞혀도 100점) */
    private static int toScore(int correctCount, int questionCount) {
        return correctCount * 100 / questionCount;
    }

    public Instant retentionDeadline(Duration resultRetention) {
        return submittedAt.plus(resultRetention);
    }

}

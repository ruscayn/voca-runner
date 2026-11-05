package org.ruscayn.vocarunner.exam.domain;

import java.time.Duration;
import java.time.Instant;

/** 제출 시각은 제출 전에는 없다 (null) */
public record ExamTime(Instant startedAt, Instant deadline, Instant submittedAt) {

    static ExamTime start(Instant now, int questionCount, int timePerQuestionSec, Duration grace) {
        return new ExamTime(now, now.plus(calculateAllowedTime(questionCount, timePerQuestionSec, grace)), null);
    }

    /**
     * (문항 수 × 문항당 시간 + 여유 시간)을 분 단위로 올림. 안내 문구를 "45분"처럼 분 단위로 보여 주므로 마감도 같은 값으로 맞춘다.
     * 예: 500문항 × 5초 + 1분 = 42분 40초 → 43분
     */
    static Duration calculateAllowedTime(int questionCount, int timePerQuestionSec, Duration grace) {
        long seconds = Duration.ofSeconds((long) questionCount * timePerQuestionSec).plus(grace).toSeconds();
        return Duration.ofMinutes(Math.ceilDiv(seconds, 60));
    }

    boolean isPastDeadline(Instant now) {
        return now.isAfter(deadline);
    }

    public long remainingSec(Instant now) {
        return Math.max(0, Duration.between(now, deadline).toSeconds());
    }

    public long allowedMinutes() {
        return Duration.between(startedAt, deadline).toMinutes();
    }

    ExamTime submit(Instant now) {
        return new ExamTime(startedAt, deadline, now);
    }

    public Instant retentionDeadline(Duration resultRetention) {
        return submittedAt.plus(resultRetention);
    }

}

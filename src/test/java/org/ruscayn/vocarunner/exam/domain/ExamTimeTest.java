package org.ruscayn.vocarunner.exam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("ExamTime : 시험 시간 (시작 시각 · 마감)")
class ExamTimeTest {

    private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");

    @ParameterizedTest(name = "{0}문항 × {1}초 + {2}초 → {3}분")
    @CsvSource({
            "500, 5, 180, 45",   // 41분 40초 + 3분 = 44분 40초 → 45분
            "5,   5, 180, 4",    // 25초 + 3분 = 3분 25초 → 4분
            "12,  5, 0,   1",    // 정확히 60초 → 1분 (올림 없음)
            "13,  5, 0,   2",    // 65초 → 2분
            "500, 10, 180, 87"   // 83분 20초 + 3분 = 86분 20초 → 87분
    })
    @DisplayName("허용 시간은 분 단위로 올림")
    void allowedTimeRoundsUpToMinutes(int count, int sec, int graceSec, long expectedMinutes) {
        Duration allowed = ExamTime.calculateAllowedTime(count, sec, Duration.ofSeconds(graceSec));

        assertThat(allowed).isEqualTo(Duration.ofMinutes(expectedMinutes));
    }

    @Test
    @DisplayName("시작하면 마감 = 시작 시각 + 허용 시간, 허용 분은 그 차이")
    void start() {
        ExamTime time = ExamTime.start(NOW, 500, 5, Duration.ofMinutes(3));

        assertThat(time.startedAt()).isEqualTo(NOW);
        assertThat(time.deadline()).isEqualTo(NOW.plus(Duration.ofMinutes(45)));
        assertThat(time.allowedMinutes()).isEqualTo(45);
        assertThat(time.submittedAt()).isNull();
    }

    @Test
    @DisplayName("마감 시각 정각까지는 마감 전, 1밀리초라도 지나면 마감 지남")
    void pastDeadline() {
        ExamTime time = new ExamTime(NOW, NOW.plusSeconds(60), null);

        assertThat(time.isPastDeadline(NOW.plusSeconds(60))).isFalse();
        assertThat(time.isPastDeadline(NOW.plusSeconds(60).plusMillis(1))).isTrue();
    }

    @Test
    @DisplayName("남은 초는 마감까지의 초, 마감이 지나면 0")
    void remainingSec() {
        ExamTime time = new ExamTime(NOW, NOW.plusSeconds(60), null);

        assertThat(time.remainingSec(NOW.plusSeconds(10))).isEqualTo(50);
        assertThat(time.remainingSec(NOW.plusSeconds(90))).isZero();
    }

    @Test
    @DisplayName("제출하면 제출 시각이 남고, 보관 마감 = 제출 시각 + 결과 보관")
    void submit() {
        ExamTime time = new ExamTime(NOW, NOW.plusSeconds(60), null).submit(NOW.plusSeconds(30));

        assertThat(time.submittedAt()).isEqualTo(NOW.plusSeconds(30));
        assertThat(time.retentionDeadline(Duration.ofHours(1))).isEqualTo(NOW.plusSeconds(30 + 3600));
    }

}

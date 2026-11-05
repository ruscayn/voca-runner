package org.ruscayn.vocarunner.exam.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverview;
import org.ruscayn.vocarunner.exam.domain.ExamCapacity;
import org.ruscayn.vocarunner.support.TestWords;
import org.ruscayn.vocarunner.word.domain.InMemoryWordCatalog;
import org.ruscayn.vocarunner.word.domain.Level;

@DisplayName("ExamLevelOverviewQueryService : 시험 레벨 개요")
class ExamLevelOverviewQueryServiceTest {

    private final ExamLevelOverviewQueryService service = new ExamLevelOverviewQueryService(
            new InMemoryWordCatalog(TestWords.words(Level.A1, 1, 20), List.of()),
            new ExamProperties(10, Duration.ofMinutes(1), Duration.ofHours(1), new ExamCapacity(100, 10),
                    Duration.ofMinutes(2)));

    @Test
    @DisplayName("레벨 하나 (준비 화면) : 문항 수 = 단어 수와 최대 문항 수 중 작은 값, 허용 시간은 분 단위 올림")
    void oneLevel() {
        ExamLevelOverview overview = service.getExamLevelOverview(Level.A1);

        assertThat(overview.wordCount()).isEqualTo(20);
        assertThat(overview.questionCount()).isEqualTo(10);
        assertThat(overview.allowedMinutes()).isEqualTo(2);     // 10문항 × 5초 + 1분 = 1분 50초 → 2분
    }

    @Test
    @DisplayName("묶음 레벨은 아직 시험을 볼 수 없어 거절 (B10)")
    void groupLevel() {
        assertThatThrownBy(() -> service.getExamLevelOverview(Level.A))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
    }

}

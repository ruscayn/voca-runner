package org.ruscayn.vocarunner.exam.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ruscayn.vocarunner.support.TestWords.questions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.word.domain.Level;

@DisplayName("ExamCapacity : 시험 수용량 (최대 진행 중 · IP당 최대 진행 중)")
class ExamCapacityTest {

    private static final ExamCapacity CAPACITY = new ExamCapacity(3, 2);
    private static final Examinee EXAMINEE = new Examinee("device-1", "10.0.0.1");

    private static Exam examOnDevice(String clientIp) {
        return Exam.start(Level.A1, questions(1), new Examinee("device-1", clientIp), Instant.now(),
                Duration.ofMinutes(3));
    }

    @Test
    @DisplayName("최대보다 적으면 통과")
    void belowMax() {
        assertThatCode(() -> CAPACITY.check(EXAMINEE, 2, 1, List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("진행 중이 최대에 닿으면 EXAM_TOO_MANY_IN_PROGRESS")
    void maxInProgress() {
        assertThatThrownBy(() -> CAPACITY.check(EXAMINEE, 3, 0, List.of()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS);
    }

    @Test
    @DisplayName("IP 진행 중이 IP당 최대에 닿으면 EXAM_TOO_MANY_IN_PROGRESS_PER_IP")
    void maxInProgressPerIp() {
        assertThatThrownBy(() -> CAPACITY.check(EXAMINEE, 2, 2, List.of()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP);
    }

    @Test
    @DisplayName("같은 기기의 진행 중 시험은 새 시험으로 바뀌므로 세지 않는다 (IP 수에서는 같은 IP 것만 뺌)")
    void sameDeviceNotCounted() {
        List<Exam> examsOnDevice = List.of(examOnDevice("10.0.0.1"), examOnDevice("10.0.0.9"));

        assertThatCode(() -> CAPACITY.check(EXAMINEE, 4, 2, examsOnDevice)).doesNotThrowAnyException();
        assertThatThrownBy(() -> CAPACITY.check(EXAMINEE, 4, 3, examsOnDevice))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP);
    }

}

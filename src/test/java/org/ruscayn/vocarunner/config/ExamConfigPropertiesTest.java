package org.ruscayn.vocarunner.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import org.ruscayn.vocarunner.exam.domain.ExamCapacity;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * 시험 설정 묶기. 자바 이름(maxQuestionCount)과 설정 이름(question-count)이 달라 @Name 으로 묶은 것을 확인한다.
 * 묶기가 풀리면 앱은 기본값(500)으로 조용히 돌아가므로 테스트로 고정한다.
 */
@DisplayName("ExamConfigProperties : 설정 이름 묶기")
class ExamConfigPropertiesTest {

    private static ExamConfigProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("voca-runner.exam", ExamConfigProperties.class)
                .get();
    }

    @Test
    @DisplayName("question-count 를 최대 문항 수로 읽는다")
    void maxQuestionCount() {
        ExamProperties properties = bind(Map.of("voca-runner.exam.question-count", "300")).toExamProperties();

        assertThat(properties.maxQuestionCount()).isEqualTo(300);
    }

    @Test
    @DisplayName("값이 없으면 기본값 (최대 문항 수 500, 여유 1분)")
    void defaults() {
        ExamProperties properties = bind(Map.of("voca-runner.exam.result-retention", "1h")).toExamProperties();

        assertThat(properties.maxQuestionCount()).isEqualTo(500);
        assertThat(properties.grace()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("max-in-progress · max-in-progress-per-ip 를 시험 수용량으로 읽는다 (IP당 기본 50)")
    void capacity() {
        ExamCapacity capacity = bind(Map.of("voca-runner.exam.max-in-progress", "300")).toExamCapacity();

        assertThat(capacity.maxInProgress()).isEqualTo(300);
        assertThat(capacity.maxInProgressPerIp()).isEqualTo(50);
    }

}

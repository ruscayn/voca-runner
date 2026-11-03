package org.ruscayn.vocarunner.exam.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시험 설정 묶기. 자바 이름(maxQuestionCount)과 설정 이름(question-count)이 달라 @Name 으로 묶은 것을 확인한다.
 * 묶기가 풀리면 앱은 기본값(500)으로 조용히 돌아가므로 테스트로 고정한다.
 */
@DisplayName("ExamProperties : 설정 이름 묶기")
class ExamPropertiesTest {

    private static ExamProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("voca-runner.test", ExamProperties.class)
                .get();
    }

    @Test
    @DisplayName("question-count 를 최대 문항 수로 읽는다")
    void maxQuestionCount() {
        ExamProperties properties = bind(Map.of("voca-runner.test.question-count", "300"));

        assertThat(properties.maxQuestionCount()).isEqualTo(300);
    }

    @Test
    @DisplayName("값이 없으면 기본값 (최대 문항 수 500, 여유 3분)")
    void defaults() {
        ExamProperties properties = bind(Map.of("voca-runner.test.grace", "3m"));

        assertThat(properties.maxQuestionCount()).isEqualTo(500);
        assertThat(properties.grace()).isEqualTo(Duration.ofMinutes(3));
    }
}

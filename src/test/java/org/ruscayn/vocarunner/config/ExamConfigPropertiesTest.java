package org.ruscayn.vocarunner.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

/**
 * 시험 설정 묶기. 자바 이름(maxQuestionCount)과 설정 이름(question-count)이 달라 @Name 으로 묶은 것을 확인한다.
 * 묶기가 풀리면 앱은 기본값(500)으로 조용히 돌아가므로 테스트로 고정한다.
 */
@DisplayName("ExamConfigProperties : 설정 이름 묶기")
class ExamConfigPropertiesTest {

    /** 설정 파일 값을 묶어 서비스가 받는 순수 값까지 만든다 */
    private static ExamProperties bind(Map<String, String> values) {
        return new Binder(new MapConfigurationPropertySource(values))
                .bind("voca-runner.test", ExamConfigProperties.class)
                .get()
                .toExamProperties();
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

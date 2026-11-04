package org.ruscayn.vocarunner.config;

import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExamConfig {

    @Bean
    public ExamProperties examProperties(ExamConfigProperties examConfigProperties) {
        return examConfigProperties.toExamProperties();
    }
}

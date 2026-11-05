package org.ruscayn.vocarunner.config;

import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import org.ruscayn.vocarunner.exam.domain.QuestionGenerator;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExamConfig {

    @Bean
    public ExamProperties examProperties(ExamConfigProperties examConfigProperties) {
        return examConfigProperties.toExamProperties();
    }

    @Bean
    public QuestionGenerator questionGenerator(WordCatalog wordCatalog) {
        return new QuestionGenerator(wordCatalog);
    }

}

package org.ruscayn.vocarunner.config;

import org.ruscayn.vocarunner.exam.application.service.ExamProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시험 설정 등록. 설정 파일에서 묶은 값(ExamConfigProperties)을 서비스가 받는 순수 값(ExamProperties)으로 넘긴다.
 * 서비스 쪽은 스프링 설정 기능에 기대지 않는다.
 */
@Configuration
public class ExamConfig {

    @Bean
    public ExamProperties examProperties(ExamConfigProperties examConfigProperties) {
        return examConfigProperties.toExamProperties();
    }
}

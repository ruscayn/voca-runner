package org.ruscayn.vocarunner.config;

import org.ruscayn.vocarunner.word.application.port.in.LoadWordCatalogUseCase;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 앱 시작 때 DB 에서 한 번 읽어 메모리에 둔다. 단어 데이터를 바꾸면 재시작해야 반영된다 */
@Configuration
public class WordCatalogConfig {

    @Bean
    public WordCatalog wordCatalog(LoadWordCatalogUseCase loadWordCatalogUseCase) {
        return loadWordCatalogUseCase.load();
    }

}

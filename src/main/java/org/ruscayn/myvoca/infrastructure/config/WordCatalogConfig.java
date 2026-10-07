package org.ruscayn.myvoca.infrastructure.config;

import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.vocabulary.WordCatalog;
import org.ruscayn.myvoca.infrastructure.cache.InMemoryWordCatalog;
import org.ruscayn.myvoca.infrastructure.persistence.vocabulary.CatalogWordLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 단어 캐시 등록. 서버 시작 시 DB 에서 1번 읽어 메모리에 올린다.
 * 단어 데이터를 수정하면 서버 재시작으로 반영.
 */
@Slf4j
@Configuration
public class WordCatalogConfig {

    @Bean
    public WordCatalog wordCatalog(CatalogWordLoader loader) {
        long begin = System.currentTimeMillis();
        InMemoryWordCatalog catalog = new InMemoryWordCatalog(loader.loadAll());

        String perLevel = Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(level -> level + "=" + catalog.countByLevel(level))
                .collect(Collectors.joining(", "));
        log.info("단어 캐시 로딩 완료: 전체 {}개 ({}) {}ms",
                catalog.size(), perLevel, System.currentTimeMillis() - begin);
        return catalog;
    }

}

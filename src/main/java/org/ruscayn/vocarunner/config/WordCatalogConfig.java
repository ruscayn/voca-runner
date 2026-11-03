package org.ruscayn.vocarunner.config;

import org.ruscayn.vocarunner.word.adapter.out.persistence.CatalogWordLoader;
import org.ruscayn.vocarunner.word.domain.InMemoryWordCatalog;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 단어 출제 목록 등록. 서버 시작 시 DB 에서 1번 읽어 메모리에 올린다.
 * 단어 데이터를 수정하면 서버 재시작으로 반영.
 */
@Slf4j
@Configuration
public class WordCatalogConfig {

    @Bean
    public WordCatalog wordCatalog(CatalogWordLoader loader) {
        long begin = System.currentTimeMillis();
        InMemoryWordCatalog catalog = new InMemoryWordCatalog(loader.loadAll(), loader.loadConfusableGroups());

        String perLevel = Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(level -> level + "=" + catalog.countByLevel(level))
                .collect(Collectors.joining(", "));
        log.info("단어 출제 목록 로딩 완료: 전체 {}행, 시험 제외 {}행, 레벨별 정답 가능 ({}), "
                + "헷갈리는 단어 묶음 {}개(단어 {}개) {}ms",
                catalog.size(), catalog.excludedCount(), perLevel,
                catalog.confusableGroupCount(), catalog.confusableWordCount(), System.currentTimeMillis() - begin);
        return catalog;
    }

}

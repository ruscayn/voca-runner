package org.ruscayn.vocarunner.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.ruscayn.vocarunner.word.application.port.out.WordPersistencePort;
import org.ruscayn.vocarunner.word.domain.InMemoryWordCatalog;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 앱 시작 때 DB 에서 한 번 읽어 메모리에 둔다. 단어 데이터를 바꾸면 재시작해야 반영된다 */
@Slf4j
@Configuration
public class WordCatalogConfig {

    @Bean
    public WordCatalog wordCatalog(WordPersistencePort wordPersistencePort) {
        long begin = System.currentTimeMillis();
        InMemoryWordCatalog catalog = new InMemoryWordCatalog(wordPersistencePort.loadCatalogWords(),
                wordPersistencePort.loadConfusableGroups());
        logCatalogLoaded(catalog, System.currentTimeMillis() - begin);
        return catalog;
    }

    private static void logCatalogLoaded(InMemoryWordCatalog catalog, long elapsedMs) {
        log.info("단어 출제 목록 로딩 완료: 전체 {}행, 시험 제외 {}행, 레벨별 정답 가능 ({}), "
                        + "헷갈리는 단어 묶음 {}개(단어 {}개) {}ms",
                catalog.size(), catalog.excludedCount(), answerableCountByDetailLevel(catalog),
                catalog.confusableGroupCount(), catalog.confusableWordCount(), elapsedMs);
    }

    private static String answerableCountByDetailLevel(InMemoryWordCatalog catalog) {
        return Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(level -> level + "=" + catalog.countByLevel(level))
                .collect(Collectors.joining(", "));
    }

}

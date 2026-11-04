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

/**
 * 단어 출제 목록 등록. 서버 시작 시 DB 에서 1번 읽어 메모리에 올린다.
 * 단어 데이터를 수정하면 서버 재시작으로 반영.
 */
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
                catalog.size(), catalog.excludedCount(), countByLevelText(catalog),
                catalog.confusableGroupCount(), catalog.confusableWordCount(), elapsedMs);
    }

    /** 세부 레벨별 정답 가능 수 (예: "A1=995, A2=…") */
    private static String countByLevelText(InMemoryWordCatalog catalog) {
        return Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(level -> level + "=" + catalog.countByLevel(level))
                .collect(Collectors.joining(", "));
    }

}

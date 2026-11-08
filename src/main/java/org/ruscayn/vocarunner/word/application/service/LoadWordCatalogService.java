package org.ruscayn.vocarunner.word.application.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.word.application.port.in.LoadWordCatalogUseCase;
import org.ruscayn.vocarunner.word.application.port.out.ReplaceWordCatalogPort;
import org.ruscayn.vocarunner.word.application.port.out.WordPersistencePort;
import org.ruscayn.vocarunner.word.domain.CatalogWord;
import org.ruscayn.vocarunner.word.domain.Level;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class LoadWordCatalogService implements LoadWordCatalogUseCase {

    private final WordPersistencePort wordPersistencePort;
    private final ReplaceWordCatalogPort replaceWordCatalogPort;

    /** 적재가 겹치면 먼저 읽은 쪽이 나중에 교체해 새 변경이 빠질 수 있어 한 번에 하나씩 돈다 */
    @Override
    public synchronized void load() {
        long begin = System.currentTimeMillis();
        WordCatalog wordCatalog = new WordCatalog(loadCatalogWords(), wordPersistencePort.loadConfusableGroups());
        replaceWordCatalogPort.replace(wordCatalog);
        logLoaded(wordCatalog, System.currentTimeMillis() - begin);
    }

    private List<CatalogWord> loadCatalogWords() {
        WordExposureRules rules = new WordExposureRules(wordPersistencePort.loadExposureRules());
        return wordPersistencePort.loadLevelWords().stream()
                .map(rules::apply)
                .toList();
    }

    private static void logLoaded(WordCatalog wordCatalog, long elapsedMs) {
        log.info("단어 출제 목록 로딩 완료: 전체 {}행, 시험 제외 {}행, 레벨별 정답 가능 ({}), "
                        + "혼동 단어 묶음 {}개(단어 {}개) {}ms",
                wordCatalog.getWordCount(), wordCatalog.getExcludedCount(), answerableCountByDetailLevel(wordCatalog),
                wordCatalog.getConfusableGroups().getGroupCount(), wordCatalog.getConfusableGroups().getWordCount(),
                elapsedMs);
    }

    private static String answerableCountByDetailLevel(WordCatalog wordCatalog) {
        return Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(level -> level + "=" + wordCatalog.countByLevel(level))
                .collect(Collectors.joining(", "));
    }

}

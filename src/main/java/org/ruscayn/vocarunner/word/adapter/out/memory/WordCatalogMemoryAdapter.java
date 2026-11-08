package org.ruscayn.vocarunner.word.adapter.out.memory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.ruscayn.vocarunner.word.application.port.out.GetWordCatalogPort;
import org.ruscayn.vocarunner.word.application.port.out.ReplaceWordCatalogPort;
import org.ruscayn.vocarunner.word.domain.WordCatalog;
import org.springframework.stereotype.Component;

@Component
public class WordCatalogMemoryAdapter implements ReplaceWordCatalogPort, GetWordCatalogPort {

    private final AtomicReference<WordCatalog> wordCatalog = new AtomicReference<>();

    @Override
    public void replace(WordCatalog newWordCatalog) {
        wordCatalog.set(Objects.requireNonNull(newWordCatalog, "wordCatalog"));
    }

    @Override
    public WordCatalog get() {
        WordCatalog loaded = wordCatalog.get();
        if (loaded == null) {
            throw new IllegalStateException("단어 출제 목록을 아직 적재하지 않았습니다");
        }
        return loaded;
    }

}

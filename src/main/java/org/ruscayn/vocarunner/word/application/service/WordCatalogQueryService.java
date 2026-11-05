package org.ruscayn.vocarunner.word.application.service;

import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.UseCase;
import org.ruscayn.vocarunner.word.application.port.in.WordCatalogQuery;
import org.ruscayn.vocarunner.word.application.port.out.GetWordCatalogPort;
import org.ruscayn.vocarunner.word.domain.WordCatalog;

@UseCase
@RequiredArgsConstructor
public class WordCatalogQueryService implements WordCatalogQuery {

    private final GetWordCatalogPort getWordCatalogPort;

    @Override
    public WordCatalog getWordCatalog() {
        return getWordCatalogPort.get();
    }

}

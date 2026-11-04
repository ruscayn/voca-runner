package org.ruscayn.vocarunner.word.application.port.out;

import java.util.Collection;
import java.util.List;
import org.ruscayn.vocarunner.word.domain.CatalogWord;

public interface WordPersistencePort {

    List<CatalogWord> loadCatalogWords();

    Collection<List<Long>> loadConfusableGroups();

}

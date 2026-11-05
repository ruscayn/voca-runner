package org.ruscayn.vocarunner.word.application.port.out;

import java.util.Collection;
import java.util.List;
import org.ruscayn.vocarunner.word.domain.LevelWord;
import org.ruscayn.vocarunner.word.domain.WordExposureRules;

public interface WordPersistencePort {

    List<WordExposureRules.Rule> loadExposureRules();

    List<LevelWord> loadLevelWords();

    Collection<List<Long>> loadConfusableGroups();

}

package org.ruscayn.myvoca.application.service;

import org.ruscayn.myvoca.application.dto.response.LevelSummary;
import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.model.vocabulary.WordCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LevelApplicationService {

    private final WordCatalog wordCatalog;

    /** 세부 레벨(A1~C2) 목록과 레벨별 단어 수 (메모리 캐시 사용) */
    public List<LevelSummary> getDetailLevels() {
        return Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(level -> new LevelSummary(level, wordCatalog.countByLevel(level)))
                .toList();
    }

}

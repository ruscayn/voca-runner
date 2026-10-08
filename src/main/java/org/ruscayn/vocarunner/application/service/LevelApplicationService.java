package org.ruscayn.vocarunner.application.service;

import org.ruscayn.vocarunner.application.dto.response.LevelSummary;
import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.vocabulary.WordCatalog;
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

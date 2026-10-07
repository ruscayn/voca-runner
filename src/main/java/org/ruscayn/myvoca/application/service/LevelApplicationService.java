package org.ruscayn.myvoca.application.service;

import org.ruscayn.myvoca.application.dto.response.LevelSummary;
import org.ruscayn.myvoca.domain.model.level.Level;
import org.ruscayn.myvoca.domain.repository.LevelWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LevelApplicationService {

    private final LevelWordRepository levelWordRepository;

    /** 세부 레벨(A1~C2) 목록과 레벨별 단어 수 */
    public List<LevelSummary> getDetailLevels() {
        return Arrays.stream(Level.values())
                .filter(level -> !level.isGroup())
                .map(level -> new LevelSummary(level, levelWordRepository.countByLevel(level)))
                .toList();
    }

}

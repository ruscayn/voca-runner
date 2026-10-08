package org.ruscayn.vocarunner.application.dto.response;

import org.ruscayn.vocarunner.domain.model.level.Level;

/** 메인 화면 레벨 선택 항목 */
public record LevelSummary(Level level, long wordCount) {

    public String code() {
        return level.name();
    }

}

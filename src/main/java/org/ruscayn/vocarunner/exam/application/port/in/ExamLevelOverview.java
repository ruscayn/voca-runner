package org.ruscayn.vocarunner.exam.application.port.in;

import org.ruscayn.vocarunner.word.domain.Level;

public record ExamLevelOverview(Level level, long wordCount, int questionCount, long allowedMinutes) {

    public String code() {
        return level.name();
    }

}

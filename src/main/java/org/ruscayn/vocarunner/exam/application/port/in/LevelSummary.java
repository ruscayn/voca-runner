package org.ruscayn.vocarunner.exam.application.port.in;

import org.ruscayn.vocarunner.word.domain.Level;

/**
 * 메인 화면 레벨 선택 항목
 *
 * @param level          세부 레벨
 * @param wordCount      단어 수 (레벨의 정답 후보 수)
 * @param questionCount  이 레벨을 고르면 실제로 나오는 문항 수 = min(정답 후보 수, 최대 문항 수)
 * @param allowedMinutes 이 레벨 시험을 마쳐야 하는 전체 시간(분, 올림)
 */
public record LevelSummary(Level level, long wordCount, int questionCount, long allowedMinutes) {

    public String code() {
        return level.name();
    }

}

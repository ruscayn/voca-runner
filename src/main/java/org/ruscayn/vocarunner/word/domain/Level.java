package org.ruscayn.vocarunner.word.domain;

import java.util.Arrays;
import java.util.List;

/** 선언 순서 = 낮은 레벨 → 높은 레벨. 높낮이 비교(isLowerThan)에 쓰므로 순서를 바꾸지 않는다 */
public enum Level {

    A1, A2, B1, B2, C1, C2,
    A, B, C;

    public boolean isGroup() {
        return name().length() == 1;
    }

    public String getGroupName() {
        return switch (name().charAt(0)) {
            case 'A' -> "Basic";
            case 'B' -> "Independent";
            default -> "Proficient";
        };
    }

    public List<Level> getDetailLevels() {
        if (!isGroup()) {
            return List.of(this);
        }
        return Arrays.stream(values())
                .filter(level -> !level.isGroup() && level.name().charAt(0) == name().charAt(0))
                .toList();
    }

    public boolean isLowerThan(Level other) {
        return this.ordinal() < other.ordinal();
    }

}

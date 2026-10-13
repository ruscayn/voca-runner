package org.ruscayn.vocarunner.domain.model.level;

import java.util.Arrays;
import java.util.List;

/**
 * 레벨 코드. DB 테이블 없이 enum 으로 관리한다.
 * <ul>
 *   <li>세부 레벨 A1~C2 : level_word.level_code, 시험 레벨</li>
 *   <li>묶음 레벨 A/B/C  : 시험 레벨로만 사용, DB 에는 저장하지 않음 (A = A1 + A2)</li>
 * </ul>
 * 선언 순서가 낮은 레벨 → 높은 레벨 순서이며, 비교에 사용한다.
 */
public enum Level {

    A1, A2, B1, B2, C1, C2,
    A, B, C;

    public boolean isGroup() {
        return name().length() == 1;
    }

    /**
     * CEFR 묶음 이름 (화면 표시용). 유럽평의회 CEFR 원문의 단계 묶음 이름을 그대로 사용한다.
     * A = Basic User, B = Independent User, C = Proficient User
     */
    public String groupName() {
        return switch (name().charAt(0)) {
            case 'A' -> "Basic";
            case 'B' -> "Independent";
            default -> "Proficient";
        };
    }

    /** 실제 출제 대상 세부 레벨. 세부 레벨이면 자기 자신, 묶음이면 소속 세부 레벨들. */
    public List<Level> detailLevels() {
        if (!isGroup()) {
            return List.of(this);
        }
        return Arrays.stream(values())
                .filter(level -> !level.isGroup() && level.name().charAt(0) == name().charAt(0))
                .toList();
    }

    /** 두 세부 레벨 중 낮은 레벨인지 */
    public boolean isLowerThan(Level other) {
        return this.ordinal() < other.ordinal();
    }

}

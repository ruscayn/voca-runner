package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;

/**
 * 메인 화면 레벨 묶음 (Basic / Independent / Proficient)
 *
 * @param name   묶음 이름
 * @param levels 묶음에 속한 세부 레벨 (낮은 레벨 → 높은 레벨)
 */
public record LevelGroupSummary(String name, List<LevelSummary> levels) {
}

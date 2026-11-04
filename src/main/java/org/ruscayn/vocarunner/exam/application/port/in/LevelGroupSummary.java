package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;

public record LevelGroupSummary(String name, List<LevelSummary> levels) {
}

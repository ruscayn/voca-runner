package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;

public record ExamLevelOverviewGroup(String name, List<ExamLevelOverview> levels) {

}

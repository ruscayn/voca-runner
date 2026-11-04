package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;

public interface ExamLevelOverviewQuery {

    List<ExamLevelOverviewGroup> getExamLevelOverviews();

}

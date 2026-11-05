package org.ruscayn.vocarunner.exam.application.port.in;

import java.util.List;
import org.ruscayn.vocarunner.word.domain.Level;

public interface ExamLevelOverviewQuery {

    List<ExamLevelOverviewGroup> getExamLevelOverviews();

    ExamLevelOverview getExamLevelOverview(Level level);

}

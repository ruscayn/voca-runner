package org.ruscayn.vocarunner.exam.adapter.in.web;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.WebAdapter;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverviewQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@WebAdapter
@Controller
@RequiredArgsConstructor
public class ExamController {

    private static final DateTimeFormatter RETENTION_DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final ExamLevelOverviewQuery examLevelOverviewQuery;
    private final ExamQuery examQuery;

    @GetMapping("/exam/ready")
    public String ready(@RequestParam Level level, Model model) {
        model.addAttribute("overview", examLevelOverviewQuery.getExamLevelOverview(level));
        model.addAttribute("groupName", level.groupName());
        model.addAttribute("timePerQuestionSec", Exam.DEFAULT_TIME_PER_QUESTION_SEC);
        return "exam";
    }

    @GetMapping("/exam/{examId}/result")
    public String result(@PathVariable UUID examId, Model model) {
        return examQuery.findResult(examId)
                .map(result -> {
                    model.addAttribute("result", result);
                    model.addAttribute("retentionDeadline",
                            RETENTION_DEADLINE_FORMAT.format(result.retentionDeadline()));
                    return "result";
                })
                .orElse("redirect:/?notice=" + MainController.NOTICE_RESULT_EXPIRED);
    }

}

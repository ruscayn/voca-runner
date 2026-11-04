package org.ruscayn.vocarunner.exam.adapter.in.web;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.WebAdapter;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.LevelGroupSummary;
import org.ruscayn.vocarunner.exam.application.port.in.LevelSummary;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@WebAdapter
@Controller
@RequiredArgsConstructor
public class MainController {

    public static final String NOTICE_EXAM_IN_USE = "test-in-use";
    public static final String NOTICE_EXAM_ENDED = "test-finished";
    public static final String NOTICE_RESULT_EXPIRED = "result-expired";

    private static final Map<String, String> NOTICE_MESSAGES = Map.of(
            NOTICE_EXAM_IN_USE, "이미 시작된 시험입니다. 시험을 시작한 화면에서만 풀 수 있습니다.",
            NOTICE_EXAM_ENDED, "이미 끝난 시험입니다. 새 시험을 시작해 주세요.",
            NOTICE_RESULT_EXPIRED, "결과 보관 시간이 지났습니다. 결과는 제출 후 잠시 동안만 볼 수 있어요.");

    private final ExamUseCase examUseCase;

    @GetMapping("/")
    public String main(@RequestParam(required = false) String notice, Model model) {
        if (!model.containsAttribute("message") && notice != null && NOTICE_MESSAGES.containsKey(notice)) {
            model.addAttribute("message", NOTICE_MESSAGES.get(notice));
        }
        addLevels(model);
        return "main";
    }

    private void addLevels(Model model) {
        List<LevelGroupSummary> levelGroups = examUseCase.getDetailLevelGroups();
        LevelSummary first = levelGroups.getFirst().levels().getFirst();
        model.addAttribute("levelGroups", levelGroups);
        model.addAttribute("questionCount", first.questionCount());
        model.addAttribute("timePerQuestionSec", Exam.DEFAULT_TIME_PER_QUESTION_SEC);
        model.addAttribute("allowedMinutes", first.allowedMinutes());
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }

}

package org.ruscayn.myvoca.presentation.controller;

import org.ruscayn.myvoca.application.service.LevelApplicationService;
import org.ruscayn.myvoca.domain.model.test.TestSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 메인 화면 : 레벨 선택 + 시험 시작 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final LevelApplicationService levelApplicationService;

    @GetMapping("/")
    public String main(Model model) {
        model.addAttribute("levels", levelApplicationService.getDetailLevels());
        model.addAttribute("questionCount", TestSession.DEFAULT_QUESTION_COUNT);
        model.addAttribute("timeLimitSec", TestSession.DEFAULT_TIME_LIMIT_SEC);
        return "main";
    }

}

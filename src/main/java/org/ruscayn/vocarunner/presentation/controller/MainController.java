package org.ruscayn.vocarunner.presentation.controller;

import org.ruscayn.vocarunner.application.service.LevelApplicationService;
import org.ruscayn.vocarunner.application.service.TestApplicationService;
import org.ruscayn.vocarunner.domain.model.test.TestSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** 메인 화면 : 레벨 선택 + 시험 시작 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final LevelApplicationService levelApplicationService;
    private final TestApplicationService testApplicationService;

    @GetMapping("/")
    public String main(Model model) {
        model.addAttribute("levels", levelApplicationService.getDetailLevels());
        model.addAttribute("questionCount", testApplicationService.getQuestionCount());
        model.addAttribute("timeLimitSec", TestSession.DEFAULT_TIME_LIMIT_SEC);
        model.addAttribute("allowedSec", testApplicationService.getAllowedSec());
        return "main";
    }

}

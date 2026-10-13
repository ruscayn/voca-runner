package org.ruscayn.vocarunner.presentation.controller;

import org.ruscayn.vocarunner.application.dto.response.LevelGroupSummary;
import org.ruscayn.vocarunner.application.dto.response.LevelSummary;
import org.ruscayn.vocarunner.application.service.LevelApplicationService;
import org.ruscayn.vocarunner.domain.model.test.TestSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/** 메인 화면 : 레벨 선택 + 시험 시작 */
@Controller
@RequiredArgsConstructor
public class MainController {

    private final LevelApplicationService levelApplicationService;

    @GetMapping("/")
    public String main(Model model) {
        List<LevelGroupSummary> levelGroups = levelApplicationService.getDetailLevelGroups();
        // 처음 선택된 레벨(첫 레벨)의 문항 수·제한 시간. 다른 레벨을 고르면 화면 스크립트가 바꾼다
        LevelSummary first = levelGroups.getFirst().levels().getFirst();
        model.addAttribute("levelGroups", levelGroups);
        model.addAttribute("questionCount", first.questionCount());
        model.addAttribute("timeLimitSec", TestSession.DEFAULT_TIME_LIMIT_SEC);
        model.addAttribute("allowedMinutes", first.allowedMinutes());
        return "main";
    }

    /** 정보 · 출처 (데이터 라이선스 표기) */
    @GetMapping("/about")
    public String about() {
        return "about";
    }

}

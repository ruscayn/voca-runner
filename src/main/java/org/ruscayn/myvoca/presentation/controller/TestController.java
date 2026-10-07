package org.ruscayn.myvoca.presentation.controller;

import org.ruscayn.myvoca.application.service.TestApplicationService;
import org.ruscayn.myvoca.domain.model.level.Level;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * 시험 화면.
 * <p>시험 화면은 시작 직후 딱 한 번만 열 수 있다. 새로고침·재접속하면 시험은 만료되고 메인으로 이동한다.
 * (시작 시 HttpSession 에 1회용 토큰을 넣고, 화면을 열 때 꺼내서 지운다)
 */
@Controller
@RequiredArgsConstructor
public class TestController {

    private static final String ENTRY_TOKEN = "TEST_ENTRY_TOKEN";

    private final TestApplicationService testApplicationService;

    @PostMapping("/test/start")
    public String start(@RequestParam Level level, HttpSession httpSession) {
        UUID sessionId = testApplicationService.start(level);
        httpSession.setAttribute(ENTRY_TOKEN, sessionId);
        return "redirect:/test/" + sessionId;
    }

    @GetMapping("/test/{sessionId}")
    public String test(@PathVariable UUID sessionId, HttpSession httpSession,
                       Model model, RedirectAttributes redirect) {
        Object token = httpSession.getAttribute(ENTRY_TOKEN);
        httpSession.removeAttribute(ENTRY_TOKEN);

        if (!sessionId.equals(token) || !testApplicationService.isInProgress(sessionId)) {
            testApplicationService.expireIfInProgress(sessionId);
            redirect.addFlashAttribute("message", "시험이 종료되었습니다. 처음부터 다시 시작해 주세요.");
            return "redirect:/";
        }

        model.addAttribute("sessionId", sessionId);
        return "test";
    }

}

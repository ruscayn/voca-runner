package org.ruscayn.vocarunner.exam.adapter.in.web;

import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.service.TestApplicationService;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.word.domain.Level;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 시험 화면.
 * <p>시험 화면은 시작 직후 딱 한 번만 열 수 있다.
 * (시작 시 HttpSession 에 1회용 입장권을 넣고, 화면을 열 때 꺼내서 지운다)
 * <ul>
 *   <li>같은 브라우저가 다시 들어오면(새로고침·재접속) 시험을 만료하고 메인으로 이동</li>
 *   <li>다른 브라우저·기기·링크 미리보기 봇이 주소를 열면 그쪽만 안내와 함께 메인으로 보내고, 시험은 그대로 둔다</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class TestController {

    private static final String ENTRY_TOKEN = "TEST_ENTRY_TOKEN";
    /** 이 브라우저(세션)가 마지막으로 연 시험 id. 같은 브라우저의 재입장과 다른 브라우저의 입장을 구분한다 */
    private static final String OPENED_TEST = "TEST_OPENED";

    /** 결과 화면의 "HH:mm까지" (사용자는 한국 기준) */
    private static final DateTimeFormatter AVAILABLE_UNTIL_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final TestApplicationService testApplicationService;
    private final ClientIpProperties clientIpProperties;

    @PostMapping("/test/start")
    public String start(@RequestParam Level level, HttpSession httpSession,
                        HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirect) {
        UUID sessionId;
        try {
            // 사용자 구분 = 기기 쿠키 + IP. 운영은 CloudFront 뒤라 IP 를 CloudFront 헤더에서 읽는다 (ClientIp)
            Examinee examinee = new Examinee(DeviceCookie.findOrCreate(request, response),
                    ClientIp.find(request, clientIpProperties.header()));
            sessionId = testApplicationService.start(level, examinee);
        } catch (CustomException e) {
            if (e.getErrorCode() == ErrorCode.TEST_SERVER_BUSY
                    || e.getErrorCode() == ErrorCode.TEST_TOO_MANY_IN_PROGRESS_SAME_NETWORK) {
                redirect.addFlashAttribute("message", e.getMessage());
                return "redirect:/";
            }
            throw e;
        }
        httpSession.setAttribute(ENTRY_TOKEN, sessionId);
        return "redirect:/test/" + sessionId;
    }

    @GetMapping("/test/{sessionId}")
    public String test(@PathVariable UUID sessionId, HttpServletRequest request,
                       Model model, RedirectAttributes redirect) {
        // 세션을 새로 만들지 않는다. 쿠키 없는 요청(다른 기기·링크 미리보기 봇)에 세션을 만들면
        // redirect 주소에 ;jsessionid 가 붙어 404 원시 JSON 이 보인다 (B9)
        HttpSession httpSession = request.getSession(false);
        Object token = httpSession == null ? null : httpSession.getAttribute(ENTRY_TOKEN);
        Object opened = httpSession == null ? null : httpSession.getAttribute(OPENED_TEST);
        boolean inProgress = testApplicationService.isInProgress(sessionId);

        // 1) 시작 직후 첫 입장 : 입장권을 쓰고, 이 브라우저가 연 시험으로 기록
        if (inProgress && sessionId.equals(token)) {
            httpSession.removeAttribute(ENTRY_TOKEN);
            httpSession.setAttribute(OPENED_TEST, sessionId);
            model.addAttribute("sessionId", sessionId);
            return "test";
        }

        // 2) 이미 끝난 시험 (결과 화면에서 뒤로가기 등) : 결과 보관 중이면 결과 화면, 아니면 안내와 함께 메인 (B8)
        //    시험 화면은 no-store 라 뒤로가기도 반드시 여기로 온다 (WebConfig)
        if (!inProgress) {
            if (testApplicationService.hasResult(sessionId)) {
                return "redirect:/test/" + sessionId + "/result";
            }
            return "redirect:/?notice=" + MainController.NOTICE_TEST_FINISHED;
        }

        // 3) 이 시험을 연 브라우저가 다시 들어옴 (새로고침·재접속·같은 브라우저 새 탭) : 다시 풀 수 없게 만료
        if (sessionId.equals(opened)) {
            testApplicationService.expireIfInProgress(sessionId, DeviceCookie.find(request));
            redirect.addFlashAttribute("message", "시험이 종료되었습니다. 처음부터 다시 시작해 주세요.");
            return "redirect:/";
        }

        // 4) 다른 브라우저·기기·링크 미리보기 봇 : 원래 사용자의 시험은 건드리지 않고 이쪽만 돌려보낸다 (B2)
        //    세션이 없을 수 있어 flash 대신 주소 파라미터로 안내한다
        return "redirect:/?notice=" + MainController.NOTICE_TEST_IN_USE;
    }

    /** 결과 화면 (완료된 시험만). 보관 시간이 지나 정리됐으면 안내와 함께 메인 (U17) */
    @GetMapping("/test/{sessionId}/result")
    public String result(@PathVariable UUID sessionId, Model model) {
        return testApplicationService.findResult(sessionId)
                .map(result -> {
                    model.addAttribute("result", result);
                    model.addAttribute("availableUntil", AVAILABLE_UNTIL_FORMAT.format(result.availableUntil()));
                    return "result";
                })
                .orElse("redirect:/?notice=" + MainController.NOTICE_RESULT_EXPIRED);
    }

}

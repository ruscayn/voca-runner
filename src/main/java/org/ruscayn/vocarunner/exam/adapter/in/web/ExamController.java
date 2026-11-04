package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.common.WebAdapter;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.exam.domain.Examinee;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 시험 화면.
 * <p>시험 화면은 시작 직후 딱 한 번만 열 수 있다.
 * (시작 시 HttpSession 에 1회용 입장 토큰을 넣고, 화면을 열 때 꺼내서 지운다)
 * <ul>
 *   <li>같은 브라우저가 다시 들어오면(새로고침·재접속) 시험을 중단하고 메인으로 이동</li>
 *   <li>다른 브라우저·기기·링크 미리보기 봇이 주소를 열면 그쪽만 안내와 함께 메인으로 보내고, 시험은 그대로 둔다</li>
 * </ul>
 */
@WebAdapter
@Controller
@RequiredArgsConstructor
public class ExamController {

    private static final String ENTRY_TOKEN = "TEST_ENTRY_TOKEN";
    /** 이 브라우저(세션)가 마지막으로 연 시험 id. 같은 브라우저의 재입장과 다른 브라우저의 입장을 구분한다 */
    private static final String OPENED_EXAM = "TEST_OPENED";

    /** 결과 화면의 "HH:mm까지" (사용자는 한국 기준) */
    private static final DateTimeFormatter RETENTION_DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final ExamUseCase examUseCase;
    private final ClientIpProperties clientIpProperties;

    @PostMapping("/test/start")
    public String start(@RequestParam Level level, HttpSession httpSession,
                        HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirect) {
        try {
            UUID examId = examUseCase.start(new StartExamCommand(level, toExaminee(request, response)));
            httpSession.setAttribute(ENTRY_TOKEN, examId);
            return "redirect:/test/" + examId;
        } catch (CustomException e) {
            return redirectToMainIfTooMany(e, redirect);
        }
    }

    /** 사용자 구분 = 기기 쿠키 + IP. 운영은 CloudFront 뒤라 IP 를 CloudFront 헤더에서 읽는다 (ClientIp) */
    private Examinee toExaminee(HttpServletRequest request, HttpServletResponse response) {
        return new Examinee(DeviceCookie.findOrCreate(request, response),
                ClientIp.find(request, clientIpProperties.header()));
    }

    /** 최대 진행 중에 걸려 거절되면 안내와 함께 메인으로. 그 밖의 오류는 그대로 던진다 */
    private static String redirectToMainIfTooMany(CustomException e, RedirectAttributes redirect) {
        if (e.getErrorCode() != ErrorCode.EXAM_TOO_MANY_IN_PROGRESS
                && e.getErrorCode() != ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP) {
            throw e;
        }
        redirect.addFlashAttribute("message", e.getMessage());
        return "redirect:/";
    }

    /**
     * 시험 화면을 연다. 시작 직후 첫 입장만 화면을 열고, 그 밖에는 경우에 따라 돌려보낸다.
     * 세션을 새로 만들지 않는다. 쿠키 없는 요청(다른 기기·링크 미리보기 봇)에 세션을 만들면
     * redirect 주소에 ;jsessionid 가 붙어 404 원시 JSON 이 보인다 (B9)
     */
    @GetMapping("/test/{examId}")
    public String exam(@PathVariable UUID examId, HttpServletRequest request,
                       Model model, RedirectAttributes redirect) {
        if (!examUseCase.isInProgress(examId)) {
            return redirectEnded(examId);
        }
        HttpSession httpSession = request.getSession(false);
        if (examId.equals(attribute(httpSession, ENTRY_TOKEN))) {
            return openFirstEntry(examId, httpSession, model);
        }
        return redirectReentry(examId, httpSession, request, redirect);
    }

    /**
     * 이미 끝난 시험 (결과 화면에서 뒤로가기 등) : 결과 보관 중이면 결과 화면, 아니면 안내와 함께 메인 (B8).
     * 시험 화면은 no-store 라 뒤로가기도 반드시 여기로 온다 (WebConfig)
     */
    private String redirectEnded(UUID examId) {
        if (examUseCase.hasResult(examId)) {
            return "redirect:/test/" + examId + "/result";
        }
        return "redirect:/?notice=" + MainController.NOTICE_EXAM_ENDED;
    }

    /** 시작 직후 첫 입장 : 입장 토큰을 쓰고, 이 브라우저가 연 시험으로 기록 */
    private static String openFirstEntry(UUID examId, HttpSession httpSession, Model model) {
        httpSession.removeAttribute(ENTRY_TOKEN);
        httpSession.setAttribute(OPENED_EXAM, examId);
        model.addAttribute("examId", examId);
        return "test";
    }

    /**
     * 첫 입장이 아닌 진행 중 시험.
     * <ul>
     *   <li>이 시험을 연 브라우저가 다시 들어옴 (새로고침·재접속·같은 브라우저 새 탭) : 다시 풀 수 없게 중단</li>
     *   <li>다른 브라우저·기기·링크 미리보기 봇 : 원래 사용자의 시험은 건드리지 않고 이쪽만 돌려보낸다 (B2).
     *       세션이 없을 수 있어 flash 대신 주소 파라미터로 안내한다</li>
     * </ul>
     */
    private String redirectReentry(UUID examId, HttpSession httpSession, HttpServletRequest request,
                                   RedirectAttributes redirect) {
        if (!examId.equals(attribute(httpSession, OPENED_EXAM))) {
            return "redirect:/?notice=" + MainController.NOTICE_EXAM_IN_USE;
        }
        examUseCase.abortExam(examId, DeviceCookie.find(request));
        redirect.addFlashAttribute("message", "시험이 종료되었습니다. 처음부터 다시 시작해 주세요.");
        return "redirect:/";
    }

    /** 세션 속성. 세션이 없으면 null */
    private static Object attribute(HttpSession httpSession, String name) {
        return httpSession == null ? null : httpSession.getAttribute(name);
    }

    /** 결과 화면 (완료된 시험만). 보관 시간이 지나 정리됐으면 안내와 함께 메인 (U17) */
    @GetMapping("/test/{examId}/result")
    public String result(@PathVariable UUID examId, Model model) {
        return examUseCase.findResult(examId)
                .map(result -> {
                    model.addAttribute("result", result);
                    model.addAttribute("retentionDeadline",
                            RETENTION_DEADLINE_FORMAT.format(result.retentionDeadline()));
                    return "result";
                })
                .orElse("redirect:/?notice=" + MainController.NOTICE_RESULT_EXPIRED);
    }

}

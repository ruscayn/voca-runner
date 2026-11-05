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
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamUseCase;
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
 * 시험 화면은 시작 직후 한 번만 열 수 있다 ({@link EntryToken}).
 * <ul>
 *   <li>같은 브라우저가 다시 들어오면(새로고침 · 재접속) 시험을 중단하고 메인으로</li>
 *   <li>다른 브라우저 · 기기 · 링크 미리보기 봇이 주소를 열면 그쪽만 안내와 함께 메인으로, 시험은 그대로</li>
 * </ul>
 */
@WebAdapter
@Controller
@RequiredArgsConstructor
public class ExamController {

    private static final DateTimeFormatter RETENTION_DEADLINE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private final StartExamUseCase startExamUseCase;
    private final ExamUseCase examUseCase;
    private final ExamQuery examQuery;
    private final ClientIpProperties clientIpProperties;

    @PostMapping("/exam/start")
    public String start(@RequestParam Level level, HttpSession httpSession,
                        HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirect) {
        try {
            UUID examId = startExamUseCase.start(new StartExamCommand(level, toExaminee(request, response)));
            EntryToken.issue(httpSession, examId);
            return "redirect:/exam/" + examId;
        } catch (CustomException e) {
            return redirectToMainIfTooMany(e, redirect);
        }
    }

    private Examinee toExaminee(HttpServletRequest request, HttpServletResponse response) {
        return new Examinee(DeviceCookie.findOrCreate(request, response),
                ClientIp.find(request, clientIpProperties.header()));
    }

    private static String redirectToMainIfTooMany(CustomException e, RedirectAttributes redirect) {
        if (e.getErrorCode() != ErrorCode.EXAM_TOO_MANY_IN_PROGRESS
                && e.getErrorCode() != ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP) {
            throw e;
        }
        redirect.addFlashAttribute("message", e.getMessage());
        return "redirect:/";
    }

    @GetMapping("/exam/{examId}")
    public String exam(@PathVariable UUID examId, HttpServletRequest request,
                       Model model, RedirectAttributes redirect) {
        if (!examQuery.isInProgress(examId)) {
            return redirectEnded(examId);
        }
        if (EntryToken.use(request, examId)) {
            model.addAttribute("examId", examId);
            return "exam";
        }
        return redirectReentry(examId, request, redirect);
    }

    private String redirectEnded(UUID examId) {
        if (examQuery.hasResult(examId)) {
            return "redirect:/exam/" + examId + "/result";
        }
        return "redirect:/?notice=" + MainController.NOTICE_EXAM_ENDED;
    }

    /** 세션이 없을 수 있어(다른 브라우저 · 봇) 안내는 flash 대신 주소 파라미터로 한다 */
    private String redirectReentry(UUID examId, HttpServletRequest request, RedirectAttributes redirect) {
        if (!EntryToken.isLastOpened(request, examId)) {
            return "redirect:/?notice=" + MainController.NOTICE_EXAM_IN_USE;
        }
        examUseCase.abortIfInProgress(examId, DeviceCookie.find(request));
        redirect.addFlashAttribute("message", "시험이 종료되었습니다. 처음부터 다시 시작해 주세요.");
        return "redirect:/";
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

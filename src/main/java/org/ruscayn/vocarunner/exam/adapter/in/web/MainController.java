package org.ruscayn.vocarunner.exam.adapter.in.web;

import org.ruscayn.vocarunner.exam.application.port.in.LevelGroupSummary;
import org.ruscayn.vocarunner.exam.application.port.in.LevelSummary;
import org.ruscayn.vocarunner.exam.application.service.LevelApplicationService;
import org.ruscayn.vocarunner.exam.domain.Exam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** 메인 화면 : 레벨 선택 + 시험 시작 */
@Controller
@RequiredArgsConstructor
public class MainController {

    /** 진행 중인 시험 주소를 다른 브라우저·기기에서 연 경우 (ExamController) */
    public static final String NOTICE_EXAM_IN_USE = "test-in-use";
    /** 이미 끝났고 결과 보관 시간도 지난 시험 주소로 들어온 경우 (ExamController) */
    public static final String NOTICE_EXAM_ENDED = "test-finished";
    /** 결과 보관 시간이 지난 결과 주소로 들어온 경우 (ExamController) */
    public static final String NOTICE_RESULT_EXPIRED = "result-expired";

    private final LevelApplicationService levelApplicationService;

    /**
     * @param notice 세션 없이 안내할 때 쓰는 코드. 정해진 코드만 문구로 바꾸고 그 밖의 값은 무시한다
     */
    @GetMapping("/")
    public String main(@RequestParam(required = false) String notice, Model model) {
        if (!model.containsAttribute("message")) {
            if (NOTICE_EXAM_IN_USE.equals(notice)) {
                model.addAttribute("message", "이미 시작된 시험입니다. 시험을 시작한 화면에서만 풀 수 있습니다.");
            } else if (NOTICE_EXAM_ENDED.equals(notice)) {
                model.addAttribute("message", "이미 끝난 시험입니다. 새 시험을 시작해 주세요.");
            } else if (NOTICE_RESULT_EXPIRED.equals(notice)) {
                model.addAttribute("message", "결과 보관 시간이 지났습니다. 결과는 제출 후 잠시 동안만 볼 수 있어요.");
            }
        }
        List<LevelGroupSummary> levelGroups = levelApplicationService.getDetailLevelGroups();
        // 처음 선택된 레벨(첫 레벨)의 문항 수·허용 시간. 다른 레벨을 고르면 화면 스크립트가 바꾼다
        LevelSummary first = levelGroups.getFirst().levels().getFirst();
        model.addAttribute("levelGroups", levelGroups);
        model.addAttribute("questionCount", first.questionCount());
        model.addAttribute("timePerQuestionSec", Exam.DEFAULT_TIME_PER_QUESTION_SEC);
        model.addAttribute("allowedMinutes", first.allowedMinutes());
        return "main";
    }

    /** 정보 · 출처 (데이터 라이선스 표기) */
    @GetMapping("/about")
    public String about() {
        return "about";
    }

}

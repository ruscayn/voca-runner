package org.ruscayn.vocarunner.exam.adapter.in.web;

import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuestionsResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamResult;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.common.WebAdapter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 시험 REST API (화면 / 추후 앱 공용). 결과 외에는 시험을 시작한 기기(VR_DEVICE 쿠키)만 쓸 수 있다 */
@WebAdapter
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class ExamApiController {

    private final ExamUseCase examUseCase;

    /** 문제 전체 (정답 제외). 시작한 기기만 */
    @GetMapping("/{examId}/questions")
    public ExamQuestionsResult questions(@PathVariable UUID examId, HttpServletRequest httpRequest) {
        return examUseCase.getQuestions(examId, DeviceCookie.find(httpRequest));
    }

    /**
     * 제출 및 채점. 시작한 기기만.
     * 같은 시험을 동시에 두 번 제출하면(요청을 직접 보낸 경우. 화면은 한 번만 보내고, 같은 브라우저 새 탭으로 열면 시험이 끝난다)
     * 먼저 저장한 쪽만 채점되고(@Version) 늦은 쪽은 저장할 때 충돌하므로, 이미 끝난 시험을 제출한 것과 같은 응답을 준다 (U39)
     */
    @PostMapping("/{examId}/submit")
    public SubmitExamResult submit(@PathVariable UUID examId, @RequestBody SubmitExamRequest request,
                                   HttpServletRequest httpRequest) {
        SubmitExamCommand command = new SubmitExamCommand(examId, DeviceCookie.find(httpRequest), request.answers());
        try {
            return examUseCase.submit(command);
        } catch (OptimisticLockingFailureException e) {
            throw new CustomException(ErrorCode.EXAM_NOT_IN_PROGRESS);
        }
    }

    /** 결과 (완료된 시험만) */
    @GetMapping("/{examId}/result")
    public ExamResult result(@PathVariable UUID examId) {
        return examUseCase.getResult(examId);
    }

    /** 중도 이탈 시 중단 (navigator.sendBeacon 용, 본문 없음). 시작한 기기만 (다른 기기면 무시) */
    @PostMapping("/{examId}/expire")
    public ResponseEntity<Void> abort(@PathVariable UUID examId, HttpServletRequest httpRequest) {
        examUseCase.abortExam(examId, DeviceCookie.find(httpRequest));
        return ResponseEntity.noContent().build();
    }

}

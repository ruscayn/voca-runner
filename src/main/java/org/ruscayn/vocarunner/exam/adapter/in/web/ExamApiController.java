package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.common.WebAdapter;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuestionsResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamResult;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@WebAdapter
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamApiController {

    private final ExamUseCase examUseCase;

    @GetMapping("/{examId}/questions")
    public ExamQuestionsResult questions(@PathVariable UUID examId, HttpServletRequest httpRequest) {
        return examUseCase.getQuestions(examId, DeviceCookie.find(httpRequest));
    }

    /**
     * 같은 시험을 동시에 두 번 제출하면 먼저 저장한 쪽만 채점되고 늦은 쪽은 저장 충돌(@Version)이 나므로,
     * 끝난 시험을 제출한 것과 같은 응답을 준다
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

    @GetMapping("/{examId}/result")
    public ExamResult result(@PathVariable UUID examId) {
        return examUseCase.getResult(examId);
    }

    /** navigator.sendBeacon 으로 보내므로 본문 없이 받는다 */
    @PostMapping("/{examId}/abort")
    public ResponseEntity<Void> abort(@PathVariable UUID examId, HttpServletRequest httpRequest) {
        examUseCase.abortIfInProgress(examId, DeviceCookie.find(httpRequest));
        return ResponseEntity.noContent().build();
    }

}

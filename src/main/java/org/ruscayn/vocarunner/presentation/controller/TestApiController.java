package org.ruscayn.vocarunner.presentation.controller;

import org.ruscayn.vocarunner.application.dto.request.TestSubmitRequest;
import org.ruscayn.vocarunner.application.dto.response.TestQuestionsResponse;
import org.ruscayn.vocarunner.application.dto.response.TestResultResponse;
import org.ruscayn.vocarunner.application.dto.response.TestSubmitResponse;
import org.ruscayn.vocarunner.application.service.TestApplicationService;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 시험 REST API (화면 / 추후 앱 공용). 결과 외에는 시험을 시작한 기기(VR_DEVICE 쿠키)만 쓸 수 있다 */
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestApiController {

    private final TestApplicationService testApplicationService;

    /** 문제 전체 (정답 제외). 시작한 기기만 */
    @GetMapping("/{sessionId}/questions")
    public TestQuestionsResponse questions(@PathVariable UUID sessionId, HttpServletRequest httpRequest) {
        return testApplicationService.getQuestions(sessionId, DeviceCookie.find(httpRequest));
    }

    /**
     * 제출 및 채점. 시작한 기기만.
     * 같은 시험을 동시에 두 번 제출하면(요청을 직접 보낸 경우. 화면은 한 번만 보내고, 같은 브라우저 새 탭으로 열면 시험이 끝난다)
     * 먼저 저장한 쪽만 채점되고(@Version) 늦은 쪽은 저장할 때 충돌하므로, 이미 끝난 시험을 제출한 것과 같은 응답을 준다 (U39)
     */
    @PostMapping("/{sessionId}/submit")
    public TestSubmitResponse submit(@PathVariable UUID sessionId,
                                     @Valid @RequestBody TestSubmitRequest request, HttpServletRequest httpRequest) {
        try {
            return testApplicationService.submit(sessionId, DeviceCookie.find(httpRequest), request);
        } catch (OptimisticLockingFailureException e) {
            throw new CustomException(ErrorCode.TEST_NOT_IN_PROGRESS);
        }
    }

    /** 결과 (완료된 시험만) */
    @GetMapping("/{sessionId}/result")
    public TestResultResponse result(@PathVariable UUID sessionId) {
        return testApplicationService.getResult(sessionId);
    }

    /** 중도 이탈 시 만료 (navigator.sendBeacon 용, 본문 없음). 시작한 기기만 (다른 기기면 무시) */
    @PostMapping("/{sessionId}/expire")
    public ResponseEntity<Void> expire(@PathVariable UUID sessionId, HttpServletRequest httpRequest) {
        testApplicationService.expireIfInProgress(sessionId, DeviceCookie.find(httpRequest));
        return ResponseEntity.noContent().build();
    }

}

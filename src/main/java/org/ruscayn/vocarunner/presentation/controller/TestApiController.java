package org.ruscayn.vocarunner.presentation.controller;

import org.ruscayn.vocarunner.application.dto.request.TestSubmitRequest;
import org.ruscayn.vocarunner.application.dto.response.TestQuestionsResponse;
import org.ruscayn.vocarunner.application.dto.response.TestResultResponse;
import org.ruscayn.vocarunner.application.dto.response.TestSubmitResponse;
import org.ruscayn.vocarunner.application.service.TestApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /** 제출 및 채점. 시작한 기기만 */
    @PostMapping("/{sessionId}/submit")
    public TestSubmitResponse submit(@PathVariable UUID sessionId,
                                     @Valid @RequestBody TestSubmitRequest request, HttpServletRequest httpRequest) {
        return testApplicationService.submit(sessionId, DeviceCookie.find(httpRequest), request);
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

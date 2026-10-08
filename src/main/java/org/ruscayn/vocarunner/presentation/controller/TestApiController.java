package org.ruscayn.vocarunner.presentation.controller;

import org.ruscayn.vocarunner.application.dto.request.TestSubmitRequest;
import org.ruscayn.vocarunner.application.dto.response.TestQuestionsResponse;
import org.ruscayn.vocarunner.application.dto.response.TestResultResponse;
import org.ruscayn.vocarunner.application.dto.response.TestSubmitResponse;
import org.ruscayn.vocarunner.application.service.TestApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** 시험 REST API (화면 / 추후 앱 공용) */
@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
public class TestApiController {

    private final TestApplicationService testApplicationService;

    /** 문제 전체 (정답 제외) */
    @GetMapping("/{sessionId}/questions")
    public TestQuestionsResponse questions(@PathVariable UUID sessionId) {
        return testApplicationService.getQuestions(sessionId);
    }

    /** 제출 및 채점 */
    @PostMapping("/{sessionId}/submit")
    public TestSubmitResponse submit(@PathVariable UUID sessionId,
                                     @Valid @RequestBody TestSubmitRequest request) {
        return testApplicationService.submit(sessionId, request);
    }

    /** 결과 (완료된 시험만) */
    @GetMapping("/{sessionId}/result")
    public TestResultResponse result(@PathVariable UUID sessionId) {
        return testApplicationService.getResult(sessionId);
    }

    /** 중도 이탈 시 만료 (navigator.sendBeacon 용, 본문 없음) */
    @PostMapping("/{sessionId}/expire")
    public ResponseEntity<Void> expire(@PathVariable UUID sessionId) {
        testApplicationService.expireIfInProgress(sessionId);
        return ResponseEntity.noContent().build();
    }

}

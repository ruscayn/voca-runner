package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.service.ExamApplicationService;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 시험 API 요청 형식 검사와 오류 응답.
 * 서비스는 가짜로 바꿔 요청이 서비스까지 가는지만 본다 (DB 없음).
 */
@WebMvcTest(ExamApiController.class)
@DisplayName("ExamApiController : 시험 API 요청 형식")
class ExamApiControllerTest {

    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DEVICE_ID = "22222222-2222-2222-2222-222222222222";
    private static final Cookie DEVICE_COOKIE = new Cookie("VR_DEVICE", DEVICE_ID);
    private static final String SUBMIT_URL = "/api/tests/" + EXAM_ID + "/submit";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamApplicationService examApplicationService;

    private ResultActions submit(MediaType contentType, String body) throws Exception {
        return mockMvc.perform(post(SUBMIT_URL).cookie(DEVICE_COOKIE).contentType(contentType).content(body));
    }

    @Nested
    @DisplayName("제출 (/api/tests/{id}/submit)")
    class Submit {

        @Test
        @DisplayName("올바른 본문이면 서비스로 넘긴다")
        void valid() throws Exception {
            submit(MediaType.APPLICATION_JSON, """
                    {"answers": [{"questionNo": 1, "selectedOptionNo": 2}, {"questionNo": 2, "selectedOptionNo": null}]}
                    """)
                    .andExpect(status().isOk());

            verify(examApplicationService).submit(eq(EXAM_ID), eq(DEVICE_ID), any());
        }

        @Test
        @DisplayName("JSON 이 아닌 본문은 400 (U44)")
        void notJson() throws Exception {
            submit(MediaType.APPLICATION_JSON, "abc")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));

            verify(examApplicationService, never()).submit(any(), any(), any());
        }

        @Test
        @DisplayName("문제 번호·보기 번호 타입이 틀리면 400 (U44)")
        void wrongType() throws Exception {
            submit(MediaType.APPLICATION_JSON, """
                    {"answers": [{"questionNo": "x", "selectedOptionNo": 1}]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));
            submit(MediaType.APPLICATION_JSON, """
                    {"answers": [{"questionNo": 1, "selectedOptionNo": "x"}]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));

            verify(examApplicationService, never()).submit(any(), any(), any());
        }

        @Test
        @DisplayName("고른 보기 목록(answers)에 null 항목이 있으면 400 (U44)")
        void nullAnswer() throws Exception {
            submit(MediaType.APPLICATION_JSON, """
                    {"answers": [null]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));

            verify(examApplicationService, never()).submit(any(), any(), any());
        }

        @Test
        @DisplayName("동시에 두 번 제출해 늦은 쪽이 저장에서 충돌하면 500 이 아니라 400 진행 중인 시험이 아님 (U39)")
        void concurrentSubmit() throws Exception {
            given(examApplicationService.submit(eq(EXAM_ID), eq(DEVICE_ID), any()))
                    .willThrow(new ObjectOptimisticLockingFailureException(Exam.class, EXAM_ID));

            submit(MediaType.APPLICATION_JSON, """
                    {"answers": [{"questionNo": 1, "selectedOptionNo": 2}]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TEST_002"));
        }

        @Test
        @DisplayName("JSON 이 아닌 형식(text/plain)은 415 (U44)")
        void notJsonContentType() throws Exception {
            submit(MediaType.TEXT_PLAIN, """
                    {"answers": []}
                    """)
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("COMMON_005"));

            verify(examApplicationService, never()).submit(any(), any(), any());
        }
    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("오류 로그")
    class ErrorLog {

        @Test
        @DisplayName("업무 오류(거절)는 스택 없이 WARN 한 줄로 남긴다")
        void businessExceptionIsWarnWithoutStack(CapturedOutput output) throws Exception {
            given(examApplicationService.submit(eq(EXAM_ID), eq(DEVICE_ID), any()))
                    .willThrow(new CustomException(ErrorCode.EXAM_EXPIRED));

            submit(MediaType.APPLICATION_JSON, """
                    {"answers": []}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("TEST_005"));

            assertThat(output.getOut()).contains("WARN").contains("Business exception").contains("TEST_005")
                    .doesNotContain("ERROR").doesNotContain("\tat ");
        }
    }

}

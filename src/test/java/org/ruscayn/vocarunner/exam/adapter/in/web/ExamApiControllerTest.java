package org.ruscayn.vocarunner.exam.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamUseCase;
import org.ruscayn.vocarunner.exam.domain.Exam;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 시험 API 요청 형식 검사와 오류 응답.
 * 서비스는 가짜로 바꿔 요청이 서비스까지 가는지만 본다 (DB 없음).
 * IP 헤더 설정은 application.yaml(운영 값 CloudFront-Viewer-Address)을 그대로 쓴다.
 */
@WebMvcTest(ExamApiController.class)
@EnableConfigurationProperties(ClientIpProperties.class)
@DisplayName("ExamApiController : 시험 API 요청 형식")
class ExamApiControllerTest {

    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DEVICE_ID = "22222222-2222-2222-2222-222222222222";
    private static final Cookie DEVICE_COOKIE = new Cookie("VR_DEVICE", DEVICE_ID);
    private static final String SUBMIT_URL = "/api/exams/" + EXAM_ID + "/submit";
    private static final int ONE_DAY_SEC = 24 * 60 * 60;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartExamUseCase startExamUseCase;

    @MockitoBean
    private ExamUseCase examUseCase;

    @MockitoBean
    private ExamQuery examQuery;

    private ResultActions submit(MediaType contentType, String body) throws Exception {
        return mockMvc.perform(post(SUBMIT_URL).cookie(DEVICE_COOKIE).contentType(contentType).content(body));
    }

    @Nested
    @DisplayName("시작 (/api/exams) — 준비 화면의 [시작]")
    class Start {

        @Test
        @DisplayName("시험을 만들어 시험 아이디를 주고, 기기 쿠키가 없으면 새로 준다 (1일)")
        void start() throws Exception {
            given(startExamUseCase.start(argThat(command -> command.level() == Level.A1))).willReturn(EXAM_ID);

            mockMvc.perform(post("/api/exams").param("level", "A1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()))
                    .andExpect(cookie().exists("VR_DEVICE"))
                    .andExpect(cookie().maxAge("VR_DEVICE", ONE_DAY_SEC));
        }

        @Test
        @DisplayName("기기 쿠키가 있으면 같은 값으로 다시 줘 기간을 시작 때부터 새로 센다")
        void renewsDeviceCookie() throws Exception {
            given(startExamUseCase.start(argThat(command -> command.level() == Level.A1))).willReturn(EXAM_ID);

            mockMvc.perform(post("/api/exams").param("level", "A1").cookie(DEVICE_COOKIE))
                    .andExpect(cookie().value("VR_DEVICE", DEVICE_ID))
                    .andExpect(cookie().maxAge("VR_DEVICE", ONE_DAY_SEC));
        }

        @Test
        @DisplayName("묶음 레벨은 시험을 만들지 않고 400 (B10 — 묶음 레벨 시험은 아직 없음)")
        void groupLevel() throws Exception {
            mockMvc.perform(post("/api/exams").param("level", "A"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));

            verify(startExamUseCase, never()).start(any());
        }

        @Test
        @DisplayName("응시자가 많아 거절되면 400 과 화면에 띄울 안내 문구")
        void serverBusy() throws Exception {
            given(startExamUseCase.start(argThat(command -> command.level() == Level.A1)))
                    .willThrow(new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS));

            mockMvc.perform(post("/api/exams").param("level", "A1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS.getMessage()));
        }

        @Test
        @DisplayName("같은 인터넷에서 진행 중 시험이 너무 많으면 400 과 안내 문구")
        void tooManySameNetwork() throws Exception {
            given(startExamUseCase.start(argThat(command -> command.level() == Level.A1)))
                    .willThrow(new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP));

            mockMvc.perform(post("/api/exams").param("level", "A1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP.getCode()))
                    .andExpect(jsonPath("$.message")
                            .value(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP.getMessage()));
        }

        @Test
        @DisplayName("IP 는 CloudFront 헤더에서 포트를 빼고 읽고, 헤더가 없으면 연결 IP (U32)")
        void clientIp() throws Exception {
            given(startExamUseCase.start(argThat(command -> command.level() == Level.A1))).willReturn(EXAM_ID);

            mockMvc.perform(post("/api/exams").param("level", "A1")
                    .header("CloudFront-Viewer-Address", "198.51.100.10:46532"));
            mockMvc.perform(post("/api/exams").param("level", "A1"));

            ArgumentCaptor<StartExamCommand> commands = ArgumentCaptor.forClass(StartExamCommand.class);
            verify(startExamUseCase, times(2)).start(commands.capture());
            assertThat(commands.getAllValues()).extracting(command -> command.examinee().clientIp())
                    .containsExactly("198.51.100.10", "127.0.0.1");
        }

    }

    @Nested
    @DisplayName("제출 (/api/exams/{id}/submit)")
    class Submit {

        @Test
        @DisplayName("올바른 본문이면 서비스로 넘긴다")
        void valid() throws Exception {
            submit(MediaType.APPLICATION_JSON, """
                    {"selectedOptions": [{"questionNo": 1, "selectedOptionNo": 2}, {"questionNo": 2, "selectedOptionNo": null}]}
                    """)
                    .andExpect(status().isOk());

            verify(examUseCase).submit(argThat(command ->
                    command.examId().equals(EXAM_ID) && DEVICE_ID.equals(command.deviceId())));
        }

        @Test
        @DisplayName("옛 이름 answers 로 보내도 받는다 (배포할 때 시험을 풀던 옛 화면)")
        void oldName() throws Exception {
            submit(MediaType.APPLICATION_JSON, """
                    {"answers": [{"questionNo": 1, "selectedOptionNo": 2}]}
                    """)
                    .andExpect(status().isOk());

            verify(examUseCase).submit(argThat(command -> command.selectedOptions().size() == 1));
        }

        @Test
        @DisplayName("JSON 이 아닌 본문은 400 (U44)")
        void notJson() throws Exception {
            submit(MediaType.APPLICATION_JSON, "abc")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));

            verify(examUseCase, never()).submit(any());
        }

        @Test
        @DisplayName("문제 번호·보기 번호 타입이 틀리면 400 (U44)")
        void wrongType() throws Exception {
            submit(MediaType.APPLICATION_JSON, """
                    {"selectedOptions": [{"questionNo": "x", "selectedOptionNo": 1}]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));
            submit(MediaType.APPLICATION_JSON, """
                    {"selectedOptions": [{"questionNo": 1, "selectedOptionNo": "x"}]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));

            verify(examUseCase, never()).submit(any());
        }

        @Test
        @DisplayName("선택한 보기 목록(selectedOptions)에 null 항목이 있으면 400 (U44)")
        void nullSelectedOption() throws Exception {
            submit(MediaType.APPLICATION_JSON, """
                    {"selectedOptions": [null]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("COMMON_002"));

            verify(examUseCase, never()).submit(any());
        }

        @Test
        @DisplayName("동시에 두 번 제출해 늦은 쪽이 저장에서 충돌하면 500 이 아니라 400 진행 중인 시험이 아님 (U39)")
        void concurrentSubmit() throws Exception {
            given(examUseCase.submit(any()))
                    .willThrow(new ObjectOptimisticLockingFailureException(Exam.class, EXAM_ID));

            submit(MediaType.APPLICATION_JSON, """
                    {"selectedOptions": [{"questionNo": 1, "selectedOptionNo": 2}]}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EXAM_002"));
        }

        @Test
        @DisplayName("JSON 이 아닌 형식(text/plain)은 415 (U44)")
        void notJsonContentType() throws Exception {
            submit(MediaType.TEXT_PLAIN, """
                    {"selectedOptions": []}
                    """)
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.code").value("COMMON_005"));

            verify(examUseCase, never()).submit(any());
        }

    }

    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    @DisplayName("오류 로그")
    class ErrorLog {

        @Test
        @DisplayName("업무 오류(거절)는 스택 없이 WARN 한 줄로 남긴다")
        void businessExceptionIsWarnWithoutStack(CapturedOutput output) throws Exception {
            given(examUseCase.submit(any()))
                    .willThrow(new CustomException(ErrorCode.EXAM_EXPIRED));

            submit(MediaType.APPLICATION_JSON, """
                    {"selectedOptions": []}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("EXAM_005"));

            assertThat(output.getOut()).contains("WARN").contains("업무 오류").contains("EXAM_005")
                    .doesNotContain("ERROR").doesNotContain("\tat ");
        }

    }

}

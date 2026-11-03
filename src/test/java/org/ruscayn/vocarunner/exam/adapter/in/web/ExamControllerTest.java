package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.service.ExamApplicationService;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamCommand;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 시험 주소로 들어왔을 때 어느 화면으로 보내는지 (B2·B8·B9·U17).
 * 서비스는 가짜로 바꿔 시험 상태(진행 중·결과 보관 중·없음)만 정해 준다 (DB 없음).
 * IP 헤더 설정은 application.yaml(운영 값 CloudFront-Viewer-Address)을 그대로 쓴다.
 */
@WebMvcTest(ExamController.class)
@EnableConfigurationProperties(ClientIpProperties.class)
@DisplayName("ExamController : 시험 화면 이동 규칙")
class ExamControllerTest {

    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DEVICE_ID = "22222222-2222-2222-2222-222222222222";
    private static final Cookie DEVICE_COOKIE = new Cookie("VR_DEVICE", DEVICE_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamApplicationService examApplicationService;

    /** [시작]을 누른 브라우저 (시험 화면 입장 토큰을 받은 세션) */
    private MockHttpSession startTest() throws Exception {
        given(examApplicationService.start(argThat(command -> command.level() == Level.A1))).willReturn(EXAM_ID);
        MockHttpSession httpSession = new MockHttpSession();
        mockMvc.perform(post("/test/start").param("level", "A1").session(httpSession).cookie(DEVICE_COOKIE))
                .andExpect(redirectedUrl("/test/" + EXAM_ID));
        return httpSession;
    }

    @Nested
    @DisplayName("시작")
    class Start {

        @Test
        @DisplayName("시작하면 시험 화면으로 보내고, 기기 쿠키가 없으면 새로 준다")
        void redirectsToTest() throws Exception {
            given(examApplicationService.start(argThat(command -> command.level() == Level.A1))).willReturn(EXAM_ID);

            mockMvc.perform(post("/test/start").param("level", "A1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/test/" + EXAM_ID))
                    .andExpect(cookie().exists("VR_DEVICE"));
        }

        @Test
        @DisplayName("응시자가 많아 거절되면 안내와 함께 메인으로")
        void serverBusy() throws Exception {
            given(examApplicationService.start(argThat(command -> command.level() == Level.A1)))
                    .willThrow(new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS));

            mockMvc.perform(post("/test/start").param("level", "A1"))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("message", ErrorCode.EXAM_TOO_MANY_IN_PROGRESS.getMessage()));
        }

        @Test
        @DisplayName("같은 인터넷에서 진행 중 시험이 너무 많으면 안내와 함께 메인으로")
        void tooManySameNetwork() throws Exception {
            given(examApplicationService.start(argThat(command -> command.level() == Level.A1)))
                    .willThrow(new CustomException(ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP));

            mockMvc.perform(post("/test/start").param("level", "A1"))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("message",
                            ErrorCode.EXAM_TOO_MANY_IN_PROGRESS_PER_IP.getMessage()));
        }

        @Test
        @DisplayName("IP 는 CloudFront 헤더에서 포트를 빼고 읽고, 헤더가 없으면 연결 IP (U32)")
        void clientIp() throws Exception {
            given(examApplicationService.start(argThat(command -> command.level() == Level.A1))).willReturn(EXAM_ID);

            mockMvc.perform(post("/test/start").param("level", "A1")
                    .header("CloudFront-Viewer-Address", "198.51.100.10:46532"));
            mockMvc.perform(post("/test/start").param("level", "A1"));

            ArgumentCaptor<StartExamCommand> commands = ArgumentCaptor.forClass(StartExamCommand.class);
            verify(examApplicationService, times(2)).start(commands.capture());
            assertThat(commands.getAllValues()).extracting(command -> command.examinee().clientIp())
                    .containsExactly("198.51.100.10", "127.0.0.1");
        }
    }

    @Nested
    @DisplayName("시험 화면 (/test/{id})")
    class ExamPage {

        @Test
        @DisplayName("시작한 브라우저가 처음 들어오면 시험 화면을 보여 주고, 브라우저가 저장하지 않게 한다")
        void firstEntry() throws Exception {
            MockHttpSession httpSession = startTest();
            given(examApplicationService.isInProgress(EXAM_ID)).willReturn(true);

            mockMvc.perform(get("/test/" + EXAM_ID).session(httpSession).cookie(DEVICE_COOKIE))
                    .andExpect(status().isOk())
                    .andExpect(view().name("test"))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }

        @Test
        @DisplayName("같은 브라우저가 다시 들어오면(새로고침) 시험을 끝내고 안내와 함께 메인으로")
        void reEntry() throws Exception {
            MockHttpSession httpSession = startTest();
            given(examApplicationService.isInProgress(EXAM_ID)).willReturn(true);
            mockMvc.perform(get("/test/" + EXAM_ID).session(httpSession).cookie(DEVICE_COOKIE))
                    .andExpect(view().name("test"));

            mockMvc.perform(get("/test/" + EXAM_ID).session(httpSession).cookie(DEVICE_COOKIE))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("message", "시험이 종료되었습니다. 처음부터 다시 시작해 주세요."));
            verify(examApplicationService).abortExam(EXAM_ID, DEVICE_ID);
        }

        @Test
        @DisplayName("다른 브라우저·기기가 진행 중 시험 주소를 열면 그쪽만 메인으로 보내고 시험은 그대로 둔다 (B2)")
        void otherBrowser() throws Exception {
            given(examApplicationService.isInProgress(EXAM_ID)).willReturn(true);

            MvcResult result = mockMvc.perform(get("/test/" + EXAM_ID))
                    .andExpect(redirectedUrl("/?notice=" + MainController.NOTICE_EXAM_IN_USE))
                    .andReturn();

            verify(examApplicationService, never()).abortExam(any(), any());
            // 세션을 만들지 않는다 (만들면 주소에 ;jsessionid 가 붙음, B9)
            assertThat(result.getRequest().getSession(false)).isNull();
        }

        @Test
        @DisplayName("이미 끝난 시험이고 결과를 보관 중이면 결과 화면으로 (결과 화면에서 뒤로 가기, B8)")
        void finishedWithResult() throws Exception {
            given(examApplicationService.isInProgress(EXAM_ID)).willReturn(false);
            given(examApplicationService.hasResult(EXAM_ID)).willReturn(true);

            mockMvc.perform(get("/test/" + EXAM_ID))
                    .andExpect(redirectedUrl("/test/" + EXAM_ID + "/result"));
        }

        @Test
        @DisplayName("이미 끝났고 결과도 정리된 시험이면 안내와 함께 메인으로 (B8)")
        void finishedWithoutResult() throws Exception {
            given(examApplicationService.isInProgress(EXAM_ID)).willReturn(false);
            given(examApplicationService.hasResult(EXAM_ID)).willReturn(false);

            mockMvc.perform(get("/test/" + EXAM_ID))
                    .andExpect(redirectedUrl("/?notice=" + MainController.NOTICE_EXAM_ENDED));
        }

        @Test
        @DisplayName("시험 id 가 올바르지 않으면 한국어 오류 화면 (B4)")
        void invalidId() throws Exception {
            mockMvc.perform(get("/test/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("error"));
        }
    }

    @Nested
    @DisplayName("결과 화면 (/test/{id}/result)")
    class ResultPage {

        @Test
        @DisplayName("결과 보관 시간이 지났으면 안내와 함께 메인으로 (U17)")
        void expired() throws Exception {
            given(examApplicationService.findResult(EXAM_ID)).willReturn(Optional.empty());

            mockMvc.perform(get("/test/" + EXAM_ID + "/result"))
                    .andExpect(redirectedUrl("/?notice=" + MainController.NOTICE_RESULT_EXPIRED));
        }
    }

}

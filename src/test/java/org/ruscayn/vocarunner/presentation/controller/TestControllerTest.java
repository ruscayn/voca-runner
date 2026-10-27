package org.ruscayn.vocarunner.presentation.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruscayn.vocarunner.application.service.TestApplicationService;
import org.ruscayn.vocarunner.domain.model.level.Level;
import org.ruscayn.vocarunner.domain.model.test.TestClient;
import org.ruscayn.vocarunner.presentation.config.ClientIpProperties;
import org.ruscayn.vocarunner.shared.exception.CustomException;
import org.ruscayn.vocarunner.shared.exception.ErrorCode;
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
import static org.mockito.ArgumentMatchers.eq;
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
@WebMvcTest(TestController.class)
@EnableConfigurationProperties(ClientIpProperties.class)
@DisplayName("TestController : 시험 화면 이동 규칙")
class TestControllerTest {

    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DEVICE_ID = "22222222-2222-2222-2222-222222222222";
    private static final Cookie DEVICE_COOKIE = new Cookie("VR_DEVICE", DEVICE_ID);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestApplicationService testApplicationService;

    /** [시작]을 누른 브라우저 (시험 화면 입장권을 받은 세션) */
    private MockHttpSession startTest() throws Exception {
        given(testApplicationService.start(eq(Level.A1), any())).willReturn(SESSION_ID);
        MockHttpSession httpSession = new MockHttpSession();
        mockMvc.perform(post("/test/start").param("level", "A1").session(httpSession).cookie(DEVICE_COOKIE))
                .andExpect(redirectedUrl("/test/" + SESSION_ID));
        return httpSession;
    }

    @Nested
    @DisplayName("시작")
    class Start {

        @Test
        @DisplayName("시작하면 시험 화면으로 보내고, 기기 쿠키가 없으면 새로 준다")
        void redirectsToTest() throws Exception {
            given(testApplicationService.start(eq(Level.A1), any())).willReturn(SESSION_ID);

            mockMvc.perform(post("/test/start").param("level", "A1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/test/" + SESSION_ID))
                    .andExpect(cookie().exists("VR_DEVICE"));
        }

        @Test
        @DisplayName("응시자가 많아 거절되면 안내와 함께 메인으로")
        void serverBusy() throws Exception {
            given(testApplicationService.start(eq(Level.A1), any()))
                    .willThrow(new CustomException(ErrorCode.TEST_SERVER_BUSY));

            mockMvc.perform(post("/test/start").param("level", "A1"))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("message", ErrorCode.TEST_SERVER_BUSY.getMessage()));
        }

        @Test
        @DisplayName("같은 인터넷에서 진행 중 시험이 너무 많으면 안내와 함께 메인으로")
        void tooManySameNetwork() throws Exception {
            given(testApplicationService.start(eq(Level.A1), any()))
                    .willThrow(new CustomException(ErrorCode.TEST_TOO_MANY_IN_PROGRESS_SAME_NETWORK));

            mockMvc.perform(post("/test/start").param("level", "A1"))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("message",
                            ErrorCode.TEST_TOO_MANY_IN_PROGRESS_SAME_NETWORK.getMessage()));
        }

        @Test
        @DisplayName("IP 는 CloudFront 헤더에서 포트를 빼고 읽고, 헤더가 없으면 연결 IP (U32)")
        void clientIp() throws Exception {
            given(testApplicationService.start(eq(Level.A1), any())).willReturn(SESSION_ID);

            mockMvc.perform(post("/test/start").param("level", "A1")
                    .header("CloudFront-Viewer-Address", "198.51.100.10:46532"));
            mockMvc.perform(post("/test/start").param("level", "A1"));

            ArgumentCaptor<TestClient> clients = ArgumentCaptor.forClass(TestClient.class);
            verify(testApplicationService, times(2)).start(eq(Level.A1), clients.capture());
            assertThat(clients.getAllValues()).extracting(TestClient::ip)
                    .containsExactly("198.51.100.10", "127.0.0.1");
        }
    }

    @Nested
    @DisplayName("시험 화면 (/test/{id})")
    class TestPage {

        @Test
        @DisplayName("시작한 브라우저가 처음 들어오면 시험 화면을 보여 주고, 브라우저가 저장하지 않게 한다")
        void firstEntry() throws Exception {
            MockHttpSession httpSession = startTest();
            given(testApplicationService.isInProgress(SESSION_ID)).willReturn(true);

            mockMvc.perform(get("/test/" + SESSION_ID).session(httpSession).cookie(DEVICE_COOKIE))
                    .andExpect(status().isOk())
                    .andExpect(view().name("test"))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }

        @Test
        @DisplayName("같은 브라우저가 다시 들어오면(새로고침) 시험을 끝내고 안내와 함께 메인으로")
        void reEntry() throws Exception {
            MockHttpSession httpSession = startTest();
            given(testApplicationService.isInProgress(SESSION_ID)).willReturn(true);
            mockMvc.perform(get("/test/" + SESSION_ID).session(httpSession).cookie(DEVICE_COOKIE))
                    .andExpect(view().name("test"));

            mockMvc.perform(get("/test/" + SESSION_ID).session(httpSession).cookie(DEVICE_COOKIE))
                    .andExpect(redirectedUrl("/"))
                    .andExpect(flash().attribute("message", "시험이 종료되었습니다. 처음부터 다시 시작해 주세요."));
            verify(testApplicationService).expireIfInProgress(SESSION_ID, DEVICE_ID);
        }

        @Test
        @DisplayName("다른 브라우저·기기가 진행 중 시험 주소를 열면 그쪽만 메인으로 보내고 시험은 그대로 둔다 (B2)")
        void otherBrowser() throws Exception {
            given(testApplicationService.isInProgress(SESSION_ID)).willReturn(true);

            MvcResult result = mockMvc.perform(get("/test/" + SESSION_ID))
                    .andExpect(redirectedUrl("/?notice=" + MainController.NOTICE_TEST_IN_USE))
                    .andReturn();

            verify(testApplicationService, never()).expireIfInProgress(any(), any());
            // 세션을 만들지 않는다 (만들면 주소에 ;jsessionid 가 붙음, B9)
            assertThat(result.getRequest().getSession(false)).isNull();
        }

        @Test
        @DisplayName("이미 끝난 시험이고 결과를 보관 중이면 결과 화면으로 (결과 화면에서 뒤로 가기, B8)")
        void finishedWithResult() throws Exception {
            given(testApplicationService.isInProgress(SESSION_ID)).willReturn(false);
            given(testApplicationService.hasResult(SESSION_ID)).willReturn(true);

            mockMvc.perform(get("/test/" + SESSION_ID))
                    .andExpect(redirectedUrl("/test/" + SESSION_ID + "/result"));
        }

        @Test
        @DisplayName("이미 끝났고 결과도 정리된 시험이면 안내와 함께 메인으로 (B8)")
        void finishedWithoutResult() throws Exception {
            given(testApplicationService.isInProgress(SESSION_ID)).willReturn(false);
            given(testApplicationService.hasResult(SESSION_ID)).willReturn(false);

            mockMvc.perform(get("/test/" + SESSION_ID))
                    .andExpect(redirectedUrl("/?notice=" + MainController.NOTICE_TEST_FINISHED));
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
            given(testApplicationService.findResult(SESSION_ID)).willReturn(Optional.empty());

            mockMvc.perform(get("/test/" + SESSION_ID + "/result"))
                    .andExpect(redirectedUrl("/?notice=" + MainController.NOTICE_RESULT_EXPIRED));
        }
    }

}

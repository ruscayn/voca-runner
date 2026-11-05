package org.ruscayn.vocarunner.exam.adapter.in.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverview;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverviewQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 시험 화면 주소로 들어왔을 때 어느 화면으로 보내는지 (B4 · U17 · P13).
 * 서비스는 가짜로 바꿔 레벨 개요 · 결과만 정해 준다 (DB 없음).
 */
@WebMvcTest(ExamController.class)
@DisplayName("ExamController : 시험 화면 이동 규칙")
class ExamControllerTest {

    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExamLevelOverviewQuery examLevelOverviewQuery;

    @MockitoBean
    private ExamQuery examQuery;

    @Nested
    @DisplayName("준비 화면 (/exam/ready)")
    class ReadyPage {

        @Test
        @DisplayName("고른 레벨의 시험 화면을 시작 전 안내로 보여 주고, 브라우저가 저장하지 않게 한다 (시험은 아직 만들지 않음)")
        void ready() throws Exception {
            given(examLevelOverviewQuery.getExamLevelOverview(Level.A1))
                    .willReturn(new ExamLevelOverview(Level.A1, 1049, 500, 43));

            mockMvc.perform(get("/exam/ready").param("level", "A1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("exam"))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }

        @Test
        @DisplayName("묶음 레벨이면 한국어 오류 화면 (B10)")
        void groupLevel() throws Exception {
            given(examLevelOverviewQuery.getExamLevelOverview(Level.A))
                    .willThrow(new CustomException(ErrorCode.INVALID_INPUT_VALUE));

            mockMvc.perform(get("/exam/ready").param("level", "A"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("error"));
        }

        @Test
        @DisplayName("없는 레벨이거나 레벨이 없으면 한국어 오류 화면 (B4)")
        void invalidLevel() throws Exception {
            mockMvc.perform(get("/exam/ready").param("level", "Z9"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("error"));
            mockMvc.perform(get("/exam/ready"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("error"));
        }

    }

    @Nested
    @DisplayName("결과 화면 (/exam/{id}/result)")
    class ResultPage {

        @Test
        @DisplayName("결과 보관 시간이 지났으면 안내와 함께 메인으로 (U17)")
        void expired() throws Exception {
            given(examQuery.findResult(EXAM_ID)).willReturn(Optional.empty());

            mockMvc.perform(get("/exam/" + EXAM_ID + "/result"))
                    .andExpect(redirectedUrl("/?notice=" + MainController.NOTICE_RESULT_EXPIRED));
        }

        @Test
        @DisplayName("시험 id 가 올바르지 않으면 한국어 오류 화면 (B4)")
        void invalidId() throws Exception {
            mockMvc.perform(get("/exam/abc/result"))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("error"));
        }

    }

}

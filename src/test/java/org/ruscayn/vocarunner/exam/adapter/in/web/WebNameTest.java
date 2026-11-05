package org.ruscayn.vocarunner.exam.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.ruscayn.vocarunner.common.CustomException;
import org.ruscayn.vocarunner.common.ErrorCode;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverview;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverviewGroup;
import org.ruscayn.vocarunner.exam.application.port.in.ExamLevelOverviewQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuery;
import org.ruscayn.vocarunner.exam.application.port.in.ExamQuestionsResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamResult;
import org.ruscayn.vocarunner.exam.application.port.in.ExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.StartExamUseCase;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamCommand;
import org.ruscayn.vocarunner.exam.application.port.in.SubmitExamResult;
import org.ruscayn.vocarunner.word.domain.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 바깥 이름 고정 (안전망). 코드 이름을 바꿀 때 컴파일러가 잡지 못하는 이름이 바뀌면 실패한다.
 * <ul>
 *   <li>API JSON 칸 이름 : 화면 스크립트(exam.js)가 읽는 응답 · 보내는 제출 본문 · 오류 응답</li>
 *   <li>화면 모델 이름 : 템플릿이 쓰는 이름 (메인 · 시험 화면 · 결과 화면)</li>
 *   <li>템플릿 안 메서드 호출 : 화면을 실제로 그려서 확인 (예: 결과 화면의 오답 수 · 보관 시간 문구)</li>
 * </ul>
 * 서비스는 가짜로 바꾼다 (DB 없음).
 */
@WebMvcTest({ExamApiController.class, ExamController.class, MainController.class})
@EnableConfigurationProperties(ClientIpProperties.class)
@DisplayName("바깥 이름 고정 : API JSON 칸 · 화면 모델 이름 · 템플릿")
class WebNameTest {

    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String DEVICE_ID = "22222222-2222-2222-2222-222222222222";
    private static final Cookie DEVICE_COOKIE = new Cookie("VR_DEVICE", DEVICE_ID);

    /** 2026-11-03 14:20 (한국 시각) */
    private static final Instant RETENTION_DEADLINE = Instant.parse("2026-11-03T05:20:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StartExamUseCase startExamUseCase;

    @MockitoBean
    private ExamUseCase examUseCase;

    @MockitoBean
    private ExamQuery examQuery;

    @MockitoBean
    private ExamLevelOverviewQuery examLevelOverviewQuery;

    private static Map<String, Object> readObject(String json, String path) {
        return JsonPath.read(json, path);
    }

    private static ExamResult resultWithOneIncorrectAndOneTimeout() {
        return new ExamResult(EXAM_ID, "C2", "Proficient", 10, 8, 80, RETENTION_DEADLINE, 60,
                List.of(new ExamResult.WrongItem(3, "abroad", "go abroad", "국내에", "해외로", "abroad"),
                        new ExamResult.WrongItem(7, "sail", "sail away", null, "항해하다", "sail")));
    }

    @Nested
    @DisplayName("API JSON 칸")
    class ApiJson {

        @Test
        @DisplayName("시험 시작 응답 : examId")
        void start() throws Exception {
            given(startExamUseCase.start(any())).willReturn(EXAM_ID);

            String json = mockMvc.perform(post("/api/exams").param("level", "A1"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(readObject(json, "$").keySet()).containsExactly("examId");
        }

        @Test
        @DisplayName("문제 받기 응답 : 시험 · 문항 · 보기 칸")
        void questions() throws Exception {
            ExamQuestionsResult.QuestionItem question = new ExamQuestionsResult.QuestionItem(1, "abroad", "go abroad",
                    List.of(new ExamQuestionsResult.Option(1, "해외로"), new ExamQuestionsResult.Option(2, "국내에")));
            given(examUseCase.deliverQuestions(EXAM_ID, DEVICE_ID)).willReturn(
                    new ExamQuestionsResult(EXAM_ID, "B1", "Independent", "ENGLISH", 1, 5, 3, 170,
                            List.of(question)));

            String json = mockMvc.perform(get("/api/exams/" + EXAM_ID + "/questions").cookie(DEVICE_COOKIE))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(readObject(json, "$").keySet()).containsExactlyInAnyOrder(
                    "examId", "level", "groupName", "subject", "questionCount", "timePerQuestionSec",
                    "allowedMinutes", "remainingSec", "questions");
            assertThat(readObject(json, "$.questions[0]").keySet())
                    .containsExactlyInAnyOrder("no", "prompt", "promptSub", "options");
            assertThat(readObject(json, "$.questions[0].options[0]").keySet())
                    .containsExactlyInAnyOrder("no", "text");
        }

        @Test
        @DisplayName("제출 본문 : answers[].questionNo · selectedOptionNo 를 읽는다")
        void submitBody() throws Exception {
            given(examUseCase.submit(any()))
                    .willReturn(new SubmitExamResult(EXAM_ID, 2, 1, 50));

            mockMvc.perform(post("/api/exams/" + EXAM_ID + "/submit").cookie(DEVICE_COOKIE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"answers": [{"questionNo": 1, "selectedOptionNo": 2}, {"questionNo": 2}]}
                                    """))
                    .andExpect(status().isOk());

            ArgumentCaptor<SubmitExamCommand> captor = ArgumentCaptor.forClass(SubmitExamCommand.class);
            verify(examUseCase).submit(captor.capture());
            assertThat(captor.getValue().examId()).isEqualTo(EXAM_ID);
            assertThat(captor.getValue().deviceId()).isEqualTo(DEVICE_ID);
            List<SubmitExamCommand.Answer> answers = captor.getValue().answers();
            assertThat(answers.get(0).questionNo()).isEqualTo(1);
            assertThat(answers.get(0).selectedOptionNo()).isEqualTo(2);
            assertThat(answers.get(1).questionNo()).isEqualTo(2);
            assertThat(answers.get(1).selectedOptionNo()).isNull();
        }

        @Test
        @DisplayName("제출 응답 칸")
        void submitResponse() throws Exception {
            given(examUseCase.submit(any()))
                    .willReturn(new SubmitExamResult(EXAM_ID, 2, 1, 50));

            String json = mockMvc.perform(post("/api/exams/" + EXAM_ID + "/submit").cookie(DEVICE_COOKIE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"answers": [{"questionNo": 1, "selectedOptionNo": 2}]}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(readObject(json, "$").keySet())
                    .containsExactlyInAnyOrder("examId", "questionCount", "correctCount", "score");
        }

        @Test
        @DisplayName("결과 응답 : 시험 결과 · 틀린 항목 칸")
        void result() throws Exception {
            given(examQuery.getResult(EXAM_ID)).willReturn(resultWithOneIncorrectAndOneTimeout());

            String json = mockMvc.perform(get("/api/exams/" + EXAM_ID + "/result"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(readObject(json, "$").keySet()).containsExactlyInAnyOrder(
                    "examId", "level", "groupName", "questionCount", "correctCount", "score",
                    "retentionDeadline", "retentionMinutes", "wrongItems");
            assertThat(readObject(json, "$.wrongItems[0]").keySet()).containsExactlyInAnyOrder(
                    "no", "prompt", "promptSub", "selectedText", "answerText", "headword");
        }

        @Test
        @DisplayName("오류 응답 : code · message · path · timestamp, 오류 코드 값")
        void error() throws Exception {
            given(examUseCase.deliverQuestions(EXAM_ID, DEVICE_ID))
                    .willThrow(new CustomException(ErrorCode.EXAM_NOT_IN_PROGRESS));

            String json = mockMvc.perform(get("/api/exams/" + EXAM_ID + "/questions").cookie(DEVICE_COOKIE))
                    .andExpect(status().isBadRequest())
                    .andReturn().getResponse().getContentAsString();

            assertThat(readObject(json, "$").keySet())
                    .containsExactlyInAnyOrder("code", "message", "path", "timestamp");
            assertThat((String) JsonPath.read(json, "$.code")).isEqualTo("EXAM_002");
        }

    }

    @Nested
    @DisplayName("화면 모델 이름 · 템플릿")
    class Page {

        @Test
        @DisplayName("메인 : levelGroups · questionCount · timePerQuestionSec · allowedMinutes, 레벨 카드를 그린다")
        void main() throws Exception {
            given(examLevelOverviewQuery.getExamLevelOverviews()).willReturn(List.of(
                    new ExamLevelOverviewGroup("Independent",
                            List.of(new ExamLevelOverview(Level.B2, 1234, 500, 45)))));

            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("main"))
                    .andExpect(model().attributeExists("levelGroups", "questionCount", "timePerQuestionSec",
                            "allowedMinutes"))
                    .andExpect(content().string(containsString(">Independent<")))
                    .andExpect(content().string(containsString("value=\"B2\"")))
                    .andExpect(content().string(containsString("1,234")));
        }

        @ParameterizedTest
        @ValueSource(strings = {"result-expired"})
        @DisplayName("메인 안내 코드 : 정해진 값이면 message 를 넣는다")
        void mainNotice(String notice) throws Exception {
            given(examLevelOverviewQuery.getExamLevelOverviews()).willReturn(List.of(
                    new ExamLevelOverviewGroup("Basic", List.of(new ExamLevelOverview(Level.A1, 1234, 500, 45)))));

            mockMvc.perform(get("/").param("notice", notice))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("message"));
        }

        @Test
        @DisplayName("준비 화면 : overview · groupName · timePerQuestionSec 를 data-* 로 그린다 (exam.js 가 읽음)")
        void ready() throws Exception {
            given(examLevelOverviewQuery.getExamLevelOverview(Level.A1))
                    .willReturn(new ExamLevelOverview(Level.A1, 1049, 500, 43));

            mockMvc.perform(get("/exam/ready").param("level", "A1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("exam"))
                    .andExpect(model().attributeExists("overview", "groupName", "timePerQuestionSec"))
                    .andExpect(content().string(containsString("data-level=\"A1\"")))
                    .andExpect(content().string(containsString("data-group-name=\"Basic\"")))
                    .andExpect(content().string(containsString("data-question-count=\"500\"")))
                    .andExpect(content().string(containsString("data-time-per-question-sec=\"5\"")))
                    .andExpect(content().string(containsString("data-allowed-minutes=\"43\"")));
        }

        @Test
        @DisplayName("결과 화면 : result · retentionDeadline, 템플릿 안 호출(오답 수 · 시간 초과 수 · 보관 시간 문구)을 그린다")
        void result() throws Exception {
            given(examQuery.findResult(EXAM_ID))
                    .willReturn(Optional.of(resultWithOneIncorrectAndOneTimeout()));

            String html = mockMvc.perform(get("/exam/" + EXAM_ID + "/result"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("result"))
                    .andExpect(model().attributeExists("result", "retentionDeadline"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(html)
                    .contains("C2 · Proficient")
                    .contains("1시간")
                    .contains("14:20")
                    .contains("abroad")
                    .contains("해외로")
                    .contains("국내에");
            assertThat(recordNumbers(html)).containsExactly("8", "1", "1");
        }

        /** 기록 카드의 숫자 (정답 · 오답 · 시간 초과 순) */
        private List<String> recordNumbers(String html) {
            return Pattern.compile("class=\"record-num\"[^>]*>([^<]*)<")
                    .matcher(html)
                    .results()
                    .map(m -> m.group(1).trim())
                    .toList();
        }

    }

}

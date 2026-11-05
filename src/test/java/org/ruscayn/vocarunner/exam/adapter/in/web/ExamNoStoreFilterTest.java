package org.ruscayn.vocarunner.exam.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("ExamNoStoreFilter : 시험 화면 · 시험 API만 브라우저가 저장하지 않게")
class ExamNoStoreFilterTest {

    private static String cacheControl(String uri) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new ExamNoStoreFilter().doFilter(request, response, new MockFilterChain());

        return response.getHeader("Cache-Control");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/exam/ready", "/api/exams", "/api/exams/abc/questions", "/api/exams/abc/result"})
    @DisplayName("준비 화면 · 시험 API 는 no-store")
    void noStore(String uri) throws Exception {
        assertThat(cacheControl(uri)).isEqualTo("no-store");
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/", "/about", "/exam/abc/result", "/css/exam.css", "/api/examsx"})
    @DisplayName("메인 · 결과 화면 · 정적 파일은 건드리지 않는다")
    void others(String uri) throws Exception {
        assertThat(cacheControl(uri)).isNull();
    }

}

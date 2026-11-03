package org.ruscayn.vocarunner.common.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OriginVerifyFilter : CloudFront 비밀 헤더 검사")
class OriginVerifyFilterTest {

    private static final String HEADER = "X-Origin-Verify";

    private static MockHttpServletResponse run(List<String> secrets, String remoteAddr, String headerValue) throws Exception {
        OriginVerifyFilter filter = new OriginVerifyFilter(new OriginVerifyProperties(HEADER, secrets));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr(remoteAddr);
        if (headerValue != null) {
            request.addHeader(HEADER, headerValue);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (chain.getRequest() == null) {
            assertThat(response.getStatus()).isEqualTo(403);
        }
        return response;
    }

    private static boolean passed(List<String> secrets, String remoteAddr, String headerValue) throws Exception {
        MockHttpServletResponse response = run(secrets, remoteAddr, headerValue);
        return response.getStatus() == 200;
    }

    @Test
    @DisplayName("허용 값이 없으면(local·자동 테스트) 헤더 없이도 통과")
    void disabled() throws Exception {
        assertThat(passed(List.of(), "10.0.0.9", null)).isTrue();
        assertThat(passed(null, "10.0.0.9", null)).isTrue();
    }

    @Test
    @DisplayName("값이 맞으면 통과, 없거나 틀리면 403")
    void verify() throws Exception {
        List<String> secrets = List.of("s3cret-value");
        assertThat(passed(secrets, "10.0.0.9", "s3cret-value")).isTrue();
        assertThat(passed(secrets, "10.0.0.9", null)).isFalse();
        assertThat(passed(secrets, "10.0.0.9", "")).isFalse();
        assertThat(passed(secrets, "10.0.0.9", "s3cret-valuX")).isFalse();
        assertThat(passed(secrets, "10.0.0.9", "s3cret-value-longer")).isFalse();
    }

    @Test
    @DisplayName("값을 바꾸는 동안 옛 값·새 값 둘 다 통과")
    void rotation() throws Exception {
        List<String> secrets = List.of("old-value", "new-value");
        assertThat(passed(secrets, "10.0.0.9", "old-value")).isTrue();
        assertThat(passed(secrets, "10.0.0.9", "new-value")).isTrue();
        assertThat(passed(secrets, "10.0.0.9", "other")).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"127.0.0.1", "0:0:0:0:0:0:0:1", "::1"})
    @DisplayName("서버 안에서 보낸 요청은 헤더 없이도 통과")
    void loopback(String remoteAddr) throws Exception {
        assertThat(passed(List.of("s3cret-value"), remoteAddr, null)).isTrue();
    }

    @Test
    @DisplayName("Parameter Store 의 쉼표 값은 목록으로 읽고, 앞뒤 공백·빈 값은 버린다")
    void bindCommaSeparated() {
        Binder binder = new Binder(new MapConfigurationPropertySource(Map.of(
                "voca-runner.origin-verify.header", HEADER,
                "voca-runner.origin-verify.secrets", "old-value, new-value,")));
        OriginVerifyProperties properties = binder.bind("voca-runner.origin-verify", OriginVerifyProperties.class).get();
        assertThat(properties.secrets()).containsExactly("old-value", "new-value");
        assertThat(properties.enabled()).isTrue();
    }

}

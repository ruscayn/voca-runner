package org.ruscayn.vocarunner.exam.adapter.in.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientIp : 사용자 IP (CloudFront 헤더)")
class ClientIpTest {

    private static final String HEADER = "CloudFront-Viewer-Address";

    private static MockHttpServletRequest request(String headerValue) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");   // CloudFront 주소 자리
        if (headerValue != null) {
            request.addHeader(HEADER, headerValue);
        }
        return request;
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "198.51.100.10:46532 | 198.51.100.10",
            "2001:db8::8a2e:370:7334:46532 | 2001:db8::8a2e:370:7334",
            "[2001:db8::1]:443 | 2001:db8::1",
            "' 198.51.100.10:46532 ' | 198.51.100.10"
    })
    @DisplayName("헤더가 있으면 포트를 뺀 IP (IPv6 는 마지막 ':' 앞까지)")
    void fromHeader(String headerValue, String expected) {
        assertThat(ClientIp.find(request(headerValue), HEADER)).isEqualTo(expected);
    }

    @Test
    @DisplayName("헤더가 없거나 비어 있으면 연결 IP")
    void noHeader() {
        assertThat(ClientIp.find(request(null), HEADER)).isEqualTo("10.0.0.9");
        assertThat(ClientIp.find(request(""), HEADER)).isEqualTo("10.0.0.9");
    }

    @Test
    @DisplayName("헤더 이름 설정이 비어 있으면(local) 헤더가 와도 보지 않는다")
    void headerOff() {
        assertThat(ClientIp.find(request("198.51.100.10:46532"), "")).isEqualTo("10.0.0.9");
        assertThat(ClientIp.find(request("198.51.100.10:46532"), null)).isEqualTo("10.0.0.9");
    }

}

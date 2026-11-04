package org.ruscayn.vocarunner.common.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 우리 CloudFront 를 거친 요청만 받는다 (AWS 설계.md 7-1 비밀 헤더).
 *
 * <p>8080 은 보안 그룹으로 CloudFront 주소만 허용하지만, 그 주소 목록은 다른 사람 계정의 CloudFront 도 포함한다.
 * 그래서 우리 CloudFront 가 원본 요청마다 붙이는 헤더 값이 맞을 때만 통과시키고, 없거나 틀리면 403 으로 끝낸다.
 * <ul>
 *   <li>허용 값이 비어 있으면 검사하지 않는다 (local·자동 테스트. 운영은 prod 프로필에서 Parameter Store 값으로 켠다)</li>
 *   <li>허용 값은 여럿일 수 있다 (값을 바꾸는 동안 옛 값·새 값 둘 다 허용)</li>
 *   <li>서버 안(127.0.0.1·::1)에서 보낸 요청은 통과 (서버에서 curl 로 확인할 때. 밖에서는 보안 그룹이 막는다)</li>
 * </ul>
 * 다른 필터보다 먼저 돈다. 설정 값은 이 필터가 직접 등록한다 (컨트롤러 테스트(@WebMvcTest)는 필터만 올리고 설정 값은 스캔하지 않음).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(OriginVerifyProperties.class)
public class OriginVerifyFilter extends OncePerRequestFilter {

    private static final Set<String> LOOPBACK = Set.of("127.0.0.1", "0:0:0:0:0:0:0:1", "::1");

    private final String header;
    private final List<byte[]> secrets;

    public OriginVerifyFilter(OriginVerifyProperties properties) {
        this.header = properties.header();
        this.secrets = properties.secrets().stream().map(s -> s.getBytes(StandardCharsets.UTF_8)).toList();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return secrets.isEmpty() || LOOPBACK.contains(request.getRemoteAddr());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (matches(request.getHeader(header))) {
            chain.doFilter(request, response);
            return;
        }
        log.warn("비밀 헤더가 없거나 틀려 거절 ip={} {} {}", request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);   // 본문 없이 (오류 화면도 보여 주지 않음)
    }

    /** 걸린 시간으로 값을 짐작하지 못하게 MessageDigest.isEqual 로 비교하고, 맞는 값을 찾아도 끝까지 비교 */
    private boolean matches(String value) {
        if (value == null) {
            return false;
        }
        byte[] given = value.getBytes(StandardCharsets.UTF_8);
        boolean ok = false;
        for (byte[] secret : secrets) {
            ok |= MessageDigest.isEqual(given, secret);
        }
        return ok;
    }

}

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
 * 보안 그룹은 8080 을 CloudFront 주소에만 열지만 그 목록에는 다른 사람 계정의 CloudFront 도 들어 있다.
 * 그래서 우리 CloudFront 가 원본 요청마다 붙이는 비밀 헤더 값이 맞을 때만 통과시킨다
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
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    private boolean matches(String value) {
        return value != null && matchesAnySecret(value.getBytes(StandardCharsets.UTF_8));
    }

    /** 걸린 시간으로 값을 짐작하지 못하게 MessageDigest.isEqual 로 비교하고, 맞는 값을 찾아도 끝까지 비교한다 */
    private boolean matchesAnySecret(byte[] given) {
        boolean ok = false;
        for (byte[] secret : secrets) {
            ok |= MessageDigest.isEqual(given, secret);
        }
        return ok;
    }

}

package org.ruscayn.vocarunner.presentation.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 모든 응답(화면·API·정적 파일·오류 화면)에 브라우저 보안 헤더를 붙인다 (U9).
 * <ul>
 *   <li>X-Frame-Options: 다른 사이트가 우리 화면을 iframe 으로 끼워 넣지 못하게 (투명하게 겹쳐 누르게 하는 속임수 방지)</li>
 *   <li>X-Content-Type-Options: 브라우저가 파일 종류를 추측해 스크립트로 실행하지 않게</li>
 *   <li>Referrer-Policy: 다른 사이트로 이동할 때 전체 주소(시험 id) 대신 사이트 주소만 보내게</li>
 *   <li>Strict-Transport-Security: HTTPS 로 들어온 요청에만. 이후 이 사이트는 HTTPS 로만 접속하게
 *       (프록시 뒤라면 server.forward-headers-strategy 를 켜야 HTTPS 로 인식)</li>
 * </ul>
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }
        chain.doFilter(request, response);
    }

}

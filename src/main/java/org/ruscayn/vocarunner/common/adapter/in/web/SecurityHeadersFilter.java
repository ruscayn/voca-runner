package org.ruscayn.vocarunner.common.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <ul>
 *   <li>X-Frame-Options: 다른 사이트가 iframe 으로 끼워 넣지 못하게 (투명하게 겹쳐 누르게 하는 속임수 방지)</li>
 *   <li>X-Content-Type-Options: 브라우저가 파일 종류를 추측해 스크립트로 실행하지 않게</li>
 *   <li>Referrer-Policy: 다른 사이트로 이동할 때 시험 id 가 든 전체 주소 대신 사이트 주소만 보내게</li>
 *   <li>Strict-Transport-Security: HTTPS 요청에만. 프록시 뒤라면 server.forward-headers-strategy 를 켜야 HTTPS 로 인식</li>
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

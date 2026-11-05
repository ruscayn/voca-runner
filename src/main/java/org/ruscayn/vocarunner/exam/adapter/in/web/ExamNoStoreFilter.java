package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 시험 화면 · 시험 API 응답을 브라우저가 저장하면 결과 화면에서 뒤로가기 할 때 옛 시험 화면을 그대로 꺼내
 * 끝난 시험을 다시 풀게 된다. no-store 면 뒤로가기도 서버로 와서 결과 화면이나 메인으로 보낼 수 있다
 */
@Component
public class ExamNoStoreFilter extends OncePerRequestFilter {

    private static final String READY_SCREEN = "/exam/ready";
    private static final String EXAM_API = "/api/exams";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.equals(READY_SCREEN) && !uri.equals(EXAM_API) && !uri.startsWith(EXAM_API + "/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        chain.doFilter(request, response);
    }

}

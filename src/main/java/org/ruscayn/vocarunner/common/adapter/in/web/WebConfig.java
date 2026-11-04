package org.ruscayn.vocarunner.common.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 시험 화면 · 시험 API 응답을 브라우저가 저장하면 결과 화면에서 뒤로가기 할 때 옛 시험 화면을 그대로 꺼내
 * 끝난 시험을 다시 풀게 된다. no-store 면 뒤로가기도 서버로 와서 결과 화면이나 메인으로 보낼 수 있다
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String EXAM_SCREEN = "/test/*";
    private static final String EXAM_API = "/api/tests/**";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new NoStoreInterceptor())
                .addPathPatterns(EXAM_SCREEN, EXAM_API);
    }

    static class NoStoreInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            return true;
        }
    }

}

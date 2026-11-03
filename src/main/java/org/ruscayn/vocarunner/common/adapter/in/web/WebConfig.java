package org.ruscayn.vocarunner.common.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 시험 화면·시험 API 응답을 브라우저가 저장하지 않게 한다 (Cache-Control: no-store).
 *
 * <p>저장해 두면 결과 화면에서 뒤로가기 할 때 서버에 묻지 않고 옛 시험 화면·문제 목록을 그대로 꺼내
 * 이미 끝난 시험을 처음부터 다시 풀게 된다 (B8). no-store 면 뒤로가기도 서버로 다시 요청하므로
 * 서버가 끝난 시험을 결과 화면이나 메인으로 보낼 수 있다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new NoStoreInterceptor())
                .addPathPatterns("/test/*", "/api/tests/**");   // /test/{id} (결과 화면 /test/{id}/result 는 제외)
    }

    static class NoStoreInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            return true;
        }
    }

}

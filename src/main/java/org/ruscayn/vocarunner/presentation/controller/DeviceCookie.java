package org.ruscayn.vocarunner.presentation.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.UUID;

/**
 * 기기 구분 쿠키 (VR_DEVICE, 무작위 값).
 * <ul>
 *   <li>같은 공인 IP를 쓰는 여러 사람(학교 와이파이·통신사 망)을 따로 센다 (U32)</li>
 *   <li>문제 받기·제출·만료는 시험을 시작한 기기만 할 수 있다 (U9)</li>
 * </ul>
 */
final class DeviceCookie {

    private static final String NAME = "VR_DEVICE";
    private static final Duration MAX_AGE = Duration.ofDays(365);

    private DeviceCookie() {
    }

    /** 쿠키의 기기 값. 없거나 형식이 다르면 null */
    static String find(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (NAME.equals(cookie.getName()) && isUuid(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /** 쿠키에 있으면 그대로, 없거나 형식이 다르면 새로 만들어 1년짜리 쿠키로 저장한다 */
    static String findOrCreate(HttpServletRequest request, HttpServletResponse response) {
        String found = find(request);
        if (found != null) {
            return found;
        }
        String deviceId = UUID.randomUUID().toString();
        ResponseCookie cookie = ResponseCookie.from(NAME, deviceId)
                .maxAge(MAX_AGE)
                .path("/")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return deviceId;
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return value.length() == 36;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}

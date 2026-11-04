package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

/**
 * 기기 구분 쿠키 (VR_DEVICE, 무작위 값).
 * <ul>
 *   <li>같은 공인 IP를 쓰는 여러 사람(학교 와이파이·통신사 망)을 따로 센다 (U32)</li>
 *   <li>문제 받기·제출·중단은 시험을 시작한 기기만 할 수 있다 (U9)</li>
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
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> NAME.equals(cookie.getName()) && isUuid(cookie.getValue()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
    }

    /** 쿠키에 있으면 그대로, 없거나 형식이 다르면 새로 만들어 1년짜리 쿠키로 저장한다 */
    static String findOrCreate(HttpServletRequest request, HttpServletResponse response) {
        String found = find(request);
        if (found != null) {
            return found;
        }
        String deviceId = UUID.randomUUID().toString();
        response.addHeader(HttpHeaders.SET_COOKIE, toCookie(deviceId, request.isSecure()).toString());
        return deviceId;
    }

    /** 1년짜리 기기 쿠키 (화면 스크립트가 읽지 못하고, 같은 사이트 요청에만 붙음) */
    private static ResponseCookie toCookie(String deviceId, boolean secure) {
        return ResponseCookie.from(NAME, deviceId)
                .maxAge(MAX_AGE)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .build();
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

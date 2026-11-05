package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

final class DeviceCookie {

    private static final String NAME = "VR_DEVICE";
    private static final Duration MAX_AGE = Duration.ofDays(1);

    private DeviceCookie() {
    }

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

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return value.length() == 36;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 이미 있어도 같은 값으로 다시 붙여, 쿠키가 시험 도중 만료되지 않게 기간을 시작 때부터 새로 센다 */
    static String findOrCreate(HttpServletRequest request, HttpServletResponse response) {
        String deviceId = Objects.requireNonNullElseGet(find(request), () -> UUID.randomUUID().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, toCookie(deviceId, request.isSecure()).toString());
        return deviceId;
    }

    private static ResponseCookie toCookie(String deviceId, boolean secure) {
        return ResponseCookie.from(NAME, deviceId)
                .maxAge(MAX_AGE)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .build();
    }

}

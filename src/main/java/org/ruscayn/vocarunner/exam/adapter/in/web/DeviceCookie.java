package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

final class DeviceCookie {

    private static final String NAME = "VR_DEVICE";
    private static final Duration MAX_AGE = Duration.ofDays(365);

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

    static String findOrCreate(HttpServletRequest request, HttpServletResponse response) {
        String found = find(request);
        if (found != null) {
            return found;
        }
        String deviceId = UUID.randomUUID().toString();
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

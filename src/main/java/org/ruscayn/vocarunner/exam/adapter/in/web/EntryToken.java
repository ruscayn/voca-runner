package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;

/** 시험 화면을 시작 직후 한 번만 열게 하는 1회용 값. 시작할 때 세션에 넣고, 시험 화면을 처음 열 때 쓰고 지운다 */
final class EntryToken {

    private static final String ENTRY_TOKEN = "ENTRY_TOKEN";
    private static final String LAST_OPENED_EXAM_ID = "LAST_OPENED_EXAM_ID";

    private EntryToken() {
    }

    static void issue(HttpSession httpSession, UUID examId) {
        httpSession.setAttribute(ENTRY_TOKEN, examId);
    }

    static boolean use(HttpServletRequest request, UUID examId) {
        HttpSession httpSession = existingSession(request);
        if (!examId.equals(attribute(httpSession, ENTRY_TOKEN))) {
            return false;
        }
        httpSession.removeAttribute(ENTRY_TOKEN);
        httpSession.setAttribute(LAST_OPENED_EXAM_ID, examId);
        return true;
    }

    static boolean isLastOpened(HttpServletRequest request, UUID examId) {
        return examId.equals(attribute(existingSession(request), LAST_OPENED_EXAM_ID));
    }

    /**
     * 세션을 새로 만들지 않는다. 쿠키 없는 요청(다른 기기 · 링크 미리보기 봇)에 세션을 만들면
     * redirect 주소에 ;jsessionid 가 붙어 404 원시 JSON 이 보인다
     */
    private static HttpSession existingSession(HttpServletRequest request) {
        return request.getSession(false);
    }

    private static Object attribute(HttpSession httpSession, String name) {
        return httpSession == null ? null : httpSession.getAttribute(name);
    }

}

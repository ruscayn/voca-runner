package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 운영은 CloudFront 뒤라 연결 IP 가 모두 CloudFront 주소이므로, CloudFront 가 넣는 CloudFront-Viewer-Address 헤더
 * ("IP:포트")에서 읽는다. 8080 에 직접 붙으면 아무 헤더나 보낼 수 있어 8080 은 CloudFront 만 열어 둔다
 */
final class ClientIp {

    private ClientIp() {
    }

    static String find(HttpServletRequest request, String headerName) {
        String value = headerValue(request, headerName);
        if (value == null || value.isBlank()) {
            return request.getRemoteAddr();
        }
        return withoutPort(value.trim());
    }

    private static String headerValue(HttpServletRequest request, String headerName) {
        if (headerName == null || headerName.isBlank()) {
            return null;
        }
        return request.getHeader(headerName);
    }

    /** IPv6 는 콜론이 여러 개라 마지막 ':' 앞까지, "[IPv6]:포트" 꼴이면 대괄호 안 */
    static String withoutPort(String address) {
        if (address.startsWith("[")) {
            int end = address.indexOf(']');
            return end > 0 ? address.substring(1, end) : address;
        }
        int colon = address.lastIndexOf(':');
        return colon > 0 ? address.substring(0, colon) : address;
    }

}

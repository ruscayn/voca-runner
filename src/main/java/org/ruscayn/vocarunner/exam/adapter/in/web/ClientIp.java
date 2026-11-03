package org.ruscayn.vocarunner.exam.adapter.in.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 사용자 IP (IP당 동시 진행 상한용, U32).
 *
 * <p>운영은 CloudFront 뒤라 연결 IP({@code getRemoteAddr()})가 모두 CloudFront 주소다.
 * 그래서 CloudFront 가 사용자 연결 IP로 직접 넣는 헤더({@code CloudFront-Viewer-Address}, 값 "IP:포트")에서 읽는다.
 * 서버(8080)에 직접 붙으면 아무 헤더나 보낼 수 있으므로 8080 은 CloudFront 만 접속하게 막는다 (infra/AWS 설계.md 8장).
 */
final class ClientIp {

    private ClientIp() {
    }

    /**
     * @param headerName 사용자 IP 가 든 헤더 이름 (voca-runner.client-ip.header). 비어 있으면 헤더를 보지 않는다 (local)
     * @return 헤더가 있으면 그 IP, 없으면 연결 IP
     */
    static String find(HttpServletRequest request, String headerName) {
        if (headerName != null && !headerName.isBlank()) {
            String value = request.getHeader(headerName);
            if (value != null && !value.isBlank()) {
                return withoutPort(value.trim());
            }
        }
        return request.getRemoteAddr();
    }

    /** "IP:포트" 에서 IP 만. IPv6 는 콜론이 여러 개라 마지막 ':' 앞까지 ("[IPv6]:포트" 꼴이면 대괄호 안) */
    static String withoutPort(String address) {
        if (address.startsWith("[")) {
            int end = address.indexOf(']');
            return end > 0 ? address.substring(1, end) : address;
        }
        int colon = address.lastIndexOf(':');
        return colon > 0 ? address.substring(0, colon) : address;
    }

}

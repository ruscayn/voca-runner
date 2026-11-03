package org.ruscayn.vocarunner.exam.domain;

/**
 * 응시자 = 시험을 시작한 쪽을 가르는 값 (기기 아이디 + 사용자 IP). 최대 진행 중 확인용.
 *
 * @param deviceId 기기(브라우저)마다 쿠키로 붙이는 무작위 값. 같은 공인 IP를 쓰는 여러 사람(학교·통신사)을 구분한다
 * @param clientIp 접속 IP. 쿠키를 지워 가며 시험을 계속 만드는 것을 막는 넉넉한 최대(IP당 최대 진행 중)에 쓴다
 */
public record Examinee(String deviceId, String clientIp) {
}

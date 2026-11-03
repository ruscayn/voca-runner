package org.ruscayn.vocarunner.exam.domain;

/**
 * 시험을 시작한 사용자 구분 값. 동시 진행 개수 제한용.
 *
 * @param deviceId 기기(브라우저)마다 쿠키로 붙이는 무작위 값. 같은 공인 IP를 쓰는 여러 사람(학교·통신사)을 구분한다
 * @param clientIp 접속 IP. 쿠키를 지워 가며 시험을 계속 만드는 것을 막는 넉넉한 상한에 쓴다
 */
public record Examinee(String deviceId, String clientIp) {
}

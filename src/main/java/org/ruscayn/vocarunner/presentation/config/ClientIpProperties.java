package org.ruscayn.vocarunner.presentation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 사용자 IP 를 읽는 방법 (application.yaml : voca-runner.client-ip.*)
 *
 * @param header 사용자 IP 가 든 요청 헤더 이름. 운영은 CloudFront 뒤라 CloudFront-Viewer-Address (값 "IP:포트").
 *               비우면 헤더를 보지 않고 연결 IP 를 쓴다 (local — CloudFront 없음)
 */
@ConfigurationProperties(prefix = "voca-runner.client-ip")
public record ClientIpProperties(String header) {
}

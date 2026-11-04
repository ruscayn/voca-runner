package org.ruscayn.vocarunner.common.adapter.in.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CloudFront 비밀 헤더 검사 (application.yaml : voca-runner.origin-verify.*, AWS 설계.md 7-1)
 *
 * @param header  CloudFront 가 원본 요청마다 붙이는 사용자 정의 헤더 이름
 * @param secrets 허용하는 헤더 값. 비어 있으면 검사하지 않는다 (local·자동 테스트).
 *                운영(prod 프로필)은 Parameter Store {@code /voca-runner/prod/voca-runner.origin-verify.secrets} 에서 채운다.
 *                값을 바꾸는 동안에는 쉼표로 옛 값·새 값을 함께 둔다
 */
@ConfigurationProperties(prefix = "voca-runner.origin-verify")
public record OriginVerifyProperties(String header, List<String> secrets) {

    public OriginVerifyProperties {
        secrets = secrets == null ? List.of()
                : secrets.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public boolean enabled() {
        return !secrets.isEmpty();
    }

}

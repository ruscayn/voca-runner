package org.ruscayn.vocarunner.common.adapter.in.web;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

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

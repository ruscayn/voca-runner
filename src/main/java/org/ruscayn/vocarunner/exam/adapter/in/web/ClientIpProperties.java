package org.ruscayn.vocarunner.exam.adapter.in.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voca-runner.client-ip")
public record ClientIpProperties(String header) {
}

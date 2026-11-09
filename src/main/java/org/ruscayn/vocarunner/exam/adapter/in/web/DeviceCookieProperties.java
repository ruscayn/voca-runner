package org.ruscayn.vocarunner.exam.adapter.in.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "voca-runner.device-cookie")
public record DeviceCookieProperties(boolean secure) {

}

package org.ruscayn.vocarunner.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.bootstrap.BootstrapRegistry;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

/**
 * prod 프로필이 시작할 때 Parameter Store /voca-runner/prod/ 값을 설정으로 읽는지 (application-prod.yaml).
 * AWS 대신 가짜 SsmClient 를 넣어 확인한다. 앱 본체(DB 등)는 띄우지 않고 설정 파일만 읽는다.
 */
@DisplayName("Parameter Store 설정 (prod 프로필)")
class ParameterStoreConfigTest {

    @Configuration(proxyBeanMethods = false)
    static class NoBeans {
    }

    /** 가짜 Parameter Store: 받은 요청을 기록하고 정해 둔 값을 돌려준다 */
    static class FakeSsm implements SsmClient {
        final List<GetParametersByPathRequest> requests = new ArrayList<>();

        @Override
        public GetParametersByPathResponse getParametersByPath(GetParametersByPathRequest request) {
            requests.add(request);
            return GetParametersByPathResponse.builder().parameters(
                    parameter("spring.datasource.username", "prod_user"),
                    parameter("spring.datasource.password", "prod-password"),
                    parameter("voca-runner.origin-verify.secrets", "old-value,new-value")).build();
        }

        private static Parameter parameter(String name, String value) {
            return Parameter.builder().name("/voca-runner/prod/" + name).value(value).build();
        }

        @Override
        public String serviceName() {
            return SERVICE_NAME;
        }

        @Override
        public void close() {
        }
    }

    private static ConfigurableApplicationContext start(FakeSsm ssm, String... profiles) {
        return new SpringApplicationBuilder(NoBeans.class)
                .web(WebApplicationType.NONE)
                .profiles(profiles)
                .addBootstrapRegistryInitializer(registry ->
                        registry.register(SsmClient.class, BootstrapRegistry.InstanceSupplier.of(ssm)))
                .run();
    }

    @Test
    @DisplayName("prod: /voca-runner/prod/ 값을 복호화해 읽고, secret.yml 의 DB 계정을 덮어쓴다")
    void prod() {
        FakeSsm ssm = new FakeSsm();
        try (ConfigurableApplicationContext context = start(ssm, "prod")) {
            assertThat(ssm.requests).isNotEmpty();
            assertThat(ssm.requests.getFirst().path()).isEqualTo("/voca-runner/prod/");
            assertThat(ssm.requests.getFirst().withDecryption()).isTrue();
            assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo("prod_user");
            assertThat(context.getEnvironment().getProperty("spring.datasource.password")).isEqualTo("prod-password");
            assertThat(context.getEnvironment().getProperty("voca-runner.origin-verify.secrets")).isEqualTo(
                    "old-value,new-value");
        }
    }

    @Test
    @DisplayName("프로필 없음(자동 테스트·운영 기준 값)·local: Parameter Store 에 묻지 않는다")
    void notProd() {
        FakeSsm ssm = new FakeSsm();
        try (ConfigurableApplicationContext context = start(ssm)) {
            assertThat(context.getEnvironment().getProperty("voca-runner.origin-verify.secrets")).isNull();
        }
        try (ConfigurableApplicationContext context = start(ssm, "local")) {
            assertThat(context.getEnvironment().getProperty("voca-runner.origin-verify.secrets")).isNull();
        }
        assertThat(ssm.requests).isEmpty();
    }

}

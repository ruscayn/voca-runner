package org.ruscayn.vocarunner.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * DB 를 쓰는 테스트가 함께 쓰는 임시 PostgreSQL. 테스트 클래스에 {@code @ImportTestcontainers(TestPostgres.class)} 를 붙인다.
 * 테스트 JVM 에서 한 번만 띄우고, Spring 이 캐시해 둔 테스트 컨텍스트가 닫힐 때(테스트가 모두 끝날 때) 내려간다.
 */
public interface TestPostgres {

    @ServiceConnection
    PostgreSQLContainer CONTAINER = new PostgreSQLContainer("postgres:18")
            .withInitScript("db/schema.sql");

}

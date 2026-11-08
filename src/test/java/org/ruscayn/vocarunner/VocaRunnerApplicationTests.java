package org.ruscayn.vocarunner;

import org.junit.jupiter.api.Test;
import org.ruscayn.vocarunner.support.TestPostgres;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@SpringBootTest
@ImportTestcontainers(TestPostgres.class)
class VocaRunnerApplicationTests {

    @Test
    void contextLoads() {
    }

}

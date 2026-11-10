package org.ruscayn.vocarunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class VocaRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocaRunnerApplication.class, args);
    }

}

package com.healthdata.demo.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.healthdata.demo.orchestrator",
    "com.healthdata.shared"
})
@EnableJpaRepositories
public class DemoOrchestratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoOrchestratorApplication.class, args);
    }
}

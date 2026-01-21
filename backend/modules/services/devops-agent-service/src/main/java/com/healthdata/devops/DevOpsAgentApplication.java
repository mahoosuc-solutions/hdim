package com.healthdata.devops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.healthdata.devops",
    "com.healthdata.shared"
})
@EnableJpaRepositories
public class DevOpsAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(DevOpsAgentApplication.class, args);
    }
}

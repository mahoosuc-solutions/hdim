package com.healthdata.costanalysis;

import com.healthdata.authentication.config.AuthenticationJwtAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    AuthenticationJwtAutoConfiguration.class
}, scanBasePackages = {
    "com.healthdata.costanalysis",
    "com.healthdata.common"
})
@EntityScan(basePackages = "com.healthdata.costanalysis.domain.model")
@EnableJpaRepositories(basePackages = "com.healthdata.costanalysis.domain.repository")
@EnableScheduling
public class CostAnalysisServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CostAnalysisServiceApplication.class, args);
    }
}

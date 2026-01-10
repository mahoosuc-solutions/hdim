package com.healthdata.gateway.fhir;

import com.healthdata.cache.CacheEvictionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@Import(CacheEvictionService.class)
@ComponentScan(basePackages = {
    "com.healthdata.gateway",
    "com.healthdata.authentication"
})
@EntityScan(basePackages = {
    "com.healthdata.authentication.domain",
    "com.healthdata.authentication.entity"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.authentication.repository"
})
public class GatewayFhirApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayFhirApplication.class, args);
    }
}

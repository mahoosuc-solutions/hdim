package com.healthdata.gateway.admin;

import com.healthdata.cache.CacheEvictionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.healthdata.gateway.admin.configversion.ConfigPromotionProperties;

@SpringBootApplication(exclude = {
    RedisAutoConfiguration.class,
    RedisRepositoriesAutoConfiguration.class
})
@Import(CacheEvictionService.class)
@ComponentScan(basePackages = {
    "com.healthdata.gateway",
    "com.healthdata.authentication",
    "com.healthdata.gateway.admin"
})
@EntityScan(basePackages = {
    "com.healthdata.authentication.domain",
    "com.healthdata.authentication.entity",
    "com.healthdata.gateway.admin.configversion"
})
@EnableJpaRepositories(basePackages = {
    "com.healthdata.authentication.repository",
    "com.healthdata.gateway.admin.configversion"
})
@EnableConfigurationProperties(ConfigPromotionProperties.class)
public class GatewayAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayAdminApplication.class, args);
    }
}

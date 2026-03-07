package com.healthdata.healthixadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableConfigurationProperties
@EnableScheduling
public class HealthixAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthixAdapterApplication.class, args);
    }
}

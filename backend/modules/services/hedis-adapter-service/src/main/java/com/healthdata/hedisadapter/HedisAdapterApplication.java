package com.healthdata.hedisadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableKafka
@EnableConfigurationProperties
@EnableScheduling
public class HedisAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(HedisAdapterApplication.class, args);
    }
}

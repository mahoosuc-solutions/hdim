package com.healthdata.corehiveadapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableConfigurationProperties
public class CorehiveAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorehiveAdapterApplication.class, args);
    }
}

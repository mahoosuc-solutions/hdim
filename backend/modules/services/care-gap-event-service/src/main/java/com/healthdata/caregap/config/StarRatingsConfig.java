package com.healthdata.caregap.config;

import com.healthdata.starrating.service.StarRatingCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StarRatingsConfig {

    @Bean
    public StarRatingCalculator starRatingCalculator() {
        return new StarRatingCalculator();
    }
}

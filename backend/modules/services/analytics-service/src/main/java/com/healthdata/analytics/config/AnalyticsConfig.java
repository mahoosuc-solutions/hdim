package com.healthdata.analytics.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableFeignClients(basePackages = "com.healthdata.analytics.client")
@EnableCaching
@EnableAsync
@EnableScheduling
public class AnalyticsConfig {
}

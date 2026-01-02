package com.healthdata.metrics;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for HDIM metrics infrastructure.
 *
 * Provides:
 * - JVM and system metrics (memory, GC, threads, CPU)
 * - @Timed annotation support for method timing
 * - Common tags for all metrics (service name, environment)
 * - Healthcare-specific metrics beans
 *
 * Metrics Endpoints:
 * - /actuator/prometheus - Prometheus scrape endpoint
 * - /actuator/metrics - Metrics browser
 *
 * Configuration:
 * - management.metrics.enabled: Enable/disable metrics (default: true)
 * - spring.application.name: Used as 'service' tag
 * - hdim.metrics.environment: Environment tag (default: development)
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(name = "management.metrics.enabled", havingValue = "true", matchIfMissing = true)
public class MetricsAutoConfiguration {

    @Value("${spring.application.name:hdim-service}")
    private String serviceName;

    @Value("${hdim.metrics.environment:development}")
    private String environment;

    /**
     * Enable @Timed annotation on methods for automatic timing.
     */
    @Bean
    @ConditionalOnMissingBean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * JVM memory metrics (heap, non-heap, buffers).
     */
    @Bean
    @ConditionalOnMissingBean
    public JvmMemoryMetrics jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    /**
     * JVM garbage collection metrics.
     */
    @Bean
    @ConditionalOnMissingBean
    public JvmGcMetrics jvmGcMetrics() {
        return new JvmGcMetrics();
    }

    /**
     * JVM thread metrics (count, states, daemon).
     */
    @Bean
    @ConditionalOnMissingBean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    /**
     * Class loader metrics.
     */
    @Bean
    @ConditionalOnMissingBean
    public ClassLoaderMetrics classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    /**
     * Processor/CPU metrics.
     */
    @Bean
    @ConditionalOnMissingBean
    public ProcessorMetrics processorMetrics() {
        return new ProcessorMetrics();
    }

    /**
     * Uptime metrics.
     */
    @Bean
    @ConditionalOnMissingBean
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }

    /**
     * Healthcare-specific business metrics collector.
     */
    @Bean
    @ConditionalOnMissingBean
    public HealthcareMetrics healthcareMetrics(MeterRegistry registry) {
        return new HealthcareMetrics(registry, serviceName);
    }

    /**
     * HTTP request metrics collector.
     */
    @Bean
    @ConditionalOnMissingBean
    public HttpMetrics httpMetrics(MeterRegistry registry) {
        return new HttpMetrics(registry, serviceName);
    }
}

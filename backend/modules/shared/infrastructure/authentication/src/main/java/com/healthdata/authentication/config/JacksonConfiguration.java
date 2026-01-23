package com.healthdata.authentication.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson Configuration for Java 8 Date/Time Support
 *
 * Explicitly registers the JSR310 module to enable serialization of:
 * - java.time.Instant
 * - java.time.LocalDate
 * - java.time.LocalDateTime
 * - java.time.ZonedDateTime
 *
 * Why this is needed:
 * - Spring Boot auto-configuration normally handles this
 * - However, in shared modules loaded via component scanning, explicit
 *   configuration ensures the JavaTimeModule is properly registered
 * - Without this, error responses with Instant timestamps fail serialization
 *
 * Formats:
 * - Instant: ISO-8601 format (e.g., "2024-01-21T10:15:30Z")
 * - LocalDate: ISO-8601 format (e.g., "2024-01-21")
 * - LocalDateTime: ISO-8601 format (e.g., "2024-01-21T10:15:30")
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Configure ObjectMapper with Java 8 date/time support.
     *
     * This bean customizes the default ObjectMapper used by Spring MVC
     * for JSON serialization/deserialization.
     *
     * Key configurations:
     * - JavaTimeModule: Handles java.time.* types
     * - WRITE_DATES_AS_TIMESTAMPS=false: Use ISO-8601 strings (not epoch millis)
     *
     * @param builder The Spring-provided ObjectMapper builder
     * @return Configured ObjectMapper
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();

        // Register Java 8 date/time module (JSR310)
        objectMapper.registerModule(new JavaTimeModule());

        // Use ISO-8601 strings for dates, not epoch milliseconds
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return objectMapper;
    }
}

package com.healthdata.featureflags;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enable Feature Flags
 *
 * Add this annotation to your Spring Boot application class to enable
 * feature flag infrastructure.
 *
 * Usage:
 * <pre>
 * @SpringBootApplication
 * @EnableFeatureFlags
 * public class NotificationServiceApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(NotificationServiceApplication.class, args);
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(FeatureFlagAutoConfiguration.class)
public @interface EnableFeatureFlags {
}

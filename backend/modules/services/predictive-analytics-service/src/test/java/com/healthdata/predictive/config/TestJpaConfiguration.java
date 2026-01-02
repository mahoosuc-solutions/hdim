package com.healthdata.predictive.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

/**
 * Test JPA Configuration
 *
 * Provides mock EntityManagerFactory bean for tests
 * to avoid needing full JPA infrastructure
 */
@TestConfiguration
@Profile("test")
public class TestJpaConfiguration {

    @Bean
    @Primary
    public EntityManagerFactory entityManagerFactory() {
        return mock(EntityManagerFactory.class);
    }

    @Bean(name = "jpaSharedEM_entityManagerFactory")
    public EntityManagerFactory jpaSharedEM_entityManagerFactory() {
        return mock(EntityManagerFactory.class);
    }
}

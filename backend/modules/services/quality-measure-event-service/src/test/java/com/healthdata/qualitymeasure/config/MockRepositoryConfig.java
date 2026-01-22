package com.healthdata.qualitymeasure.config;

import com.healthdata.qualitymeasure.persistence.CohortMeasureRateRepository;
import com.healthdata.qualitymeasure.persistence.MeasureEvaluationRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

/**
 * Test configuration providing mocked repositories
 *
 * Use with @Import(MockRepositoryConfig.class) in tests
 *
 * ★ Insight ─────────────────────────────────────
 * This configuration enables tests to run without database dependencies:
 *
 * 1. **@TestConfiguration** - Lightweight Spring test context
 * 2. **@Primary** - Overrides real repository beans
 * 3. **Mock instances** - Controlled behavior via Mockito
 *
 * Use this for:
 * - Service layer tests (focus on business logic)
 * - Controller tests (focus on HTTP handling)
 * - Integration tests that don't need real persistence
 *
 * Do NOT use this for:
 * - Repository integration tests (need real JPA)
 * - Entity-migration validation tests
 * ─────────────────────────────────────────────────
 */
@TestConfiguration
public class MockRepositoryConfig {

    /**
     * Provides mocked MeasureEvaluationRepository
     *
     * Tests must configure mock behavior with when(...).thenReturn(...)
     */
    @Bean
    @Primary
    public MeasureEvaluationRepository mockEvaluationRepository() {
        return mock(MeasureEvaluationRepository.class);
    }

    /**
     * Provides mocked CohortMeasureRateRepository
     *
     * Tests must configure mock behavior with when(...).thenReturn(...)
     */
    @Bean
    @Primary
    public CohortMeasureRateRepository mockCohortRepository() {
        return mock(CohortMeasureRateRepository.class);
    }
}

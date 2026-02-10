package com.healthdata.demo.strategy;

import com.healthdata.demo.application.DemoSeedingService;
import com.healthdata.demo.application.DemoSeedingService.GenerationResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class MultiTenantStrategyTest {

    @Test
    void seedScenarioWithOverrides_RejectsInvalidPatientCount() {
        DemoSeedingService seedingService = Mockito.mock(DemoSeedingService.class);
        MultiTenantStrategy strategy = new MultiTenantStrategy(seedingService, 200, 30);

        ScenarioSeedingStrategy.SeedingResult result = strategy.seedScenarioWithOverrides(0, 30);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("patientsPerTenant");
    }

    @Test
    void seedScenarioWithOverrides_RejectsInvalidCareGapPercentage() {
        DemoSeedingService seedingService = Mockito.mock(DemoSeedingService.class);
        MultiTenantStrategy strategy = new MultiTenantStrategy(seedingService, 200, 30);

        ScenarioSeedingStrategy.SeedingResult result = strategy.seedScenarioWithOverrides(100, 120);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("careGapPercentage");
    }

    @Test
    void seedScenarioWithOverrides_UsesOverridesWhenProvided() {
        DemoSeedingService seedingService = Mockito.mock(DemoSeedingService.class);
        MultiTenantStrategy strategy = new MultiTenantStrategy(seedingService, 200, 30);

        GenerationResult gen = new GenerationResult();
        gen.setSuccess(true);
        gen.setPatientCount(10);
        gen.setCareGapCount(3);
        when(seedingService.generatePatientCohort(anyInt(), anyString(), anyInt())).thenReturn(gen);

        ScenarioSeedingStrategy.SeedingResult result = strategy.seedScenarioWithOverrides(10, 40);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPatientsCreated()).isGreaterThan(0);
    }
}

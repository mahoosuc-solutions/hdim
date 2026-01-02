package com.healthdata.quality.persistence;

import com.healthdata.quality.config.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@BaseIntegrationTest
@Sql(scripts = "classpath:test-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CustomMeasureRepositoryTest {

    @Autowired
    private CustomMeasureRepository repository;

    @Test
    void shouldSaveAndFetchByTenantAndStatus() {
        CustomMeasureEntity draft = CustomMeasureEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("test-tenant")
                .name("TEST_MEASURE")
                .version("1.0.0")
                .status("DRAFT")
                .createdBy("test")
                .build();

        repository.save(draft);

        List<CustomMeasureEntity> drafts = repository.findByTenantIdAndStatusOrderByCreatedAtDesc("test-tenant", "DRAFT");
        assertThat(drafts).hasSize(1);
        assertThat(drafts.get(0).getName()).isEqualTo("TEST_MEASURE");
    }

    @Test
    void shouldFindByIdAndTenant() {
        CustomMeasureEntity draft = CustomMeasureEntity.builder()
                .id(UUID.randomUUID())
                .tenantId("test-tenant")
                .name("TEST_MEASURE")
                .version("1.0.0")
                .status("DRAFT")
                .createdBy("test")
                .build();

        CustomMeasureEntity saved = repository.save(draft);
        CustomMeasureEntity found = repository.findByTenantIdAndId("test-tenant", saved.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
    }
}

package com.healthdata.quality.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.persistence.CustomMeasureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for custom measure batch operations.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CustomMeasureBatchApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomMeasureRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT_ID = "other-tenant";

    private UUID draftMeasure1Id;
    private UUID draftMeasure2Id;
    private UUID publishedMeasureId;
    private UUID otherTenantMeasureId;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Create draft measures for tenant
        CustomMeasureEntity draft1 = CustomMeasureEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .name("Draft Measure 1")
                .version("1.0.0")
                .status("DRAFT")
                .description("Test draft measure 1")
                .category("CUSTOM")
                .year(2024)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .build();
        draftMeasure1Id = repository.save(draft1).getId();

        CustomMeasureEntity draft2 = CustomMeasureEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .name("Draft Measure 2")
                .version("1.0.0")
                .status("DRAFT")
                .description("Test draft measure 2")
                .category("CUSTOM")
                .year(2024)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .build();
        draftMeasure2Id = repository.save(draft2).getId();

        // Create already published measure
        CustomMeasureEntity published = CustomMeasureEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .name("Published Measure")
                .version("1.0.0")
                .status("PUBLISHED")
                .description("Test published measure")
                .category("CUSTOM")
                .year(2024)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .publishedDate(LocalDateTime.now())
                .build();
        publishedMeasureId = repository.save(published).getId();

        // Create measure for other tenant
        CustomMeasureEntity otherTenant = CustomMeasureEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(OTHER_TENANT_ID)
                .name("Other Tenant Measure")
                .version("1.0.0")
                .status("DRAFT")
                .description("Test measure for other tenant")
                .category("CUSTOM")
                .year(2024)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .build();
        otherTenantMeasureId = repository.save(otherTenant).getId();
    }

    @Test
    @DisplayName("Should batch publish draft measures successfully")
    void testBatchPublishDraftMeasures() throws Exception {
        Map<String, Object> request = Map.of(
                "measureIds", List.of(draftMeasure1Id.toString(), draftMeasure2Id.toString())
        );

        mockMvc.perform(post("/quality-measure/custom-measures/batch-publish")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0));

        // Verify measures are published
        CustomMeasureEntity updated1 = repository.findById(draftMeasure1Id).orElseThrow();
        assertThat(updated1.getStatus()).isEqualTo("PUBLISHED");
        assertThat(updated1.getPublishedDate()).isNotNull();

        CustomMeasureEntity updated2 = repository.findById(draftMeasure2Id).orElseThrow();
        assertThat(updated2.getStatus()).isEqualTo("PUBLISHED");
        assertThat(updated2.getPublishedDate()).isNotNull();
    }

    @Test
    @DisplayName("Should skip already published measures during batch publish")
    void testBatchPublishSkipsAlreadyPublished() throws Exception {
        Map<String, Object> request = Map.of(
                "measureIds", List.of(draftMeasure1Id.toString(), publishedMeasureId.toString())
        );

        mockMvc.perform(post("/quality-measure/custom-measures/batch-publish")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.failedCount").value(0));
    }

    @Test
    @DisplayName("Should enforce tenant isolation in batch publish")
    void testBatchPublishTenantIsolation() throws Exception {
        Map<String, Object> request = Map.of(
                "measureIds", List.of(otherTenantMeasureId.toString())
        );

        mockMvc.perform(post("/quality-measure/custom-measures/batch-publish")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify measure was not published
        CustomMeasureEntity unchanged = repository.findById(otherTenantMeasureId).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo("DRAFT");
        assertThat(unchanged.getPublishedDate()).isNull();
    }

    @Test
    @DisplayName("Should reject batch publish with empty measure IDs")
    void testBatchPublishEmptyList() throws Exception {
        Map<String, Object> request = Map.of("measureIds", List.of());

        mockMvc.perform(post("/quality-measure/custom-measures/batch-publish")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should batch delete measures successfully")
    void testBatchDeleteMeasures() throws Exception {
        Map<String, Object> request = Map.of(
                "measureIds", List.of(draftMeasure1Id.toString(), draftMeasure2Id.toString()),
                "force", false
        );

        mockMvc.perform(delete("/quality-measure/custom-measures/batch-delete")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deletedCount").value(2))
                .andExpect(jsonPath("$.failedCount").value(0));

        // Verify soft delete
        CustomMeasureEntity deleted1 = repository.findById(draftMeasure1Id).orElseThrow();
        assertThat(deleted1.getDeletedAt()).isNotNull();
        assertThat(deleted1.getDeletedBy()).isNotNull();

        CustomMeasureEntity deleted2 = repository.findById(draftMeasure2Id).orElseThrow();
        assertThat(deleted2.getDeletedAt()).isNotNull();
        assertThat(deleted2.getDeletedBy()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce tenant isolation in batch delete")
    void testBatchDeleteTenantIsolation() throws Exception {
        Map<String, Object> request = Map.of(
                "measureIds", List.of(otherTenantMeasureId.toString()),
                "force", false
        );

        mockMvc.perform(delete("/quality-measure/custom-measures/batch-delete")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // Verify measure was not deleted
        CustomMeasureEntity unchanged = repository.findById(otherTenantMeasureId).orElseThrow();
        assertThat(unchanged.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("Should reject batch delete with empty measure IDs")
    void testBatchDeleteEmptyList() throws Exception {
        Map<String, Object> request = Map.of(
                "measureIds", List.of(),
                "force", false
        );

        mockMvc.perform(delete("/quality-measure/custom-measures/batch-delete")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should delete single measure successfully")
    void testDeleteSingleMeasure() throws Exception {
        mockMvc.perform(delete("/quality-measure/custom-measures/{id}", draftMeasure1Id)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNoContent());

        // Verify soft delete
        CustomMeasureEntity deleted = repository.findById(draftMeasure1Id).orElseThrow();
        assertThat(deleted.getDeletedAt()).isNotNull();
        assertThat(deleted.getDeletedBy()).isNotNull();
    }

    @Test
    @DisplayName("Should enforce tenant isolation in single delete")
    void testDeleteSingleMeasureTenantIsolation() throws Exception {
        mockMvc.perform(delete("/quality-measure/custom-measures/{id}", otherTenantMeasureId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().is5xxServerError());

        // Verify measure was not deleted
        CustomMeasureEntity unchanged = repository.findById(otherTenantMeasureId).orElseThrow();
        assertThat(unchanged.getDeletedAt()).isNull();
    }
}

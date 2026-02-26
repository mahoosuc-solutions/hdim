package com.healthdata.quality.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.healthdata.quality.persistence.CustomMeasureEntity;
import com.healthdata.quality.service.CustomMeasureService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom Measure Controller Tests")
class CustomMeasureControllerTest {

    @Mock
    private CustomMeasureService customMeasureService;

    @InjectMocks
    private CustomMeasureController controller;

    @Test
    @DisplayName("Should create draft custom measure")
    void shouldCreateDraft() {
        CustomMeasureEntity entity = new CustomMeasureEntity();
        when(customMeasureService.createDraft(
            eq("tenant-1"), eq("Measure A"), eq("desc"), eq("HEDIS"), eq(2024),
            eq("Owner Team"), eq("Diabetes"), eq("MONTHLY"), eq("75%"),
            eq("HIGH"), eq("Rollout notes"), eq("diabetes,preventive"), eq("user-1")))
            .thenReturn(entity);

        CustomMeasureController.CreateCustomMeasureRequest request =
            new CustomMeasureController.CreateCustomMeasureRequest(
                "Measure A", "desc", "HEDIS", 2024,
                "Owner Team", "Diabetes", "MONTHLY", "75%",
                "HIGH", "Rollout notes", "diabetes,preventive", "user-1");

        var response = controller.createDraft("tenant-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(entity);
    }

    @Test
    @DisplayName("Should list measures with optional status")
    void shouldListMeasures() {
        List<CustomMeasureEntity> measures = List.of(new CustomMeasureEntity());
        when(customMeasureService.list("tenant-1", "draft")).thenReturn(measures);

        var response = controller.list("tenant-1", "draft");

        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Should fetch measure by id")
    void shouldGetById() {
        UUID id = UUID.randomUUID();
        CustomMeasureEntity measure = new CustomMeasureEntity();
        when(customMeasureService.getById("tenant-1", id)).thenReturn(measure);

        var response = controller.getById("tenant-1", id);

        assertThat(response.getBody()).isEqualTo(measure);
    }

    @Test
    @DisplayName("Should update draft measure")
    void shouldUpdateDraft() {
        UUID id = UUID.randomUUID();
        CustomMeasureEntity measure = new CustomMeasureEntity();
        when(customMeasureService.updateDraft(
            eq("tenant-1"), eq(id), eq("Measure B"), eq("desc2"), eq("CMS"), eq(2025),
            eq("Owner Team"), eq("Cardiology"), eq("QUARTERLY"), eq("80%"),
            eq("MEDIUM"), eq("Update notes"), eq("cardio,quality")))
            .thenReturn(measure);

        CustomMeasureController.UpdateCustomMeasureRequest request =
            new CustomMeasureController.UpdateCustomMeasureRequest(
                "Measure B", "desc2", "CMS", 2025,
                "Owner Team", "Cardiology", "QUARTERLY", "80%",
                "MEDIUM", "Update notes", "cardio,quality");

        var response = controller.updateDraft("tenant-1", id, request);

        assertThat(response.getBody()).isEqualTo(measure);
    }

    @Test
    @DisplayName("Should delete custom measure")
    void shouldDelete() {
        UUID id = UUID.randomUUID();

        var response = controller.delete("tenant-1", id);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(customMeasureService).delete("tenant-1", id, "clinical-portal");
    }

    @Test
    @DisplayName("Should batch publish measures")
    void shouldBatchPublish() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(customMeasureService.batchPublish("tenant-1", ids, "clinical-portal"))
            .thenReturn(new CustomMeasureService.BatchPublishResult(1, 1, 0, List.of()));

        CustomMeasureController.BatchPublishRequest request =
            new CustomMeasureController.BatchPublishRequest(ids);

        var response = controller.batchPublish("tenant-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().publishedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return bad request when batch publish fails")
    void shouldReturnBadRequestOnBatchPublishFailure() {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(customMeasureService.batchPublish(eq("tenant-1"), eq(ids), eq("clinical-portal")))
            .thenThrow(new IllegalArgumentException("invalid"));

        CustomMeasureController.BatchPublishRequest request =
            new CustomMeasureController.BatchPublishRequest(ids);

        var response = controller.batchPublish("tenant-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).contains("invalid");
    }

    @Test
    @DisplayName("Should batch delete measures")
    void shouldBatchDelete() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        when(customMeasureService.batchDelete("tenant-1", ids, "clinical-portal", true))
            .thenReturn(new CustomMeasureService.BatchDeleteResult(2, 0, List.of(), List.of()));

        CustomMeasureController.BatchDeleteRequest request =
            new CustomMeasureController.BatchDeleteRequest(ids, true);

        var response = controller.batchDelete("tenant-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().deletedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should return bad request when batch delete fails")
    void shouldReturnBadRequestOnBatchDeleteFailure() {
        List<UUID> ids = List.of(UUID.randomUUID());
        when(customMeasureService.batchDelete(eq("tenant-1"), eq(ids), eq("clinical-portal"), eq(false)))
            .thenThrow(new IllegalArgumentException("invalid-delete"));

        CustomMeasureController.BatchDeleteRequest request =
            new CustomMeasureController.BatchDeleteRequest(ids, false);

        var response = controller.batchDelete("tenant-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().errors()).contains("invalid-delete");
    }
}

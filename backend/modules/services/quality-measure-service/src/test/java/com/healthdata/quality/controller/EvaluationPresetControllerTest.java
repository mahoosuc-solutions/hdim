package com.healthdata.quality.controller;

import com.healthdata.quality.dto.EvaluationDefaultPresetDTO;
import com.healthdata.quality.dto.SaveEvaluationPresetRequest;
import com.healthdata.quality.persistence.EvaluationDefaultPresetEntity;
import com.healthdata.quality.service.EvaluationPresetService;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EvaluationPresetControllerTest {

    @Test
    void shouldReturnDefaultPresetWhenPresent() {
        EvaluationPresetService service = mock(EvaluationPresetService.class);
        EvaluationPresetController controller = new EvaluationPresetController(service);

        EvaluationDefaultPresetEntity entity = EvaluationDefaultPresetEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .userId("user-1")
            .measureId("measure-1")
            .patientId(UUID.randomUUID())
            .useCqlEngine(false)
            .createdAt(OffsetDateTime.now())
            .build();

        when(service.findDefaultPreset("tenant-1", "user-1"))
            .thenReturn(Optional.of(entity));

        var response = controller.getDefaultPreset("tenant-1", "user-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMeasureId()).isEqualTo("measure-1");
    }

    @Test
    void shouldReturnNotFoundWhenDefaultPresetMissing() {
        EvaluationPresetService service = mock(EvaluationPresetService.class);
        EvaluationPresetController controller = new EvaluationPresetController(service);

        when(service.findDefaultPreset("tenant-1", null))
            .thenReturn(Optional.empty());

        var response = controller.getDefaultPreset("tenant-1", null);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void shouldSaveDefaultPreset() {
        EvaluationPresetService service = mock(EvaluationPresetService.class);
        EvaluationPresetController controller = new EvaluationPresetController(service);

        SaveEvaluationPresetRequest request = new SaveEvaluationPresetRequest();
        request.setMeasureId("measure-2");
        request.setPatientId(UUID.randomUUID());
        request.setUseCqlEngine(true);

        EvaluationDefaultPresetEntity entity = EvaluationDefaultPresetEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .userId("user-1")
            .measureId("measure-2")
            .patientId(request.getPatientId())
            .useCqlEngine(true)
            .createdAt(OffsetDateTime.now())
            .build();

        when(service.saveDefaultPreset("tenant-1", "user-1", request))
            .thenReturn(entity);

        var response = controller.saveDefaultPreset("tenant-1", "user-1", request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        EvaluationDefaultPresetDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getMeasureId()).isEqualTo("measure-2");
        assertThat(body.getPatientId()).isEqualTo(request.getPatientId());
    }

    @Test
    void shouldClearDefaultPreset() {
        EvaluationPresetService service = mock(EvaluationPresetService.class);
        EvaluationPresetController controller = new EvaluationPresetController(service);

        var response = controller.clearDefaultPreset("tenant-1", "user-1");

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(service).clearDefaultPreset("tenant-1", "user-1");
    }
}

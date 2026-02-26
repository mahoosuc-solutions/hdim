package com.healthdata.quality.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Custom Measure Controller Request Tests")
class CustomMeasureControllerRequestTest {

    @Test
    @DisplayName("Should construct request and response records")
    void shouldConstructRequestAndResponseRecords() {
        CustomMeasureController.CreateCustomMeasureRequest create =
            new CustomMeasureController.CreateCustomMeasureRequest(
                "Measure A", "desc", "HEDIS", 2024,
                "Owner Team", "Diabetes", "MONTHLY", "75%",
                "HIGH", "Rollout notes", "diabetes,preventive", "user-1"
            );
        CustomMeasureController.UpdateCustomMeasureRequest update =
            new CustomMeasureController.UpdateCustomMeasureRequest(
                "Measure B", "desc2", "CMS", 2025,
                "Owner Team", "Cardiology", "QUARTERLY", "80%",
                "MEDIUM", "Update notes", "cardio,quality"
            );

        CustomMeasureController.BatchPublishRequest publishRequest =
            new CustomMeasureController.BatchPublishRequest(List.of(UUID.randomUUID()));
        CustomMeasureController.BatchDeleteRequest deleteRequest =
            new CustomMeasureController.BatchDeleteRequest(List.of(UUID.randomUUID()), true);

        CustomMeasureController.BatchPublishResponse publishResponse =
            new CustomMeasureController.BatchPublishResponse(1, 0, 0, List.of());
        CustomMeasureController.BatchDeleteResponse deleteResponse =
            new CustomMeasureController.BatchDeleteResponse(1, 0, List.of(), List.of());

        assertThat(create.name()).isEqualTo("Measure A");
        assertThat(create.owner()).isEqualTo("Owner Team");
        assertThat(update.category()).isEqualTo("CMS");
        assertThat(update.reportingCadence()).isEqualTo("QUARTERLY");
        assertThat(publishRequest.measureIds()).hasSize(1);
        assertThat(deleteRequest.force()).isTrue();
        assertThat(publishResponse.publishedCount()).isEqualTo(1);
        assertThat(deleteResponse.deletedCount()).isEqualTo(1);
    }
}

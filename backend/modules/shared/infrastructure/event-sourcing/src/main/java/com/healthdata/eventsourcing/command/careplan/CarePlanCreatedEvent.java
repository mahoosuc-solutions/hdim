package com.healthdata.eventsourcing.command.careplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.FhirResourceEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarePlanCreatedEvent extends FhirResourceEvent {
    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("patient_id")
    private String patientId;

    @JsonProperty("careplan_title")
    private String carePlanTitle;

    @JsonProperty("start_date")
    private LocalDate startDate;

    @JsonProperty("end_date")
    private LocalDate endDate;

    @JsonProperty("care_coordinator_id")
    private String careCoordinatorId;

    @JsonProperty("target_conditions")
    private List<String> targetConditions;

    @JsonProperty("goals")
    private List<String> goals;

    @Override
    public String getResourceType() {
        return "CarePlan";
    }

    @Override
    public String getEventType() {
        return "CarePlanCreated";
    }

    @Override
    public String getAggregateId() {
        return "patient-" + tenantId + "-" + patientId;
    }
}

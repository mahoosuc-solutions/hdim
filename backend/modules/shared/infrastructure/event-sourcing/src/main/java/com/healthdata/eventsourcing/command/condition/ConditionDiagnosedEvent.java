package com.healthdata.eventsourcing.command.condition;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.healthdata.eventsourcing.event.FhirResourceEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConditionDiagnosedEvent extends FhirResourceEvent {
    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("patient_id")
    private String patientId;

    @JsonProperty("icd_code")
    private String icdCode;

    @JsonProperty("onset_date")
    private LocalDate onsetDate;

    @JsonProperty("clinical_status")
    private String clinicalStatus;

    @JsonProperty("verification_status")
    private String verificationStatus;

    @Override
    public String getResourceType() {
        return "Condition";
    }

    @Override
    public String getEventType() {
        return "ConditionDiagnosed";
    }

    @Override
    public String getAggregateId() {
        return "patient-" + tenantId + "-" + patientId;
    }
}

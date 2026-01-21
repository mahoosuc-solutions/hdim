package com.healthdata.eventsourcing.command.condition;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.time.LocalDate;

@Getter
@ToString
@Builder
public class DiagnoseConditionCommand {
    private final String tenantId;
    private final String patientId;
    private final String icdCode;
    private final LocalDate onsetDate;
    private final String clinicalStatus;
    private final String verificationStatus;
}

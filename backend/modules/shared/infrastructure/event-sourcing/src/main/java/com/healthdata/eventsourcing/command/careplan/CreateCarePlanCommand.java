package com.healthdata.eventsourcing.command.careplan;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import java.time.LocalDate;
import java.util.List;

@Getter
@ToString
@Builder
public class CreateCarePlanCommand {
    private final String tenantId;
    private final String patientId;
    private final String carePlanTitle;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String careCoordinatorId;
    private final List<String> targetConditions;
    private final List<String> goals;
}

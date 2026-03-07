package com.healthdata.corehiveadapter.controller.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void scoreCareGapsRequest_validRequest_noViolations() {
        var request = ScoreCareGapsRequest.builder()
                .patientId("patient-123")
                .careGaps(List.of(ScoreCareGapsRequest.CareGapItem.builder()
                        .gapId("gap-1")
                        .measureId("BCS")
                        .build()))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void scoreCareGapsRequest_missingPatientId_hasViolation() {
        var request = ScoreCareGapsRequest.builder()
                .careGaps(List.of())
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("patientId"))).isTrue();
    }

    @Test
    void scoreCareGapsRequest_nullCareGaps_hasViolation() {
        var request = ScoreCareGapsRequest.builder()
                .patientId("patient-123")
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void calculateRoiRequest_validRequest_noViolations() {
        var request = CalculateRoiRequest.builder()
                .contractId("contract-abc")
                .totalLives(10000)
                .totalContractValue(new BigDecimal("5000000"))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void calculateRoiRequest_missingContractId_hasViolation() {
        var request = CalculateRoiRequest.builder()
                .totalLives(10000)
                .totalContractValue(new BigDecimal("5000000"))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void calculateRoiRequest_negativeLives_hasViolation() {
        var request = CalculateRoiRequest.builder()
                .contractId("contract-abc")
                .totalLives(-1)
                .totalContractValue(new BigDecimal("5000000"))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}

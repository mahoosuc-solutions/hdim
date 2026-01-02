package com.healthdata.consent.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("ConsentEntity")
class ConsentEntityTest {

    @Test
    @DisplayName("Should set defaults on create when missing")
    void shouldSetDefaultsOnCreate() {
        ConsentEntity entity = new ConsentEntity();
        entity.setProvisionType(null);

        ReflectionTestUtils.invokeMethod(entity, "onCreate");

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getLastModifiedAt()).isNotNull();
        assertThat(entity.getProvisionType()).isEqualTo("permit");
    }

    @Test
    @DisplayName("Should update lastModifiedAt on update")
    void shouldUpdateLastModifiedAt() {
        ConsentEntity entity = new ConsentEntity();
        entity.setLastModifiedAt(LocalDateTime.of(2020, 1, 1, 0, 0));

        ReflectionTestUtils.invokeMethod(entity, "onUpdate");

        assertThat(entity.getLastModifiedAt()).isAfter(LocalDateTime.of(2020, 1, 1, 0, 0));
    }

    @Test
    @DisplayName("Should report active consent when status and dates match")
    void shouldReportActiveConsent() {
        ConsentEntity entity = baseConsent();
        entity.setStatus("active");
        entity.setValidFrom(LocalDate.now().minusDays(1));
        entity.setValidTo(LocalDate.now().plusDays(1));

        assertThat(entity.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should report inactive consent when status is not active")
    void shouldReportInactiveWhenStatusMismatch() {
        ConsentEntity entity = baseConsent();
        entity.setStatus("revoked");

        assertThat(entity.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should report inactive consent when before valid from date")
    void shouldReportInactiveWhenBeforeValidFrom() {
        ConsentEntity entity = baseConsent();
        entity.setStatus("active");
        entity.setValidFrom(LocalDate.now().plusDays(1));

        assertThat(entity.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should report inactive consent when after valid to date")
    void shouldReportInactiveWhenAfterValidTo() {
        ConsentEntity entity = baseConsent();
        entity.setStatus("active");
        entity.setValidTo(LocalDate.now().minusDays(1));

        assertThat(entity.isActive()).isFalse();
    }

    @Test
    @DisplayName("Should report expired consent when validTo is past")
    void shouldReportExpiredConsent() {
        ConsentEntity entity = baseConsent();
        entity.setValidTo(LocalDate.now().minusDays(2));

        assertThat(entity.isExpired()).isTrue();
    }

    @Test
    @DisplayName("Should not report expired consent when no validTo")
    void shouldReportNotExpiredWhenNoValidTo() {
        ConsentEntity entity = baseConsent();
        entity.setValidTo(null);

        assertThat(entity.isExpired()).isFalse();
    }

    private ConsentEntity baseConsent() {
        return ConsentEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .scope("read")
            .status("active")
            .category("treatment")
            .provisionType("permit")
            .validFrom(LocalDate.now().minusDays(1))
            .consentDate(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .lastModifiedAt(LocalDateTime.now())
            .createdBy("tester")
            .lastModifiedBy("tester")
            .build();
    }
}

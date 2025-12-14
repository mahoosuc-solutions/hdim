package com.healthdata.priorauth.service;

import com.healthdata.priorauth.persistence.PriorAuthRequestEntity;
import com.healthdata.priorauth.persistence.PriorAuthRequestEntity.Urgency;
import com.healthdata.priorauth.persistence.PriorAuthRequestEntity.Status;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Prior Authorization SLA tracking functionality.
 * Tests SLA deadline calculation and breach detection per CMS-0057-F requirements.
 *
 * CMS SLA Requirements:
 * - STAT (urgent): 72 hours
 * - ROUTINE: 7 calendar days
 */
class SlaTrackingTest {

    @Nested
    @DisplayName("calculateSlaDeadline() tests")
    class CalculateSlaDeadlineTests {

        @Test
        @DisplayName("Should calculate 72-hour deadline for STAT urgency")
        void calculateSlaDeadline_withStatUrgency_shouldBe72Hours() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.STAT);
            LocalDateTime submittedAt = LocalDateTime.of(2024, 1, 15, 10, 0);
            entity.setSubmittedAt(submittedAt);

            // Act
            entity.calculateSlaDeadline();

            // Assert
            LocalDateTime expectedDeadline = submittedAt.plusHours(72);
            assertThat(entity.getSlaDeadline()).isEqualTo(expectedDeadline);
            assertThat(entity.getSlaDeadline()).isEqualTo(LocalDateTime.of(2024, 1, 18, 10, 0));
        }

        @Test
        @DisplayName("Should calculate 7-day deadline for ROUTINE urgency")
        void calculateSlaDeadline_withRoutineUrgency_shouldBe7Days() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.ROUTINE);
            LocalDateTime submittedAt = LocalDateTime.of(2024, 1, 15, 10, 0);
            entity.setSubmittedAt(submittedAt);

            // Act
            entity.calculateSlaDeadline();

            // Assert
            LocalDateTime expectedDeadline = submittedAt.plusDays(7);
            assertThat(entity.getSlaDeadline()).isEqualTo(expectedDeadline);
            assertThat(entity.getSlaDeadline()).isEqualTo(LocalDateTime.of(2024, 1, 22, 10, 0));
        }

        @Test
        @DisplayName("Should not set deadline when submittedAt is null")
        void calculateSlaDeadline_withNullSubmittedAt_shouldNotSetDeadline() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.STAT);
            entity.setSubmittedAt(null);

            // Act
            entity.calculateSlaDeadline();

            // Assert
            assertThat(entity.getSlaDeadline()).isNull();
        }

        @Test
        @DisplayName("Should not set deadline when urgency is null")
        void calculateSlaDeadline_withNullUrgency_shouldNotSetDeadline() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(null);
            entity.setSubmittedAt(LocalDateTime.now());

            // Act
            entity.calculateSlaDeadline();

            // Assert
            assertThat(entity.getSlaDeadline()).isNull();
        }

        @Test
        @DisplayName("Should handle weekend submissions correctly")
        void calculateSlaDeadline_withWeekendSubmission_shouldCalculateCorrectly() {
            // Arrange - Submit on Saturday
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.STAT);
            LocalDateTime saturdaySubmission = LocalDateTime.of(2024, 1, 13, 14, 0); // Saturday 2PM
            entity.setSubmittedAt(saturdaySubmission);

            // Act
            entity.calculateSlaDeadline();

            // Assert - Deadline is 72 hours later (Tuesday 2PM)
            assertThat(entity.getSlaDeadline()).isEqualTo(LocalDateTime.of(2024, 1, 16, 14, 0));
        }

        @Test
        @DisplayName("Should handle month boundary correctly")
        void calculateSlaDeadline_acrossMonthBoundary_shouldCalculateCorrectly() {
            // Arrange - Submit near end of month
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.ROUTINE);
            LocalDateTime endOfMonth = LocalDateTime.of(2024, 1, 29, 12, 0);
            entity.setSubmittedAt(endOfMonth);

            // Act
            entity.calculateSlaDeadline();

            // Assert - Deadline is 7 days later, into February
            assertThat(entity.getSlaDeadline()).isEqualTo(LocalDateTime.of(2024, 2, 5, 12, 0));
        }
    }

    @Nested
    @DisplayName("isSlaBreached() tests")
    class IsSlaBreachedTests {

        @Test
        @DisplayName("Should return false when SLA deadline is null")
        void isSlaBreached_withNullDeadline_shouldReturnFalse() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setSlaDeadline(null);

            // Act & Assert
            assertThat(entity.isSlaBreached()).isFalse();
        }

        @Test
        @DisplayName("Should return false when decision made before deadline")
        void isSlaBreached_withDecisionBeforeDeadline_shouldReturnFalse() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setStatus(Status.APPROVED);
            LocalDateTime deadline = LocalDateTime.of(2024, 1, 20, 12, 0);
            entity.setSlaDeadline(deadline);
            entity.setDecisionAt(deadline.minusHours(2)); // Decided 2 hours before deadline

            // Act & Assert
            assertThat(entity.isSlaBreached()).isFalse();
        }

        @Test
        @DisplayName("Should return true when decision made after deadline")
        void isSlaBreached_withDecisionAfterDeadline_shouldReturnTrue() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setStatus(Status.APPROVED);
            LocalDateTime deadline = LocalDateTime.of(2024, 1, 20, 12, 0);
            entity.setSlaDeadline(deadline);
            entity.setDecisionAt(deadline.plusHours(1)); // Decided 1 hour after deadline

            // Act & Assert
            assertThat(entity.isSlaBreached()).isTrue();
        }

        @Test
        @DisplayName("Should return true for pending request past deadline")
        void isSlaBreached_withPendingPastDeadline_shouldReturnTrue() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setStatus(Status.PENDING_SUBMISSION);
            entity.setSlaDeadline(LocalDateTime.now().minusHours(1)); // Deadline was 1 hour ago

            // Act & Assert
            assertThat(entity.isSlaBreached()).isTrue();
        }

        @Test
        @DisplayName("Should return false for pending request before deadline")
        void isSlaBreached_withPendingBeforeDeadline_shouldReturnFalse() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setStatus(Status.PENDING_SUBMISSION);
            entity.setSlaDeadline(LocalDateTime.now().plusDays(1)); // Deadline is tomorrow

            // Act & Assert
            assertThat(entity.isSlaBreached()).isFalse();
        }

        @ParameterizedTest
        @MethodSource("terminalStatusProvider")
        @DisplayName("Should check decisionAt for terminal statuses")
        void isSlaBreached_withTerminalStatus_shouldCheckDecisionAt(Status status) {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setStatus(status);
            LocalDateTime deadline = LocalDateTime.of(2024, 1, 20, 12, 0);
            entity.setSlaDeadline(deadline);
            entity.setDecisionAt(deadline.minusMinutes(30)); // Decided 30 min before deadline

            // Act & Assert
            assertThat(entity.isSlaBreached()).isFalse();
        }

        static Stream<Arguments> terminalStatusProvider() {
            return Stream.of(
                Arguments.of(Status.APPROVED),
                Arguments.of(Status.DENIED),
                Arguments.of(Status.PARTIALLY_APPROVED),
                Arguments.of(Status.CANCELLED)
            );
        }
    }

    @Nested
    @DisplayName("SLA compliance scenarios")
    class SlaComplianceScenarios {

        @Test
        @DisplayName("Scenario: STAT request approved within 72 hours")
        void scenario_statApprovedWithin72Hours_shouldBeCompliant() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.STAT);
            LocalDateTime submitted = LocalDateTime.of(2024, 1, 15, 9, 0);
            entity.setSubmittedAt(submitted);
            entity.calculateSlaDeadline();

            // Approved at 48 hours
            entity.setStatus(Status.APPROVED);
            entity.setDecisionAt(submitted.plusHours(48));

            // Assert
            assertThat(entity.isSlaBreached()).isFalse();
            assertThat(entity.getSlaDeadline()).isEqualTo(submitted.plusHours(72));
        }

        @Test
        @DisplayName("Scenario: STAT request denied at 73 hours (breached)")
        void scenario_statDeniedAt73Hours_shouldBeBreached() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.STAT);
            LocalDateTime submitted = LocalDateTime.of(2024, 1, 15, 9, 0);
            entity.setSubmittedAt(submitted);
            entity.calculateSlaDeadline();

            // Denied at 73 hours (1 hour late)
            entity.setStatus(Status.DENIED);
            entity.setDecisionAt(submitted.plusHours(73));

            // Assert
            assertThat(entity.isSlaBreached()).isTrue();
        }

        @Test
        @DisplayName("Scenario: ROUTINE request approved on day 6")
        void scenario_routineApprovedOnDay6_shouldBeCompliant() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.ROUTINE);
            LocalDateTime submitted = LocalDateTime.of(2024, 1, 15, 9, 0);
            entity.setSubmittedAt(submitted);
            entity.calculateSlaDeadline();

            // Approved on day 6
            entity.setStatus(Status.APPROVED);
            entity.setDecisionAt(submitted.plusDays(6));

            // Assert
            assertThat(entity.isSlaBreached()).isFalse();
        }

        @Test
        @DisplayName("Scenario: ROUTINE request still pending on day 8 (breached)")
        void scenario_routinePendingOnDay8_shouldBeBreached() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.ROUTINE);
            LocalDateTime submitted = LocalDateTime.now().minusDays(8);
            entity.setSubmittedAt(submitted);
            entity.calculateSlaDeadline();
            entity.setStatus(Status.SUBMITTED);

            // Assert - Still pending, past deadline
            assertThat(entity.isSlaBreached()).isTrue();
        }

        @Test
        @DisplayName("Scenario: Request cancelled before decision")
        void scenario_cancelledBeforeDecision_shouldCheckCancelTime() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.STAT);
            LocalDateTime submitted = LocalDateTime.of(2024, 1, 15, 9, 0);
            entity.setSubmittedAt(submitted);
            entity.calculateSlaDeadline();

            // Cancelled at 24 hours (well within SLA)
            entity.setStatus(Status.CANCELLED);
            entity.setDecisionAt(submitted.plusHours(24));

            // Assert
            assertThat(entity.isSlaBreached()).isFalse();
        }
    }

    @Nested
    @DisplayName("SLA deadline edge cases")
    class SlaDeadlineEdgeCases {

        @Test
        @DisplayName("Should handle exact deadline time correctly (not breached)")
        void exactDeadlineTime_withDecisionAtDeadline_shouldNotBeBreached() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setStatus(Status.APPROVED);
            LocalDateTime deadline = LocalDateTime.of(2024, 1, 20, 12, 0, 0);
            entity.setSlaDeadline(deadline);
            entity.setDecisionAt(deadline); // Exactly at deadline

            // Assert - Should NOT be breached (decision not AFTER deadline)
            assertThat(entity.isSlaBreached()).isFalse();
        }

        @Test
        @DisplayName("Should handle decision 1 second after deadline as breached")
        void oneSecondAfterDeadline_shouldBeBreached() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setStatus(Status.APPROVED);
            LocalDateTime deadline = LocalDateTime.of(2024, 1, 20, 12, 0, 0);
            entity.setSlaDeadline(deadline);
            entity.setDecisionAt(deadline.plusSeconds(1)); // 1 second late

            // Assert
            assertThat(entity.isSlaBreached()).isTrue();
        }

        @Test
        @DisplayName("Should handle leap year correctly")
        void leapYear_shouldCalculateCorrectly() {
            // Arrange
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.ROUTINE);
            LocalDateTime leapDay = LocalDateTime.of(2024, 2, 29, 10, 0); // 2024 is a leap year
            entity.setSubmittedAt(leapDay);

            // Act
            entity.calculateSlaDeadline();

            // Assert - 7 days from Feb 29 should be March 7
            assertThat(entity.getSlaDeadline()).isEqualTo(LocalDateTime.of(2024, 3, 7, 10, 0));
        }

        @Test
        @DisplayName("Should handle daylight saving time transition")
        void dstTransition_shouldCalculateCorrectly() {
            // Arrange - March 10, 2024 is DST start in US
            PriorAuthRequestEntity entity = createBaseEntity();
            entity.setUrgency(Urgency.STAT);
            LocalDateTime beforeDst = LocalDateTime.of(2024, 3, 9, 10, 0);
            entity.setSubmittedAt(beforeDst);

            // Act
            entity.calculateSlaDeadline();

            // Assert - 72 hours later (note: LocalDateTime doesn't account for DST)
            assertThat(entity.getSlaDeadline()).isEqualTo(LocalDateTime.of(2024, 3, 12, 10, 0));
        }
    }

    // Helper methods

    private PriorAuthRequestEntity createBaseEntity() {
        return PriorAuthRequestEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("test-tenant")
            .patientId(UUID.randomUUID())
            .paRequestId("PA-" + UUID.randomUUID().toString().substring(0, 8))
            .serviceCode("99213")
            .status(Status.DRAFT)
            .build();
    }
}

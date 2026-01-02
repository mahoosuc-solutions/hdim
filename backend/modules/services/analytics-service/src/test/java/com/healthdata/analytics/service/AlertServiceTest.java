package com.healthdata.analytics.service;

import com.healthdata.analytics.dto.AlertDto;
import com.healthdata.analytics.dto.KpiSummaryDto;
import com.healthdata.analytics.persistence.AlertRuleEntity;
import com.healthdata.analytics.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertService.
 * Tests alert rule CRUD operations and evaluation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Alert Service Tests")
class AlertServiceTest {

    @Mock
    private AlertRuleRepository alertRepository;

    @Mock
    private KpiService kpiService;

    private AlertService service;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final UUID ALERT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AlertService(alertRepository, kpiService);
    }

    @Nested
    @DisplayName("Get Alert Rules Tests")
    class GetAlertRulesTests {

        @Test
        @DisplayName("Should return all alert rules for tenant")
        void shouldReturnAllAlertRules() {
            // Given
            List<AlertRuleEntity> entities = List.of(
                    createAlertRule("High Quality Alert", "GT", BigDecimal.valueOf(90)),
                    createAlertRule("Low RAF Alert", "LT", BigDecimal.valueOf(1.0))
            );
            when(alertRepository.findByTenantId(TENANT_ID)).thenReturn(entities);

            // When
            List<AlertDto> result = service.getAlertRules(TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("High Quality Alert");
        }

        @Test
        @DisplayName("Should return empty list when no rules exist")
        void shouldReturnEmptyList() {
            // Given
            when(alertRepository.findByTenantId(TENANT_ID)).thenReturn(List.of());

            // When
            List<AlertDto> result = service.getAlertRules(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return paginated alert rules")
        void shouldReturnPaginatedRules() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            List<AlertRuleEntity> entities = List.of(createAlertRule("Alert 1", "GT", BigDecimal.valueOf(50)));
            Page<AlertRuleEntity> page = new PageImpl<>(entities, pageable, 1);
            when(alertRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);

            // When
            Page<AlertDto> result = service.getAlertRulesPaginated(TENANT_ID, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Get Single Alert Rule Tests")
    class GetSingleAlertRuleTests {

        @Test
        @DisplayName("Should return alert rule by ID")
        void shouldReturnAlertRuleById() {
            // Given
            AlertRuleEntity entity = createAlertRule("My Alert", "GT", BigDecimal.valueOf(75));
            when(alertRepository.findByIdAndTenantId(ALERT_ID, TENANT_ID))
                    .thenReturn(Optional.of(entity));

            // When
            Optional<AlertDto> result = service.getAlertRule(ALERT_ID, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("My Alert");
        }

        @Test
        @DisplayName("Should return empty when alert rule not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            when(alertRepository.findByIdAndTenantId(ALERT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<AlertDto> result = service.getAlertRule(ALERT_ID, TENANT_ID);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Active Alert Rules Tests")
    class GetActiveAlertRulesTests {

        @Test
        @DisplayName("Should return only active alert rules")
        void shouldReturnActiveRules() {
            // Given
            List<AlertRuleEntity> activeRules = List.of(
                    createAlertRule("Active Alert 1", "GT", BigDecimal.valueOf(80)),
                    createAlertRule("Active Alert 2", "LT", BigDecimal.valueOf(50))
            );
            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID)).thenReturn(activeRules);

            // When
            List<AlertDto> result = service.getActiveAlertRules(TENANT_ID);

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Create Alert Rule Tests")
    class CreateAlertRuleTests {

        @Test
        @DisplayName("Should create alert rule successfully")
        void shouldCreateAlertRule() {
            // Given
            AlertDto dto = AlertDto.builder()
                    .name("New Alert")
                    .description("Alert description")
                    .metricType("QUALITY_SCORE")
                    .metricName("Overall Score")
                    .conditionOperator("GT")
                    .thresholdValue(BigDecimal.valueOf(90))
                    .severity("HIGH")
                    .isActive(true)
                    .build();

            AlertRuleEntity savedEntity = createAlertRule("New Alert", "GT", BigDecimal.valueOf(90));
            when(alertRepository.save(any(AlertRuleEntity.class))).thenReturn(savedEntity);

            // When
            AlertDto result = service.createAlertRule(dto, TENANT_ID, USER_ID);

            // Then
            assertThat(result.getName()).isEqualTo("New Alert");
            verify(alertRepository).save(any(AlertRuleEntity.class));
        }

        @Test
        @DisplayName("Should set default values when not provided")
        void shouldSetDefaults() {
            // Given
            AlertDto dto = AlertDto.builder()
                    .name("Simple Alert")
                    .metricType("QUALITY_SCORE")
                    .conditionOperator("GT")
                    .thresholdValue(BigDecimal.valueOf(50))
                    .build();

            ArgumentCaptor<AlertRuleEntity> captor = ArgumentCaptor.forClass(AlertRuleEntity.class);
            when(alertRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createAlertRule(dto, TENANT_ID, USER_ID);

            // Then
            AlertRuleEntity saved = captor.getValue();
            assertThat(saved.getSeverity()).isEqualTo("MEDIUM");
            assertThat(saved.getCooldownMinutes()).isEqualTo(60);
            assertThat(saved.getIsActive()).isTrue();
        }

        @Test
        @DisplayName("Should set createdBy to current user")
        void shouldSetCreatedBy() {
            // Given
            AlertDto dto = AlertDto.builder()
                    .name("User Alert")
                    .metricType("RAF_SCORE")
                    .conditionOperator("LT")
                    .thresholdValue(BigDecimal.valueOf(1.0))
                    .build();

            ArgumentCaptor<AlertRuleEntity> captor = ArgumentCaptor.forClass(AlertRuleEntity.class);
            when(alertRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.createAlertRule(dto, TENANT_ID, USER_ID);

            // Then
            assertThat(captor.getValue().getCreatedBy()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("Update Alert Rule Tests")
    class UpdateAlertRuleTests {

        @Test
        @DisplayName("Should update alert rule successfully")
        void shouldUpdateAlertRule() {
            // Given
            AlertRuleEntity existing = createAlertRule("Old Name", "GT", BigDecimal.valueOf(50));
            AlertDto dto = AlertDto.builder()
                    .name("Updated Name")
                    .description("New Description")
                    .metricType("QUALITY_SCORE")
                    .conditionOperator("LT")
                    .thresholdValue(BigDecimal.valueOf(60))
                    .severity("CRITICAL")
                    .isActive(false)
                    .build();

            when(alertRepository.findByIdAndTenantId(ALERT_ID, TENANT_ID))
                    .thenReturn(Optional.of(existing));
            when(alertRepository.save(any(AlertRuleEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            Optional<AlertDto> result = service.updateAlertRule(ALERT_ID, dto, TENANT_ID);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should return empty when updating non-existent rule")
        void shouldReturnEmptyWhenUpdatingNonExistent() {
            // Given
            when(alertRepository.findByIdAndTenantId(ALERT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When
            Optional<AlertDto> result = service.updateAlertRule(ALERT_ID, new AlertDto(), TENANT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(alertRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Alert Rule Tests")
    class DeleteAlertRuleTests {

        @Test
        @DisplayName("Should delete alert rule successfully")
        void shouldDeleteAlertRule() {
            // Given
            when(alertRepository.existsByIdAndTenantId(ALERT_ID, TENANT_ID)).thenReturn(true);

            // When
            boolean result = service.deleteAlertRule(ALERT_ID, TENANT_ID);

            // Then
            assertThat(result).isTrue();
            verify(alertRepository).deleteById(ALERT_ID);
        }

        @Test
        @DisplayName("Should return false when rule not found")
        void shouldReturnFalseWhenNotFound() {
            // Given
            when(alertRepository.existsByIdAndTenantId(ALERT_ID, TENANT_ID)).thenReturn(false);

            // When
            boolean result = service.deleteAlertRule(ALERT_ID, TENANT_ID);

            // Then
            assertThat(result).isFalse();
            verify(alertRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Check Alerts Tests")
    class CheckAlertsTests {

        @Test
        @DisplayName("Should trigger alert when condition is met")
        void shouldTriggerAlertWhenConditionMet() {
            // Given
            AlertRuleEntity rule = createAlertRuleWithCooldown("Quality Alert", "GT",
                    BigDecimal.valueOf(80), null); // Never triggered before

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> qualityKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("QUALITY_SCORE")
                            .metricName("Overall Score")
                            .currentValue(BigDecimal.valueOf(95)) // Above threshold
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", qualityKpis,
                    "hcc", List.of(),
                    "careGaps", List.of()
            ));
            when(alertRepository.save(any(AlertRuleEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
            verify(alertRepository).save(any(AlertRuleEntity.class));
        }

        @Test
        @DisplayName("Should not trigger alert when condition is not met")
        void shouldNotTriggerWhenConditionNotMet() {
            // Given
            AlertRuleEntity rule = createAlertRuleWithCooldown("Quality Alert", "GT",
                    BigDecimal.valueOf(80), null);

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> qualityKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("QUALITY_SCORE")
                            .metricName("Overall Score")
                            .currentValue(BigDecimal.valueOf(75)) // Below threshold
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", qualityKpis,
                    "hcc", List.of(),
                    "careGaps", List.of()
            ));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).isEmpty();
            verify(alertRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should respect cooldown period")
        void shouldRespectCooldownPeriod() {
            // Given
            AlertRuleEntity rule = createAlertRuleWithCooldown("Quality Alert", "GT",
                    BigDecimal.valueOf(80), LocalDateTime.now().minusMinutes(30)); // Triggered 30 min ago, cooldown 60 min

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> qualityKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("QUALITY_SCORE")
                            .metricName("Overall Score")
                            .currentValue(BigDecimal.valueOf(95))
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", qualityKpis,
                    "hcc", List.of(),
                    "careGaps", List.of()
            ));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).isEmpty(); // Should be blocked by cooldown
        }

        @Test
        @DisplayName("Should trigger alert after cooldown expires")
        void shouldTriggerAfterCooldownExpires() {
            // Given
            AlertRuleEntity rule = createAlertRuleWithCooldown("Quality Alert", "GT",
                    BigDecimal.valueOf(80), LocalDateTime.now().minusMinutes(90)); // Triggered 90 min ago, cooldown 60 min

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> qualityKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("QUALITY_SCORE")
                            .metricName("Overall Score")
                            .currentValue(BigDecimal.valueOf(95))
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", qualityKpis,
                    "hcc", List.of(),
                    "careGaps", List.of()
            ));
            when(alertRepository.save(any(AlertRuleEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should increment trigger count when alert fires")
        void shouldIncrementTriggerCount() {
            // Given
            AlertRuleEntity rule = createAlertRuleWithCooldown("Quality Alert", "GT",
                    BigDecimal.valueOf(80), null);
            rule.setTriggerCount(5);

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> qualityKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("QUALITY_SCORE")
                            .metricName("Overall Score")
                            .currentValue(BigDecimal.valueOf(95))
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", qualityKpis,
                    "hcc", List.of(),
                    "careGaps", List.of()
            ));

            ArgumentCaptor<AlertRuleEntity> captor = ArgumentCaptor.forClass(AlertRuleEntity.class);
            when(alertRepository.save(captor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            service.checkAlerts(TENANT_ID);

            // Then
            assertThat(captor.getValue().getTriggerCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should evaluate LT condition correctly")
        void shouldEvaluateLTCondition() {
            // Given
            AlertRuleEntity rule = createAlertRuleWithCooldown("Low Quality Alert", "LT",
                    BigDecimal.valueOf(70), null);

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> qualityKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("QUALITY_SCORE")
                            .metricName("Overall Score")
                            .currentValue(BigDecimal.valueOf(65)) // Below threshold
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", qualityKpis,
                    "hcc", List.of(),
                    "careGaps", List.of()
            ));
            when(alertRepository.save(any(AlertRuleEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should evaluate EQ condition correctly")
        void shouldEvaluateEQCondition() {
            // Given
            AlertRuleEntity rule = createAlertRuleWithCooldown("Exact Alert", "EQ",
                    BigDecimal.valueOf(100), null);

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> qualityKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("QUALITY_SCORE")
                            .metricName("Overall Score")
                            .currentValue(BigDecimal.valueOf(100)) // Equals threshold
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", qualityKpis,
                    "hcc", List.of(),
                    "careGaps", List.of()
            ));
            when(alertRepository.save(any(AlertRuleEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should handle HCC metric type")
        void shouldHandleHccMetricType() {
            // Given
            AlertRuleEntity rule = createAlertRuleForHcc("RAF Alert", "LT", BigDecimal.valueOf(1.0));

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> hccKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("RAF_SCORE")
                            .metricName("Average RAF Score")
                            .currentValue(BigDecimal.valueOf(0.85)) // Below threshold
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", List.of(),
                    "hcc", hccKpis,
                    "careGaps", List.of()
            ));
            when(alertRepository.save(any(AlertRuleEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should handle care gap metric type")
        void shouldHandleCareGapMetricType() {
            // Given
            AlertRuleEntity rule = createAlertRuleForCareGap("Gap Alert", "GT", BigDecimal.valueOf(100));

            when(alertRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(rule));

            List<KpiSummaryDto> careGapKpis = List.of(
                    KpiSummaryDto.builder()
                            .metricType("CARE_GAP_RATE")
                            .metricName("Open Care Gaps")
                            .currentValue(BigDecimal.valueOf(150)) // Above threshold
                            .asOfDate(LocalDate.now())
                            .build()
            );

            when(kpiService.getAllKpis(TENANT_ID)).thenReturn(Map.of(
                    "quality", List.of(),
                    "hcc", List.of(),
                    "careGaps", careGapKpis
            ));
            when(alertRepository.save(any(AlertRuleEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // When
            List<AlertDto> result = service.checkAlerts(TENANT_ID);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Get Recently Triggered Alerts Tests")
    class GetRecentlyTriggeredAlertsTests {

        @Test
        @DisplayName("Should return recently triggered alerts")
        void shouldReturnRecentlyTriggeredAlerts() {
            // Given
            List<AlertRuleEntity> triggeredRules = List.of(
                    createAlertRuleWithCooldown("Alert 1", "GT", BigDecimal.valueOf(80),
                            LocalDateTime.now().minusHours(1)),
                    createAlertRuleWithCooldown("Alert 2", "LT", BigDecimal.valueOf(50),
                            LocalDateTime.now().minusMinutes(30))
            );

            when(alertRepository.findRecentlyTriggeredRules(eq(TENANT_ID), any(PageRequest.class)))
                    .thenReturn(triggeredRules);

            // When
            List<AlertDto> result = service.getRecentlyTriggeredAlerts(TENANT_ID, 10);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimit() {
            // Given
            when(alertRepository.findRecentlyTriggeredRules(eq(TENANT_ID), argThat(
                    pageRequest -> pageRequest.getPageSize() == 5)))
                    .thenReturn(List.of());

            // When
            service.getRecentlyTriggeredAlerts(TENANT_ID, 5);

            // Then
            verify(alertRepository).findRecentlyTriggeredRules(eq(TENANT_ID),
                    argThat(pageRequest -> pageRequest.getPageSize() == 5));
        }
    }

    // ==================== Helper Methods ====================

    private AlertRuleEntity createAlertRule(String name, String operator, BigDecimal threshold) {
        return AlertRuleEntity.builder()
                .id(ALERT_ID)
                .tenantId(TENANT_ID)
                .name(name)
                .metricType("QUALITY_SCORE")
                .metricName("Overall Score")
                .conditionOperator(operator)
                .thresholdValue(threshold)
                .severity("MEDIUM")
                .cooldownMinutes(60)
                .isActive(true)
                .triggerCount(0)
                .createdBy(USER_ID)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private AlertRuleEntity createAlertRuleWithCooldown(String name, String operator,
                                                        BigDecimal threshold, LocalDateTime lastTriggered) {
        AlertRuleEntity rule = createAlertRule(name, operator, threshold);
        rule.setLastTriggeredAt(lastTriggered);
        return rule;
    }

    private AlertRuleEntity createAlertRuleForHcc(String name, String operator, BigDecimal threshold) {
        AlertRuleEntity rule = createAlertRule(name, operator, threshold);
        rule.setMetricType("RAF_SCORE");
        rule.setMetricName("Average RAF Score");
        return rule;
    }

    private AlertRuleEntity createAlertRuleForCareGap(String name, String operator, BigDecimal threshold) {
        AlertRuleEntity rule = createAlertRule(name, operator, threshold);
        rule.setMetricType("CARE_GAP_RATE");
        rule.setMetricName("Open Care Gaps");
        return rule;
    }
}

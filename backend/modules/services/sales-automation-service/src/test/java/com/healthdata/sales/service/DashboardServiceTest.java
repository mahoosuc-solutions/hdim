package com.healthdata.sales.service;

import com.healthdata.sales.dto.*;
import com.healthdata.sales.entity.*;
import com.healthdata.sales.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private LeadService leadService;

    @Mock
    private OpportunityService opportunityService;

    @Mock
    private ActivityService activityService;

    @InjectMocks
    private DashboardService dashboardService;

    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("getDashboard")
    class GetDashboard {

        @Test
        @DisplayName("should return complete dashboard")
        void shouldReturnCompleteDashboard() {
            // Mock lead counts
            for (LeadStatus status : LeadStatus.values()) {
                when(leadRepository.countByTenantIdAndStatus(tenantId, status)).thenReturn(5L);
            }
            when(leadRepository.findRecentLeadsByStatus(eq(tenantId), eq(LeadStatus.NEW), any()))
                .thenReturn(Collections.emptyList());

            // Mock pipeline values
            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(new BigDecimal("500000"));
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(new BigDecimal("200000"));
            for (OpportunityStage stage : OpportunityStage.values()) {
                when(opportunityRepository.countByTenantIdAndStage(tenantId, stage)).thenReturn(3L);
            }

            // Mock activity counts
            when(activityRepository.countPendingActivities(tenantId)).thenReturn(10L);
            when(activityRepository.findCompletedActivitiesInRange(eq(tenantId), any(), any()))
                .thenReturn(Collections.emptyList());

            // Mock account counts
            for (AccountStage stage : AccountStage.values()) {
                when(accountRepository.countByTenantIdAndStage(tenantId, stage)).thenReturn(2L);
            }

            // Mock recent items
            when(leadService.findAll(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(opportunityService.findOpenOpportunities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(activityService.findUpcomingActivities(eq(tenantId), eq(7), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(activityService.findOverdueActivities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getLeads()).isNotNull();
            assertThat(result.getPipeline()).isNotNull();
            assertThat(result.getActivities()).isNotNull();
            assertThat(result.getAccounts()).isNotNull();
            assertThat(result.getRecent()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Lead Metrics")
    class LeadMetricsTests {

        @Test
        @DisplayName("should calculate total leads correctly")
        void shouldCalculateTotalLeadsCorrectly() {
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.NEW)).thenReturn(10L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.CONTACTED)).thenReturn(8L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.QUALIFIED)).thenReturn(6L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.CONVERTED)).thenReturn(4L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.UNQUALIFIED)).thenReturn(2L);
            when(leadRepository.findRecentLeadsByStatus(eq(tenantId), eq(LeadStatus.NEW), any()))
                .thenReturn(Collections.emptyList());

            // Mock other required data
            mockOtherDashboardData();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getLeads().getTotalLeads()).isEqualTo(30L); // 10+8+6+4+2
        }

        @Test
        @DisplayName("should calculate conversion rate correctly")
        void shouldCalculateConversionRateCorrectly() {
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.NEW)).thenReturn(0L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.CONTACTED)).thenReturn(0L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.QUALIFIED)).thenReturn(0L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.CONVERTED)).thenReturn(8L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.UNQUALIFIED)).thenReturn(2L);
            when(leadRepository.findRecentLeadsByStatus(eq(tenantId), eq(LeadStatus.NEW), any()))
                .thenReturn(Collections.emptyList());

            // Mock other required data
            mockOtherDashboardData();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            // Conversion rate = converted / (converted + unqualified) = 8 / (8 + 2) = 80%
            assertThat(result.getLeads().getConversionRate()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("should count leads by status")
        void shouldCountLeadsByStatus() {
            for (LeadStatus status : LeadStatus.values()) {
                when(leadRepository.countByTenantIdAndStatus(tenantId, status)).thenReturn(5L);
            }
            when(leadRepository.findRecentLeadsByStatus(eq(tenantId), eq(LeadStatus.NEW), any()))
                .thenReturn(Collections.emptyList());

            mockOtherDashboardData();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getLeads().getLeadsByStatus()).hasSize(LeadStatus.values().length);
            assertThat(result.getLeads().getLeadsByStatus().get(LeadStatus.NEW.name())).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Pipeline Metrics")
    class PipelineMetricsTests {

        @Test
        @DisplayName("should return pipeline values")
        void shouldReturnPipelineValues() {
            mockLeadData();

            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(new BigDecimal("750000"));
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(new BigDecimal("375000"));
            for (OpportunityStage stage : OpportunityStage.values()) {
                when(opportunityRepository.countByTenantIdAndStage(tenantId, stage)).thenReturn(5L);
            }

            mockActivityAndAccountData();
            mockRecentItems();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getPipeline().getTotalPipelineValue())
                .isEqualByComparingTo(new BigDecimal("750000"));
            assertThat(result.getPipeline().getWeightedPipelineValue())
                .isEqualByComparingTo(new BigDecimal("375000"));
        }

        @Test
        @DisplayName("should calculate win rate correctly")
        void shouldCalculateWinRateCorrectly() {
            mockLeadData();

            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);

            // 15 won, 5 lost = 75% win rate
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON)).thenReturn(15L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_LOST)).thenReturn(5L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DISCOVERY)).thenReturn(0L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DEMO)).thenReturn(0L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.PROPOSAL)).thenReturn(0L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.NEGOTIATION)).thenReturn(0L);

            mockActivityAndAccountData();
            mockRecentItems();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getPipeline().getWinRate()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("should count opportunities by stage")
        void shouldCountOpportunitiesByStage() {
            mockLeadData();

            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DISCOVERY)).thenReturn(10L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DEMO)).thenReturn(8L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.PROPOSAL)).thenReturn(5L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.NEGOTIATION)).thenReturn(3L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON)).thenReturn(2L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_LOST)).thenReturn(1L);

            mockActivityAndAccountData();
            mockRecentItems();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getPipeline().getOpportunitiesByStage().get(OpportunityStage.DISCOVERY.name())).isEqualTo(10L);
            assertThat(result.getPipeline().getOpportunitiesByStage().get(OpportunityStage.DEMO.name())).isEqualTo(8L);
        }

        @Test
        @DisplayName("should handle null pipeline values")
        void shouldHandleNullPipelineValues() {
            mockLeadData();

            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(null);
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(null);
            for (OpportunityStage stage : OpportunityStage.values()) {
                when(opportunityRepository.countByTenantIdAndStage(tenantId, stage)).thenReturn(0L);
            }

            mockActivityAndAccountData();
            mockRecentItems();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getPipeline().getTotalPipelineValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPipeline().getWeightedPipelineValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Activity Metrics")
    class ActivityMetricsTests {

        @Test
        @DisplayName("should return pending activities count")
        void shouldReturnPendingActivitiesCount() {
            mockLeadData();
            mockPipelineData();

            when(activityRepository.countPendingActivities(tenantId)).thenReturn(15L);
            when(activityRepository.findCompletedActivitiesInRange(eq(tenantId), any(), any()))
                .thenReturn(Collections.emptyList());

            mockAccountData();
            mockRecentItems();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getActivities().getPendingActivities()).isEqualTo(15L);
        }

        @Test
        @DisplayName("should count activities by type for completed this week")
        void shouldCountActivitiesByType() {
            mockLeadData();
            mockPipelineData();

            Activity callActivity = new Activity();
            callActivity.setActivityType(ActivityType.CALL);
            callActivity.setCompleted(true);

            Activity emailActivity = new Activity();
            emailActivity.setActivityType(ActivityType.EMAIL);
            emailActivity.setCompleted(true);

            when(activityRepository.countPendingActivities(tenantId)).thenReturn(0L);
            when(activityRepository.findCompletedActivitiesInRange(eq(tenantId), any(), any()))
                .thenReturn(List.of(callActivity, emailActivity, callActivity)); // 2 calls, 1 email

            mockAccountData();
            mockRecentItems();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getActivities().getCallsThisWeek()).isEqualTo(2L);
            assertThat(result.getActivities().getEmailsThisWeek()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("Account Metrics")
    class AccountMetricsTests {

        @Test
        @DisplayName("should count accounts by stage")
        void shouldCountAccountsByStage() {
            mockLeadData();
            mockPipelineData();
            mockActivityData();

            when(accountRepository.countByTenantIdAndStage(tenantId, AccountStage.PROSPECT)).thenReturn(10L);
            when(accountRepository.countByTenantIdAndStage(tenantId, AccountStage.QUALIFIED)).thenReturn(8L);
            when(accountRepository.countByTenantIdAndStage(tenantId, AccountStage.PROPOSAL)).thenReturn(3L);
            when(accountRepository.countByTenantIdAndStage(tenantId, AccountStage.CUSTOMER)).thenReturn(5L);
            when(accountRepository.countByTenantIdAndStage(tenantId, AccountStage.CHURNED)).thenReturn(2L);

            mockRecentItems();

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getAccounts().getTotalAccounts()).isEqualTo(28L); // 10+8+3+5+2
            assertThat(result.getAccounts().getActiveAccounts()).isEqualTo(26L); // Total - churned
            assertThat(result.getAccounts().getAccountsByStage().get(AccountStage.CUSTOMER.name())).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("Recent Items")
    class RecentItemsTests {

        @Test
        @DisplayName("should return recent leads")
        void shouldReturnRecentLeads() {
            mockLeadData();
            mockPipelineData();
            mockActivityData();
            mockAccountData();

            LeadDTO recentLead = LeadDTO.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .build();

            when(leadService.findAll(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(recentLead)));
            when(opportunityService.findOpenOpportunities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(activityService.findUpcomingActivities(eq(tenantId), eq(7), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(activityService.findOverdueActivities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getRecent().getRecentLeads()).hasSize(1);
            assertThat(result.getRecent().getRecentLeads().get(0).getFirstName()).isEqualTo("John");
        }

        @Test
        @DisplayName("should return recent opportunities")
        void shouldReturnRecentOpportunities() {
            mockLeadData();
            mockPipelineData();
            mockActivityData();
            mockAccountData();

            OpportunityDTO recentOpp = OpportunityDTO.builder()
                .id(UUID.randomUUID())
                .name("Big Deal")
                .amount(new BigDecimal("100000"))
                .build();

            when(leadService.findAll(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(opportunityService.findOpenOpportunities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(recentOpp)));
            when(activityService.findUpcomingActivities(eq(tenantId), eq(7), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(activityService.findOverdueActivities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getRecent().getRecentOpportunities()).hasSize(1);
            assertThat(result.getRecent().getRecentOpportunities().get(0).getName()).isEqualTo("Big Deal");
        }

        @Test
        @DisplayName("should return upcoming and overdue activities")
        void shouldReturnUpcomingAndOverdueActivities() {
            mockLeadData();
            mockPipelineData();
            mockActivityData();
            mockAccountData();

            ActivityDTO upcomingActivity = ActivityDTO.builder()
                .id(UUID.randomUUID())
                .subject("Demo call")
                .activityType(ActivityType.DEMO)
                .scheduledAt(LocalDateTime.now().plusDays(1))
                .build();

            ActivityDTO overdueActivity = ActivityDTO.builder()
                .id(UUID.randomUUID())
                .subject("Follow-up call")
                .activityType(ActivityType.CALL)
                .scheduledAt(LocalDateTime.now().minusDays(2))
                .build();

            when(leadService.findAll(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(opportunityService.findOpenOpportunities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(activityService.findUpcomingActivities(eq(tenantId), eq(7), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(upcomingActivity)));
            when(activityService.findOverdueActivities(eq(tenantId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(overdueActivity)));

            SalesDashboardDTO result = dashboardService.getDashboard(tenantId);

            assertThat(result.getRecent().getUpcomingActivities()).hasSize(1);
            assertThat(result.getRecent().getOverdueActivities()).hasSize(1);
        }
    }

    // Helper methods to reduce repetition

    private void mockOtherDashboardData() {
        mockPipelineData();
        mockActivityAndAccountData();
        mockRecentItems();
    }

    private void mockLeadData() {
        for (LeadStatus status : LeadStatus.values()) {
            when(leadRepository.countByTenantIdAndStatus(tenantId, status)).thenReturn(0L);
        }
        when(leadRepository.findRecentLeadsByStatus(eq(tenantId), eq(LeadStatus.NEW), any()))
            .thenReturn(Collections.emptyList());
    }

    private void mockPipelineData() {
        when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
        when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
        for (OpportunityStage stage : OpportunityStage.values()) {
            when(opportunityRepository.countByTenantIdAndStage(tenantId, stage)).thenReturn(0L);
        }
    }

    private void mockActivityData() {
        when(activityRepository.countPendingActivities(tenantId)).thenReturn(0L);
        when(activityRepository.findCompletedActivitiesInRange(eq(tenantId), any(), any()))
            .thenReturn(Collections.emptyList());
    }

    private void mockAccountData() {
        for (AccountStage stage : AccountStage.values()) {
            when(accountRepository.countByTenantIdAndStage(tenantId, stage)).thenReturn(0L);
        }
    }

    private void mockActivityAndAccountData() {
        mockActivityData();
        mockAccountData();
    }

    private void mockRecentItems() {
        when(leadService.findAll(eq(tenantId), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(opportunityService.findOpenOpportunities(eq(tenantId), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(activityService.findUpcomingActivities(eq(tenantId), eq(7), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(activityService.findOverdueActivities(eq(tenantId), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));
    }
}

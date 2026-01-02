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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private LeadRepository leadRepository;

    @InjectMocks
    private PipelineService pipelineService;

    private UUID tenantId;
    private UUID opportunityId;
    private UUID accountId;
    private Opportunity testOpportunity;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        opportunityId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testOpportunity = createTestOpportunity();
    }

    private Opportunity createTestOpportunity() {
        Opportunity opp = new Opportunity();
        opp.setId(opportunityId);
        opp.setTenantId(tenantId);
        opp.setAccountId(accountId);
        opp.setName("Enterprise Deal");
        opp.setAmount(new BigDecimal("100000"));
        opp.setStage(OpportunityStage.DISCOVERY);
        opp.setProbability(20);
        opp.setExpectedCloseDate(LocalDate.now().plusMonths(2));
        opp.setCreatedAt(LocalDateTime.now().minusDays(5));
        opp.setUpdatedAt(LocalDateTime.now());
        return opp;
    }

    @Nested
    @DisplayName("getKanbanView")
    class GetKanbanView {

        @Test
        @DisplayName("should return kanban view with all stages")
        void shouldReturnKanbanViewWithAllStages() {
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity));

            for (OpportunityStage stage : OpportunityStage.values()) {
                when(opportunityRepository.findByTenantIdAndStage(eq(tenantId), eq(stage), any(PageRequest.class)))
                    .thenReturn(stage == OpportunityStage.DISCOVERY ? opportunityPage : new PageImpl<>(Collections.emptyList()));
            }
            when(activityRepository.findByOpportunityId(any())).thenReturn(Collections.emptyList());

            PipelineKanbanDTO result = pipelineService.getKanbanView(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getColumns()).hasSize(OpportunityStage.values().length);
            assertThat(result.getSummary()).isNotNull();
        }

        @Test
        @DisplayName("should calculate pipeline summary correctly")
        void shouldCalculatePipelineSummaryCorrectly() {
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity));

            for (OpportunityStage stage : OpportunityStage.values()) {
                when(opportunityRepository.findByTenantIdAndStage(eq(tenantId), eq(stage), any(PageRequest.class)))
                    .thenReturn(stage == OpportunityStage.DISCOVERY ? opportunityPage : new PageImpl<>(Collections.emptyList()));
            }
            when(activityRepository.findByOpportunityId(any())).thenReturn(Collections.emptyList());

            PipelineKanbanDTO result = pipelineService.getKanbanView(tenantId);

            assertThat(result.getSummary().getOpenOpportunities()).isEqualTo(1);
            assertThat(result.getSummary().getTotalPipelineValue()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        @Test
        @DisplayName("should build kanban cards with account and contact info")
        void shouldBuildKanbanCardsWithRelatedInfo() {
            Account account = new Account();
            account.setId(accountId);
            account.setName("Test Healthcare ACO");

            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity));

            for (OpportunityStage stage : OpportunityStage.values()) {
                when(opportunityRepository.findByTenantIdAndStage(eq(tenantId), eq(stage), any(PageRequest.class)))
                    .thenReturn(stage == OpportunityStage.DISCOVERY ? opportunityPage : new PageImpl<>(Collections.emptyList()));
            }
            when(accountRepository.findByIdAndTenantId(accountId, tenantId)).thenReturn(Optional.of(account));
            when(activityRepository.findByOpportunityId(any())).thenReturn(Collections.emptyList());

            PipelineKanbanDTO result = pipelineService.getKanbanView(tenantId);

            PipelineKanbanDTO.KanbanColumn discoveryColumn = result.getColumns().stream()
                .filter(c -> c.getStage() == OpportunityStage.DISCOVERY)
                .findFirst()
                .orElse(null);

            assertThat(discoveryColumn).isNotNull();
            assertThat(discoveryColumn.getOpportunities()).hasSize(1);
            assertThat(discoveryColumn.getOpportunities().get(0).getAccountName()).isEqualTo("Test Healthcare ACO");
        }
    }

    @Nested
    @DisplayName("getForecast")
    class GetForecast {

        @Test
        @DisplayName("should return forecast with all periods")
        void shouldReturnForecastWithAllPeriods() {
            when(opportunityRepository.findByExpectedCloseDateRange(eq(tenantId), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(opportunityRepository.findByTenantIdAndStage(eq(tenantId), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(leadRepository.countByTenantIdAndStatus(eq(tenantId), any())).thenReturn(0L);
            when(opportunityRepository.countByTenantIdAndStage(eq(tenantId), any())).thenReturn(0L);
            when(opportunityRepository.findOpenOpportunities(eq(tenantId), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            PipelineForecastDTO result = pipelineService.getForecast(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getCurrentMonth()).isNotNull();
            assertThat(result.getCurrentQuarter()).isNotNull();
            assertThat(result.getCurrentYear()).isNotNull();
            assertThat(result.getMonthlyForecasts()).hasSize(6);
            assertThat(result.getFunnel()).isNotNull();
            assertThat(result.getHistorical()).isNotNull();
        }

        @Test
        @DisplayName("should calculate conversion funnel correctly")
        void shouldCalculateConversionFunnelCorrectly() {
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.NEW)).thenReturn(50L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.QUALIFIED)).thenReturn(30L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.CONVERTED)).thenReturn(20L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.CONTACTED)).thenReturn(10L);
            when(leadRepository.countByTenantIdAndStatus(tenantId, LeadStatus.UNQUALIFIED)).thenReturn(5L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DISCOVERY)).thenReturn(15L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.DEMO)).thenReturn(10L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.PROPOSAL)).thenReturn(8L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.NEGOTIATION)).thenReturn(5L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON)).thenReturn(12L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_LOST)).thenReturn(8L);

            when(opportunityRepository.findByExpectedCloseDateRange(eq(tenantId), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(opportunityRepository.findByTenantIdAndStage(eq(tenantId), eq(OpportunityStage.CLOSED_WON), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(opportunityRepository.findOpenOpportunities(eq(tenantId), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

            PipelineForecastDTO result = pipelineService.getForecast(tenantId);

            assertThat(result.getFunnel()).isNotNull();
            assertThat(result.getFunnel().getTotalLeads()).isGreaterThan(0);
            assertThat(result.getFunnel().getClosedWon()).isEqualTo(12L);
            assertThat(result.getFunnel().getOverallWinRate()).isEqualTo(60.0); // 12/(12+8) = 60%
        }
    }

    @Nested
    @DisplayName("moveToStage")
    class MoveToStage {

        @Test
        @DisplayName("should move opportunity to new stage")
        void shouldMoveOpportunityToNewStage() {
            StageTransitionRequest request = new StageTransitionRequest();
            request.setTargetStage(OpportunityStage.DEMO);
            request.setNextStep("Schedule product demo");

            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

            OpportunityDTO result = pipelineService.moveToStage(tenantId, opportunityId, request);

            assertThat(result).isNotNull();
            assertThat(testOpportunity.getStage()).isEqualTo(OpportunityStage.DEMO);
            assertThat(testOpportunity.getNextStep()).isEqualTo("Schedule product demo");
        }

        @Test
        @DisplayName("should set actual close date when moving to CLOSED_WON")
        void shouldSetActualCloseDateWhenClosedWon() {
            StageTransitionRequest request = new StageTransitionRequest();
            request.setTargetStage(OpportunityStage.CLOSED_WON);
            request.setFinalAmount(new BigDecimal("95000"));

            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

            pipelineService.moveToStage(tenantId, opportunityId, request);

            assertThat(testOpportunity.getStage()).isEqualTo(OpportunityStage.CLOSED_WON);
            assertThat(testOpportunity.getActualCloseDate()).isEqualTo(LocalDate.now());
            assertThat(testOpportunity.getAmount()).isEqualByComparingTo(new BigDecimal("95000"));
        }

        @Test
        @DisplayName("should set lost reason when moving to CLOSED_LOST")
        void shouldSetLostReasonWhenClosedLost() {
            StageTransitionRequest request = new StageTransitionRequest();
            request.setTargetStage(OpportunityStage.CLOSED_LOST);
            request.setLostReason(LostReason.COMPETITOR);
            request.setCompetitor("HealthQuality Systems");

            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

            pipelineService.moveToStage(tenantId, opportunityId, request);

            assertThat(testOpportunity.getStage()).isEqualTo(OpportunityStage.CLOSED_LOST);
            assertThat(testOpportunity.getLostReason()).isEqualTo(LostReason.COMPETITOR);
            assertThat(testOpportunity.getCompetitor()).isEqualTo("HealthQuality Systems");
        }

        @Test
        @DisplayName("should create follow-up activity when requested")
        void shouldCreateFollowUpActivityWhenRequested() {
            StageTransitionRequest request = new StageTransitionRequest();
            request.setTargetStage(OpportunityStage.DEMO);
            request.setCreateFollowUpTask(true);
            request.setFollowUpDays(5);
            request.setFollowUpTaskDescription("Prepare demo environment");

            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

            pipelineService.moveToStage(tenantId, opportunityId, request);

            verify(activityRepository).save(any(Activity.class));
        }

        @Test
        @DisplayName("should throw exception when opportunity not found")
        void shouldThrowExceptionWhenOpportunityNotFound() {
            StageTransitionRequest request = new StageTransitionRequest();
            request.setTargetStage(OpportunityStage.DEMO);

            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> pipelineService.moveToStage(tenantId, opportunityId, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Opportunity not found");
        }

        @Test
        @DisplayName("should throw exception when trying to reopen closed deal")
        void shouldThrowExceptionWhenReopeningClosedDeal() {
            testOpportunity.setStage(OpportunityStage.CLOSED_WON);
            StageTransitionRequest request = new StageTransitionRequest();
            request.setTargetStage(OpportunityStage.DISCOVERY);

            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));

            assertThatThrownBy(() -> pipelineService.moveToStage(tenantId, opportunityId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reopen");
        }
    }

    @Nested
    @DisplayName("getClosingSoon")
    class GetClosingSoon {

        @Test
        @DisplayName("should return opportunities closing within specified days")
        void shouldReturnOpportunitiesClosingSoon() {
            testOpportunity.setExpectedCloseDate(LocalDate.now().plusDays(5));
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity));

            when(opportunityRepository.findByExpectedCloseDateRange(eq(tenantId), any(), any(), any()))
                .thenReturn(opportunityPage);

            List<OpportunityDTO> result = pipelineService.getClosingSoon(tenantId, 7);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(opportunityId);
        }

        @Test
        @DisplayName("should exclude closed opportunities")
        void shouldExcludeClosedOpportunities() {
            Opportunity closedOpp = createTestOpportunity();
            closedOpp.setStage(OpportunityStage.CLOSED_WON);
            closedOpp.setExpectedCloseDate(LocalDate.now().plusDays(5));
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(closedOpp));

            when(opportunityRepository.findByExpectedCloseDateRange(eq(tenantId), any(), any(), any()))
                .thenReturn(opportunityPage);

            List<OpportunityDTO> result = pipelineService.getClosingSoon(tenantId, 7);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getStagnantOpportunities")
    class GetStagnantOpportunities {

        @Test
        @DisplayName("should return opportunities with no recent activity")
        void shouldReturnStagnantOpportunities() {
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity));

            when(opportunityRepository.findOpenOpportunities(eq(tenantId), any()))
                .thenReturn(opportunityPage);
            when(activityRepository.findByOpportunityId(opportunityId))
                .thenReturn(Collections.emptyList()); // No activities

            List<OpportunityDTO> result = pipelineService.getStagnantOpportunities(tenantId, 7);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("should exclude opportunities with recent activity")
        void shouldExcludeOpportunitiesWithRecentActivity() {
            Activity recentActivity = new Activity();
            recentActivity.setCreatedAt(LocalDateTime.now().minusDays(1)); // Very recent

            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity));

            when(opportunityRepository.findOpenOpportunities(eq(tenantId), any()))
                .thenReturn(opportunityPage);
            when(activityRepository.findByOpportunityId(opportunityId))
                .thenReturn(List.of(recentActivity));

            List<OpportunityDTO> result = pipelineService.getStagnantOpportunities(tenantId, 7);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("At-Risk Deals")
    class AtRiskDeals {

        @Test
        @DisplayName("should identify overdue deals as at-risk")
        void shouldIdentifyOverdueDealsAsAtRisk() {
            testOpportunity.setExpectedCloseDate(LocalDate.now().minusDays(5)); // Overdue
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity));

            when(opportunityRepository.findOpenOpportunities(eq(tenantId), any()))
                .thenReturn(opportunityPage);
            when(activityRepository.findByOpportunityId(any()))
                .thenReturn(Collections.emptyList());
            when(opportunityRepository.findByExpectedCloseDateRange(eq(tenantId), any(), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(opportunityRepository.findByTenantIdAndStage(eq(tenantId), any(), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
            when(leadRepository.countByTenantIdAndStatus(eq(tenantId), any())).thenReturn(0L);
            when(opportunityRepository.countByTenantIdAndStage(eq(tenantId), any())).thenReturn(0L);

            PipelineForecastDTO result = pipelineService.getForecast(tenantId);

            assertThat(result.getAtRiskDeals()).isNotEmpty();
            assertThat(result.getAtRiskDeals().get(0).getDaysOverdue()).isGreaterThan(0);
        }
    }
}

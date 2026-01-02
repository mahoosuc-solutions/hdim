package com.healthdata.sales.service;

import com.healthdata.sales.dto.OpportunityDTO;
import com.healthdata.sales.dto.PipelineMetricsDTO;
import com.healthdata.sales.entity.Opportunity;
import com.healthdata.sales.entity.OpportunityStage;
import com.healthdata.sales.repository.OpportunityRepository;
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
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OpportunityServiceTest {

    @Mock
    private OpportunityRepository opportunityRepository;

    @InjectMocks
    private OpportunityService opportunityService;

    private UUID tenantId;
    private UUID opportunityId;
    private UUID accountId;
    private Opportunity testOpportunity;
    private OpportunityDTO testOpportunityDTO;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        opportunityId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        testOpportunity = createTestOpportunity();
        testOpportunityDTO = createTestOpportunityDTO();
    }

    private Opportunity createTestOpportunity() {
        Opportunity opportunity = new Opportunity();
        opportunity.setId(opportunityId);
        opportunity.setTenantId(tenantId);
        opportunity.setAccountId(accountId);
        opportunity.setName("HDIM Enterprise License");
        opportunity.setDescription("Enterprise license deal for ACO");
        opportunity.setAmount(new BigDecimal("120000"));
        opportunity.setStage(OpportunityStage.DISCOVERY);
        opportunity.setProbability(20);
        opportunity.setExpectedCloseDate(LocalDate.now().plusMonths(3));
        opportunity.setProductTier("ENTERPRISE");
        opportunity.setContractLengthMonths(36);
        opportunity.setCreatedAt(LocalDateTime.now());
        opportunity.setUpdatedAt(LocalDateTime.now());
        return opportunity;
    }

    private OpportunityDTO createTestOpportunityDTO() {
        return OpportunityDTO.builder()
            .id(opportunityId)
            .tenantId(tenantId)
            .accountId(accountId)
            .name("HDIM Enterprise License")
            .description("Enterprise license deal for ACO")
            .amount(new BigDecimal("120000"))
            .stage(OpportunityStage.DISCOVERY)
            .probability(20)
            .expectedCloseDate(LocalDate.now().plusMonths(3))
            .productTier("ENTERPRISE")
            .contractLengthMonths(36)
            .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return opportunity when found")
        void shouldReturnOpportunityWhenFound() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));

            OpportunityDTO result = opportunityService.findById(tenantId, opportunityId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(opportunityId);
            assertThat(result.getName()).isEqualTo("HDIM Enterprise License");
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("120000"));
            verify(opportunityRepository).findByIdAndTenantId(opportunityId, tenantId);
        }

        @Test
        @DisplayName("should throw RuntimeException when not found")
        void shouldThrowExceptionWhenNotFound() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> opportunityService.findById(tenantId, opportunityId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Opportunity not found");
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated opportunities")
        void shouldReturnPaginatedOpportunities() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity), pageable, 1);

            when(opportunityRepository.findByTenantId(tenantId, pageable)).thenReturn(opportunityPage);

            Page<OpportunityDTO> result = opportunityService.findAll(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should create and return new opportunity")
        void shouldCreateAndReturnNewOpportunity() {
            when(opportunityRepository.save(any(Opportunity.class))).thenReturn(testOpportunity);

            OpportunityDTO result = opportunityService.create(tenantId, testOpportunityDTO);

            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("HDIM Enterprise License");
            verify(opportunityRepository).save(any(Opportunity.class));
        }

        @Test
        @DisplayName("should set default stage to DISCOVERY when not provided")
        void shouldSetDefaultStageWhenNotProvided() {
            OpportunityDTO dtoWithoutStage = OpportunityDTO.builder()
                .name("New Opportunity")
                .amount(new BigDecimal("50000"))
                .build();

            Opportunity savedOpportunity = new Opportunity();
            savedOpportunity.setId(UUID.randomUUID());
            savedOpportunity.setTenantId(tenantId);
            savedOpportunity.setName("New Opportunity");
            savedOpportunity.setStage(OpportunityStage.DISCOVERY);

            when(opportunityRepository.save(any(Opportunity.class))).thenReturn(savedOpportunity);

            OpportunityDTO result = opportunityService.create(tenantId, dtoWithoutStage);

            assertThat(result).isNotNull();
            assertThat(result.getStage()).isEqualTo(OpportunityStage.DISCOVERY);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update existing opportunity")
        void shouldUpdateExistingOpportunity() {
            OpportunityDTO updateDTO = OpportunityDTO.builder()
                .name("Updated Opportunity Name")
                .amount(new BigDecimal("150000"))
                .build();

            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenReturn(testOpportunity);

            OpportunityDTO result = opportunityService.update(tenantId, opportunityId, updateDTO);

            assertThat(result).isNotNull();
            verify(opportunityRepository).save(testOpportunity);
        }

        @Test
        @DisplayName("should throw RuntimeException when updating non-existent opportunity")
        void shouldThrowExceptionWhenUpdatingNonExistent() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> opportunityService.update(tenantId, opportunityId, testOpportunityDTO))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Opportunity not found");
        }
    }

    @Nested
    @DisplayName("updateStage")
    class UpdateStage {

        @Test
        @DisplayName("should update opportunity stage")
        void shouldUpdateOpportunityStage() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenReturn(testOpportunity);

            OpportunityDTO result = opportunityService.updateStage(tenantId, opportunityId, OpportunityStage.PROPOSAL);

            assertThat(result).isNotNull();
            verify(opportunityRepository).save(testOpportunity);
        }

        @Test
        @DisplayName("should set actual close date when stage is CLOSED_WON")
        void shouldSetActualCloseDateWhenClosedWon() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

            opportunityService.updateStage(tenantId, opportunityId, OpportunityStage.CLOSED_WON);

            assertThat(testOpportunity.getActualCloseDate()).isEqualTo(LocalDate.now());
            assertThat(testOpportunity.getStage()).isEqualTo(OpportunityStage.CLOSED_WON);
        }

        @Test
        @DisplayName("should set actual close date when stage is CLOSED_LOST")
        void shouldSetActualCloseDateWhenClosedLost() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));
            when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

            opportunityService.updateStage(tenantId, opportunityId, OpportunityStage.CLOSED_LOST);

            assertThat(testOpportunity.getActualCloseDate()).isEqualTo(LocalDate.now());
            assertThat(testOpportunity.getStage()).isEqualTo(OpportunityStage.CLOSED_LOST);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete existing opportunity")
        void shouldDeleteExistingOpportunity() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));

            opportunityService.delete(tenantId, opportunityId);

            verify(opportunityRepository).delete(testOpportunity);
        }

        @Test
        @DisplayName("should throw RuntimeException when deleting non-existent opportunity")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> opportunityService.delete(tenantId, opportunityId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Opportunity not found");
        }
    }

    @Nested
    @DisplayName("findByStage")
    class FindByStage {

        @Test
        @DisplayName("should return opportunities filtered by stage")
        void shouldReturnOpportunitiesFilteredByStage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity), pageable, 1);

            when(opportunityRepository.findByTenantIdAndStage(tenantId, OpportunityStage.DISCOVERY, pageable))
                .thenReturn(opportunityPage);

            Page<OpportunityDTO> result = opportunityService.findByStage(tenantId, OpportunityStage.DISCOVERY, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStage()).isEqualTo(OpportunityStage.DISCOVERY);
        }
    }

    @Nested
    @DisplayName("findOpenOpportunities")
    class FindOpenOpportunities {

        @Test
        @DisplayName("should return only open opportunities")
        void shouldReturnOnlyOpenOpportunities() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity), pageable, 1);

            when(opportunityRepository.findOpenOpportunities(tenantId, pageable))
                .thenReturn(opportunityPage);

            Page<OpportunityDTO> result = opportunityService.findOpenOpportunities(tenantId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("findByAccount")
    class FindByAccount {

        @Test
        @DisplayName("should return opportunities for specific account")
        void shouldReturnOpportunitiesForAccount() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Opportunity> opportunityPage = new PageImpl<>(List.of(testOpportunity), pageable, 1);

            when(opportunityRepository.findByTenantIdAndAccountId(tenantId, accountId, pageable))
                .thenReturn(opportunityPage);

            Page<OpportunityDTO> result = opportunityService.findByAccount(tenantId, accountId, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAccountId()).isEqualTo(accountId);
        }
    }

    @Nested
    @DisplayName("getPipelineMetrics")
    class GetPipelineMetrics {

        @Test
        @DisplayName("should return pipeline metrics")
        void shouldReturnPipelineMetrics() {
            BigDecimal totalPipeline = new BigDecimal("500000");
            BigDecimal weightedPipeline = new BigDecimal("150000");

            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(totalPipeline);
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(weightedPipeline);
            when(opportunityRepository.countByTenantIdAndStage(eq(tenantId), any(OpportunityStage.class)))
                .thenReturn(5L);

            PipelineMetricsDTO result = opportunityService.getPipelineMetrics(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getTotalPipelineValue()).isEqualByComparingTo(totalPipeline);
            assertThat(result.getWeightedPipelineValue()).isEqualByComparingTo(weightedPipeline);
        }

        @Test
        @DisplayName("should handle null pipeline values")
        void shouldHandleNullPipelineValues() {
            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(null);
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(null);
            when(opportunityRepository.countByTenantIdAndStage(eq(tenantId), any(OpportunityStage.class)))
                .thenReturn(0L);

            PipelineMetricsDTO result = opportunityService.getPipelineMetrics(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getTotalPipelineValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getWeightedPipelineValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should calculate win rate correctly")
        void shouldCalculateWinRateCorrectly() {
            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON))
                .thenReturn(3L);
            when(opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_LOST))
                .thenReturn(2L);
            when(opportunityRepository.countByTenantIdAndStage(eq(tenantId), any(OpportunityStage.class)))
                .thenAnswer(inv -> {
                    OpportunityStage stage = inv.getArgument(1);
                    if (stage == OpportunityStage.CLOSED_WON) return 3L;
                    if (stage == OpportunityStage.CLOSED_LOST) return 2L;
                    return 0L;
                });

            PipelineMetricsDTO result = opportunityService.getPipelineMetrics(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getWinRate()).isEqualTo(60.0); // 3 / (3 + 2) = 60%
            assertThat(result.getWonOpportunities()).isEqualTo(3L);
            assertThat(result.getLostOpportunities()).isEqualTo(2L);
        }

        @Test
        @DisplayName("should handle zero closed deals for win rate")
        void shouldHandleZeroClosedDealsForWinRate() {
            when(opportunityRepository.sumOpenPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.sumWeightedPipelineValue(tenantId)).thenReturn(BigDecimal.ZERO);
            when(opportunityRepository.countByTenantIdAndStage(eq(tenantId), any(OpportunityStage.class)))
                .thenReturn(0L);

            PipelineMetricsDTO result = opportunityService.getPipelineMetrics(tenantId);

            assertThat(result).isNotNull();
            assertThat(result.getWinRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("DTO Mapping")
    class DTOMapping {

        @Test
        @DisplayName("should correctly map all fields from entity to DTO")
        void shouldCorrectlyMapAllFieldsFromEntityToDTO() {
            when(opportunityRepository.findByIdAndTenantId(opportunityId, tenantId))
                .thenReturn(Optional.of(testOpportunity));

            OpportunityDTO result = opportunityService.findById(tenantId, opportunityId);

            assertThat(result.getId()).isEqualTo(testOpportunity.getId());
            assertThat(result.getTenantId()).isEqualTo(testOpportunity.getTenantId());
            assertThat(result.getAccountId()).isEqualTo(testOpportunity.getAccountId());
            assertThat(result.getName()).isEqualTo(testOpportunity.getName());
            assertThat(result.getDescription()).isEqualTo(testOpportunity.getDescription());
            assertThat(result.getAmount()).isEqualByComparingTo(testOpportunity.getAmount());
            assertThat(result.getStage()).isEqualTo(testOpportunity.getStage());
            assertThat(result.getProbability()).isEqualTo(testOpportunity.getProbability());
            assertThat(result.getExpectedCloseDate()).isEqualTo(testOpportunity.getExpectedCloseDate());
            assertThat(result.getProductTier()).isEqualTo(testOpportunity.getProductTier());
            assertThat(result.getContractLengthMonths()).isEqualTo(testOpportunity.getContractLengthMonths());
        }
    }
}

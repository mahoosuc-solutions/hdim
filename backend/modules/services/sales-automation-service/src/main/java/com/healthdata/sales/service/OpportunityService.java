package com.healthdata.sales.service;

import com.healthdata.sales.dto.OpportunityDTO;
import com.healthdata.sales.dto.PipelineMetricsDTO;
import com.healthdata.sales.entity.Opportunity;
import com.healthdata.sales.entity.OpportunityStage;
import com.healthdata.sales.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;

    @Transactional(readOnly = true)
    public Page<OpportunityDTO> findAll(UUID tenantId, Pageable pageable) {
        return opportunityRepository.findByTenantId(tenantId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public OpportunityDTO findById(UUID tenantId, UUID id) {
        Opportunity opportunity = opportunityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));
        return toDTO(opportunity);
    }

    @Transactional
    public OpportunityDTO create(UUID tenantId, OpportunityDTO dto) {
        dto.setTenantId(tenantId);
        Opportunity opportunity = toEntity(dto);
        opportunity.updateProbabilityFromStage();
        opportunity = opportunityRepository.save(opportunity);
        log.info("Created opportunity {} for tenant {}", opportunity.getId(), tenantId);
        return toDTO(opportunity);
    }

    @Transactional
    public OpportunityDTO update(UUID tenantId, UUID id, OpportunityDTO dto) {
        Opportunity opportunity = opportunityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));

        updateEntity(opportunity, dto);
        opportunity.updateProbabilityFromStage();
        opportunity = opportunityRepository.save(opportunity);
        log.info("Updated opportunity {}", opportunity.getId());
        return toDTO(opportunity);
    }

    @Transactional
    public OpportunityDTO updateStage(UUID tenantId, UUID id, OpportunityStage newStage) {
        Opportunity opportunity = opportunityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));

        opportunity.setStage(newStage);
        opportunity.updateProbabilityFromStage();

        if (newStage == OpportunityStage.CLOSED_WON || newStage == OpportunityStage.CLOSED_LOST) {
            opportunity.setActualCloseDate(LocalDate.now());
        }

        opportunity = opportunityRepository.save(opportunity);
        log.info("Updated opportunity {} stage to {}", opportunity.getId(), newStage);
        return toDTO(opportunity);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        Opportunity opportunity = opportunityRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));
        opportunityRepository.delete(opportunity);
        log.info("Deleted opportunity {}", id);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityDTO> findByStage(UUID tenantId, OpportunityStage stage, Pageable pageable) {
        return opportunityRepository.findByTenantIdAndStage(tenantId, stage, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityDTO> findOpenOpportunities(UUID tenantId, Pageable pageable) {
        return opportunityRepository.findOpenOpportunities(tenantId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityDTO> findByAccount(UUID tenantId, UUID accountId, Pageable pageable) {
        return opportunityRepository.findByTenantIdAndAccountId(tenantId, accountId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public PipelineMetricsDTO getPipelineMetrics(UUID tenantId) {
        BigDecimal totalPipeline = opportunityRepository.sumOpenPipelineValue(tenantId);
        BigDecimal weightedPipeline = opportunityRepository.sumWeightedPipelineValue(tenantId);

        Map<String, Long> opportunitiesByStage = new HashMap<>();
        for (OpportunityStage stage : OpportunityStage.values()) {
            Long count = opportunityRepository.countByTenantIdAndStage(tenantId, stage);
            opportunitiesByStage.put(stage.name(), count);
        }

        Long wonCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_WON);
        Long lostCount = opportunityRepository.countByTenantIdAndStage(tenantId, OpportunityStage.CLOSED_LOST);
        Long totalClosed = wonCount + lostCount;
        Double winRate = totalClosed > 0 ? (wonCount * 100.0) / totalClosed : 0.0;

        return PipelineMetricsDTO.builder()
            .totalPipelineValue(totalPipeline != null ? totalPipeline : BigDecimal.ZERO)
            .weightedPipelineValue(weightedPipeline != null ? weightedPipeline : BigDecimal.ZERO)
            .opportunitiesByStage(opportunitiesByStage)
            .wonOpportunities(wonCount)
            .lostOpportunities(lostCount)
            .winRate(winRate)
            .build();
    }

    private OpportunityDTO toDTO(Opportunity entity) {
        return OpportunityDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .accountId(entity.getAccountId())
            .primaryContactId(entity.getPrimaryContactId())
            .name(entity.getName())
            .description(entity.getDescription())
            .amount(entity.getAmount())
            .stage(entity.getStage())
            .probability(entity.getProbability())
            .expectedCloseDate(entity.getExpectedCloseDate())
            .actualCloseDate(entity.getActualCloseDate())
            .lostReason(entity.getLostReason())
            .lostReasonDetail(entity.getLostReasonDetail())
            .competitor(entity.getCompetitor())
            .nextStep(entity.getNextStep())
            .productTier(entity.getProductTier())
            .contractLengthMonths(entity.getContractLengthMonths())
            .zohoOpportunityId(entity.getZohoOpportunityId())
            .ownerUserId(entity.getOwnerUserId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .weightedAmount(entity.getWeightedAmount())
            .build();
    }

    private Opportunity toEntity(OpportunityDTO dto) {
        Opportunity entity = new Opportunity();
        entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
        entity.setTenantId(dto.getTenantId());
        entity.setAccountId(dto.getAccountId());
        entity.setPrimaryContactId(dto.getPrimaryContactId());
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setAmount(dto.getAmount());
        entity.setStage(dto.getStage() != null ? dto.getStage() : OpportunityStage.DISCOVERY);
        entity.setExpectedCloseDate(dto.getExpectedCloseDate());
        entity.setProductTier(dto.getProductTier());
        entity.setContractLengthMonths(dto.getContractLengthMonths());
        entity.setOwnerUserId(dto.getOwnerUserId());
        return entity;
    }

    private void updateEntity(Opportunity entity, OpportunityDTO dto) {
        if (dto.getAccountId() != null) entity.setAccountId(dto.getAccountId());
        if (dto.getPrimaryContactId() != null) entity.setPrimaryContactId(dto.getPrimaryContactId());
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getAmount() != null) entity.setAmount(dto.getAmount());
        if (dto.getStage() != null) entity.setStage(dto.getStage());
        if (dto.getExpectedCloseDate() != null) entity.setExpectedCloseDate(dto.getExpectedCloseDate());
        if (dto.getLostReason() != null) entity.setLostReason(dto.getLostReason());
        if (dto.getLostReasonDetail() != null) entity.setLostReasonDetail(dto.getLostReasonDetail());
        if (dto.getCompetitor() != null) entity.setCompetitor(dto.getCompetitor());
        if (dto.getNextStep() != null) entity.setNextStep(dto.getNextStep());
        if (dto.getProductTier() != null) entity.setProductTier(dto.getProductTier());
        if (dto.getContractLengthMonths() != null) entity.setContractLengthMonths(dto.getContractLengthMonths());
        if (dto.getOwnerUserId() != null) entity.setOwnerUserId(dto.getOwnerUserId());
    }
}

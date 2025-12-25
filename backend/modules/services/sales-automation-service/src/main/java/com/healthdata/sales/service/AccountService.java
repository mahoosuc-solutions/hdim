package com.healthdata.sales.service;

import com.healthdata.sales.dto.AccountDTO;
import com.healthdata.sales.entity.Account;
import com.healthdata.sales.entity.AccountStage;
import com.healthdata.sales.entity.OrganizationType;
import com.healthdata.sales.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public Page<AccountDTO> findAll(UUID tenantId, Pageable pageable) {
        return accountRepository.findByTenantId(tenantId, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public AccountDTO findById(UUID tenantId, UUID id) {
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + id));
        return toDTO(account);
    }

    @Transactional
    public AccountDTO create(UUID tenantId, AccountDTO dto) {
        dto.setTenantId(tenantId);
        Account account = toEntity(dto);
        account = accountRepository.save(account);
        log.info("Created account {} for tenant {}", account.getId(), tenantId);
        return toDTO(account);
    }

    @Transactional
    public AccountDTO update(UUID tenantId, UUID id, AccountDTO dto) {
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + id));

        updateEntity(account, dto);
        account = accountRepository.save(account);
        log.info("Updated account {}", account.getId());
        return toDTO(account);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        Account account = accountRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + id));
        accountRepository.delete(account);
        log.info("Deleted account {}", id);
    }

    @Transactional(readOnly = true)
    public Page<AccountDTO> findByStage(UUID tenantId, AccountStage stage, Pageable pageable) {
        return accountRepository.findByTenantIdAndStage(tenantId, stage, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<AccountDTO> findByOrganizationType(UUID tenantId, OrganizationType type, Pageable pageable) {
        return accountRepository.findByTenantIdAndOrganizationType(tenantId, type, pageable)
            .map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<AccountDTO> search(UUID tenantId, String query, Pageable pageable) {
        return accountRepository.searchByName(tenantId, query, pageable)
            .map(this::toDTO);
    }

    private AccountDTO toDTO(Account entity) {
        return AccountDTO.builder()
            .id(entity.getId())
            .tenantId(entity.getTenantId())
            .name(entity.getName())
            .organizationType(entity.getOrganizationType())
            .website(entity.getWebsite())
            .phone(entity.getPhone())
            .addressLine1(entity.getAddressLine1())
            .addressLine2(entity.getAddressLine2())
            .city(entity.getCity())
            .state(entity.getState())
            .zipCode(entity.getZipCode())
            .patientCount(entity.getPatientCount())
            .ehrCount(entity.getEhrCount())
            .ehrSystems(entity.getEhrSystems())
            .stage(entity.getStage())
            .annualRevenue(entity.getAnnualRevenue())
            .employeeCount(entity.getEmployeeCount())
            .industry(entity.getIndustry())
            .description(entity.getDescription())
            .zohoAccountId(entity.getZohoAccountId())
            .ownerUserId(entity.getOwnerUserId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    private Account toEntity(AccountDTO dto) {
        Account entity = new Account();
        entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
        entity.setTenantId(dto.getTenantId());
        entity.setName(dto.getName());
        entity.setOrganizationType(dto.getOrganizationType());
        entity.setWebsite(dto.getWebsite());
        entity.setPhone(dto.getPhone());
        entity.setAddressLine1(dto.getAddressLine1());
        entity.setAddressLine2(dto.getAddressLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setZipCode(dto.getZipCode());
        entity.setPatientCount(dto.getPatientCount());
        entity.setEhrCount(dto.getEhrCount());
        entity.setEhrSystems(dto.getEhrSystems());
        entity.setStage(dto.getStage() != null ? dto.getStage() : AccountStage.PROSPECT);
        entity.setAnnualRevenue(dto.getAnnualRevenue());
        entity.setEmployeeCount(dto.getEmployeeCount());
        entity.setIndustry(dto.getIndustry());
        entity.setDescription(dto.getDescription());
        entity.setOwnerUserId(dto.getOwnerUserId());
        return entity;
    }

    private void updateEntity(Account entity, AccountDTO dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getOrganizationType() != null) entity.setOrganizationType(dto.getOrganizationType());
        if (dto.getWebsite() != null) entity.setWebsite(dto.getWebsite());
        if (dto.getPhone() != null) entity.setPhone(dto.getPhone());
        if (dto.getAddressLine1() != null) entity.setAddressLine1(dto.getAddressLine1());
        if (dto.getAddressLine2() != null) entity.setAddressLine2(dto.getAddressLine2());
        if (dto.getCity() != null) entity.setCity(dto.getCity());
        if (dto.getState() != null) entity.setState(dto.getState());
        if (dto.getZipCode() != null) entity.setZipCode(dto.getZipCode());
        if (dto.getPatientCount() != null) entity.setPatientCount(dto.getPatientCount());
        if (dto.getEhrCount() != null) entity.setEhrCount(dto.getEhrCount());
        if (dto.getEhrSystems() != null) entity.setEhrSystems(dto.getEhrSystems());
        if (dto.getStage() != null) entity.setStage(dto.getStage());
        if (dto.getAnnualRevenue() != null) entity.setAnnualRevenue(dto.getAnnualRevenue());
        if (dto.getEmployeeCount() != null) entity.setEmployeeCount(dto.getEmployeeCount());
        if (dto.getIndustry() != null) entity.setIndustry(dto.getIndustry());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getOwnerUserId() != null) entity.setOwnerUserId(dto.getOwnerUserId());
    }
}

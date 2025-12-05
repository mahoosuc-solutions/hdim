package com.healthdata.cql.service;

import com.healthdata.cql.entity.ValueSet;
import com.healthdata.cql.repository.ValueSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Value Sets
 *
 * Provides business logic for creating, updating, retrieving, and managing
 * SNOMED, LOINC, RxNorm, and other code system value sets used in CQL evaluation.
 */
@Service
@Transactional
public class ValueSetService {

    private static final Logger logger = LoggerFactory.getLogger(ValueSetService.class);

    private final ValueSetRepository valueSetRepository;

    public ValueSetService(ValueSetRepository valueSetRepository) {
        this.valueSetRepository = valueSetRepository;
    }

    /**
     * Create a new value set
     */
    public ValueSet createValueSet(ValueSet valueSet) {
        logger.info("Creating value set: {} (OID: {}) for tenant: {}",
                valueSet.getName(), valueSet.getOid(), valueSet.getTenantId());

        // Check if value set with same OID already exists
        if (valueSetRepository.existsByTenantIdAndOidAndActiveTrue(
                valueSet.getTenantId(), valueSet.getOid())) {
            throw new IllegalArgumentException(
                    "Value set with OID " + valueSet.getOid() +
                    " already exists for tenant " + valueSet.getTenantId());
        }

        // Set default status if not provided
        if (valueSet.getStatus() == null) {
            valueSet.setStatus("ACTIVE");
        }

        // Set active flag
        valueSet.setActive(true);

        ValueSet savedValueSet = valueSetRepository.save(valueSet);
        logger.info("Created value set with ID: {}", savedValueSet.getId());
        return savedValueSet;
    }

    /**
     * Update an existing value set
     */
    public ValueSet updateValueSet(UUID valueSetId, ValueSet updates) {
        logger.info("Updating value set: {}", valueSetId);

        ValueSet existingValueSet = valueSetRepository.findById(valueSetId)
                .orElseThrow(() -> new IllegalArgumentException("Value set not found: " + valueSetId));

        // Verify tenant matches
        if (!existingValueSet.getTenantId().equals(updates.getTenantId())) {
            throw new IllegalArgumentException("Tenant mismatch for value set: " + valueSetId);
        }

        // Update mutable fields
        if (updates.getOid() != null) {
            existingValueSet.setOid(updates.getOid());
        }
        if (updates.getName() != null) {
            existingValueSet.setName(updates.getName());
        }
        if (updates.getVersion() != null) {
            existingValueSet.setVersion(updates.getVersion());
        }
        if (updates.getCodeSystem() != null) {
            existingValueSet.setCodeSystem(updates.getCodeSystem());
        }
        if (updates.getCodes() != null) {
            existingValueSet.setCodes(updates.getCodes());
        }
        if (updates.getDescription() != null) {
            existingValueSet.setDescription(updates.getDescription());
        }
        if (updates.getPublisher() != null) {
            existingValueSet.setPublisher(updates.getPublisher());
        }
        if (updates.getStatus() != null) {
            existingValueSet.setStatus(updates.getStatus());
        }
        if (updates.getFhirValueSetId() != null) {
            existingValueSet.setFhirValueSetId(updates.getFhirValueSetId());
        }

        ValueSet updatedValueSet = valueSetRepository.save(existingValueSet);
        logger.info("Updated value set: {}", valueSetId);
        return updatedValueSet;
    }

    /**
     * Get a value set by ID
     */
    @Transactional(readOnly = true)
    public Optional<ValueSet> getValueSetById(UUID valueSetId) {
        return valueSetRepository.findById(valueSetId);
    }

    /**
     * Get a value set by tenant and ID
     */
    @Transactional(readOnly = true)
    public Optional<ValueSet> getValueSetByIdAndTenant(UUID valueSetId, String tenantId) {
        return valueSetRepository.findById(valueSetId)
                .filter(valueSet -> valueSet.getTenantId().equals(tenantId))
                .filter(ValueSet::getActive);
    }

    /**
     * Get value set by OID
     */
    @Transactional(readOnly = true)
    public Optional<ValueSet> getValueSetByOid(String tenantId, String oid) {
        return valueSetRepository.findByTenantIdAndOidAndActiveTrue(tenantId, oid);
    }

    /**
     * Get value set by OID and version
     */
    @Transactional(readOnly = true)
    public Optional<ValueSet> getValueSetByOidAndVersion(
            String tenantId, String oid, String version) {
        return valueSetRepository.findByTenantIdAndOidAndVersionAndActiveTrue(
                tenantId, oid, version);
    }

    /**
     * Get the latest version of a value set by OID
     */
    @Transactional(readOnly = true)
    public Optional<ValueSet> getLatestValueSetVersion(String tenantId, String oid) {
        return valueSetRepository.findLatestVersionByOid(tenantId, oid);
    }

    /**
     * Get all versions of a value set by OID
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getAllValueSetVersions(String tenantId, String oid) {
        return valueSetRepository.findAllVersionsByOid(tenantId, oid);
    }

    /**
     * Get value set by name
     */
    @Transactional(readOnly = true)
    public Optional<ValueSet> getValueSetByName(String tenantId, String name) {
        return valueSetRepository.findByTenantIdAndNameAndActiveTrue(tenantId, name);
    }

    /**
     * Get all active value sets for a tenant
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getAllValueSets(String tenantId) {
        return valueSetRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    /**
     * Get all value sets for a tenant with pagination
     */
    @Transactional(readOnly = true)
    public Page<ValueSet> getAllValueSets(String tenantId, Pageable pageable) {
        return valueSetRepository.findByTenantIdAndActiveTrue(tenantId, pageable);
    }

    /**
     * Get value sets by code system
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getValueSetsByCodeSystem(String tenantId, String codeSystem) {
        return valueSetRepository.findByTenantIdAndCodeSystemAndActiveTrue(tenantId, codeSystem);
    }

    /**
     * Get value sets by code system with pagination
     */
    @Transactional(readOnly = true)
    public Page<ValueSet> getValueSetsByCodeSystem(
            String tenantId, String codeSystem, Pageable pageable) {
        return valueSetRepository.findByTenantIdAndCodeSystemAndActiveTrue(
                tenantId, codeSystem, pageable);
    }

    /**
     * Get ACTIVE value sets (status = ACTIVE)
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getActiveValueSets(String tenantId) {
        return valueSetRepository.findActiveValueSets(tenantId);
    }

    /**
     * Get value sets by status
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getValueSetsByStatus(String tenantId, String status) {
        return valueSetRepository.findByTenantIdAndStatusAndActiveTrue(tenantId, status);
    }

    /**
     * Get value sets by publisher
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getValueSetsByPublisher(String tenantId, String publisher) {
        return valueSetRepository.findByTenantIdAndPublisherAndActiveTrue(tenantId, publisher);
    }

    /**
     * Search value sets by name
     */
    @Transactional(readOnly = true)
    public List<ValueSet> searchValueSetsByName(String tenantId, String searchTerm) {
        return valueSetRepository.searchByName(tenantId, searchTerm);
    }

    /**
     * Get value sets by OID prefix (e.g., all value sets from a specific organization)
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getValueSetsByOidPrefix(String tenantId, String oidPrefix) {
        return valueSetRepository.findByOidPrefix(tenantId, oidPrefix);
    }

    /**
     * Get common code system value sets (SNOMED, LOINC, RxNorm)
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getCommonCodeSystemValueSets(String tenantId) {
        return valueSetRepository.findCommonCodeSystemValueSets(tenantId);
    }

    /**
     * Get SNOMED value sets
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getSnomedValueSets(String tenantId) {
        return valueSetRepository.findByTenantIdAndCodeSystemAndActiveTrue(tenantId, "SNOMED");
    }

    /**
     * Get LOINC value sets
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getLoincValueSets(String tenantId) {
        return valueSetRepository.findByTenantIdAndCodeSystemAndActiveTrue(tenantId, "LOINC");
    }

    /**
     * Get RxNorm value sets
     */
    @Transactional(readOnly = true)
    public List<ValueSet> getRxNormValueSets(String tenantId) {
        return valueSetRepository.findByTenantIdAndCodeSystemAndActiveTrue(tenantId, "RxNorm");
    }

    /**
     * Activate a value set
     */
    public ValueSet activateValueSet(UUID valueSetId, String tenantId) {
        logger.info("Activating value set: {}", valueSetId);

        ValueSet valueSet = getValueSetByIdAndTenant(valueSetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Value set not found: " + valueSetId));

        valueSet.setStatus("ACTIVE");
        ValueSet updatedValueSet = valueSetRepository.save(valueSet);

        logger.info("Activated value set: {}", valueSetId);
        return updatedValueSet;
    }

    /**
     * Retire a value set
     */
    public ValueSet retireValueSet(UUID valueSetId, String tenantId) {
        logger.info("Retiring value set: {}", valueSetId);

        ValueSet valueSet = getValueSetByIdAndTenant(valueSetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Value set not found: " + valueSetId));

        valueSet.setStatus("RETIRED");
        ValueSet updatedValueSet = valueSetRepository.save(valueSet);

        logger.info("Retired value set: {}", valueSetId);
        return updatedValueSet;
    }

    /**
     * Soft delete a value set
     */
    public void deleteValueSet(UUID valueSetId, String tenantId) {
        logger.info("Soft deleting value set: {}", valueSetId);

        ValueSet valueSet = getValueSetByIdAndTenant(valueSetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Value set not found: " + valueSetId));

        valueSet.setActive(false);
        valueSetRepository.save(valueSet);

        logger.info("Soft deleted value set: {}", valueSetId);
    }

    /**
     * Count value sets for a tenant
     */
    @Transactional(readOnly = true)
    public long countValueSets(String tenantId) {
        return valueSetRepository.countByTenantIdAndActiveTrue(tenantId);
    }

    /**
     * Count value sets by code system
     */
    @Transactional(readOnly = true)
    public long countValueSetsByCodeSystem(String tenantId, String codeSystem) {
        return valueSetRepository.countByTenantIdAndCodeSystemAndActiveTrue(tenantId, codeSystem);
    }

    /**
     * Count value sets by status
     */
    @Transactional(readOnly = true)
    public long countValueSetsByStatus(String tenantId, String status) {
        return valueSetRepository.countByTenantIdAndStatusAndActiveTrue(tenantId, status);
    }

    /**
     * Check if a value set exists by OID
     */
    @Transactional(readOnly = true)
    public boolean valueSetExistsByOid(String tenantId, String oid) {
        return valueSetRepository.existsByTenantIdAndOidAndActiveTrue(tenantId, oid);
    }

    /**
     * Check if a value set exists by name
     */
    @Transactional(readOnly = true)
    public boolean valueSetExistsByName(String tenantId, String name) {
        return valueSetRepository.existsByTenantIdAndNameAndActiveTrue(tenantId, name);
    }

    /**
     * Check if a code exists in a value set
     * This is a placeholder - actual implementation would parse the JSON codes
     */
    public boolean codeExistsInValueSet(UUID valueSetId, String tenantId, String code) {
        logger.info("Checking if code {} exists in value set: {}", code, valueSetId);

        ValueSet valueSet = getValueSetByIdAndTenant(valueSetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Value set not found: " + valueSetId));

        if (valueSet.getCodes() == null || valueSet.getCodes().isEmpty()) {
            return false;
        }

        // TODO: Parse JSON codes array and check if code exists
        logger.warn("Code lookup not yet fully implemented - placeholder called");

        return valueSet.getCodes().contains(code);
    }
}

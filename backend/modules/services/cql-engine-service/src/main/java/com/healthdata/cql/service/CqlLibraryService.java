package com.healthdata.cql.service;

import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.repository.CqlLibraryRepository;
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
 * Service for managing CQL Libraries
 *
 * Provides business logic for creating, updating, retrieving, and managing
 * Clinical Quality Language (CQL) libraries used in quality measure evaluation.
 */
@Service
@Transactional
public class CqlLibraryService {

    private static final Logger logger = LoggerFactory.getLogger(CqlLibraryService.class);

    private final CqlLibraryRepository libraryRepository;

    public CqlLibraryService(CqlLibraryRepository libraryRepository) {
        this.libraryRepository = libraryRepository;
    }

    /**
     * Create a new CQL library
     */
    public CqlLibrary createLibrary(CqlLibrary library) {
        // Validate required fields
        if (library.getTenantId() == null || library.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (library.getLibraryName() == null || library.getLibraryName().trim().isEmpty()) {
            throw new IllegalArgumentException("Library name is required");
        }
        if (library.getVersion() == null || library.getVersion().trim().isEmpty()) {
            throw new IllegalArgumentException("Library version is required");
        }

        logger.info("Creating CQL library: {} v{} for tenant: {}",
                library.getLibraryName(), library.getVersion(), library.getTenantId());

        // Check if library with same name and version already exists
        if (libraryRepository.existsByTenantIdAndLibraryNameAndVersionAndActiveTrue(
                library.getTenantId(), library.getLibraryName(), library.getVersion())) {
            throw new IllegalArgumentException(
                    "Library " + library.getLibraryName() + " v" + library.getVersion() +
                    " already exists for tenant " + library.getTenantId());
        }

        // Set default status if not provided
        if (library.getStatus() == null) {
            library.setStatus("DRAFT");
        }

        // Set active flag
        library.setActive(true);

        CqlLibrary savedLibrary = libraryRepository.save(library);
        logger.info("Created CQL library with ID: {}", savedLibrary.getId());
        return savedLibrary;
    }

    /**
     * Update an existing CQL library
     */
    public CqlLibrary updateLibrary(UUID libraryId, CqlLibrary updates) {
        logger.info("Updating CQL library: {}", libraryId);

        CqlLibrary existingLibrary = libraryRepository.findById(libraryId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        // Verify tenant matches
        if (!existingLibrary.getTenantId().equals(updates.getTenantId())) {
            throw new IllegalArgumentException("Tenant mismatch for library: " + libraryId);
        }

        // Update mutable fields
        if (updates.getLibraryName() != null) {
            existingLibrary.setLibraryName(updates.getLibraryName());
        }
        if (updates.getVersion() != null) {
            existingLibrary.setVersion(updates.getVersion());
        }
        if (updates.getStatus() != null) {
            existingLibrary.setStatus(updates.getStatus());
        }
        if (updates.getCqlContent() != null) {
            existingLibrary.setCqlContent(updates.getCqlContent());
        }
        if (updates.getElmJson() != null) {
            existingLibrary.setElmJson(updates.getElmJson());
        }
        if (updates.getElmXml() != null) {
            existingLibrary.setElmXml(updates.getElmXml());
        }
        if (updates.getDescription() != null) {
            existingLibrary.setDescription(updates.getDescription());
        }
        if (updates.getPublisher() != null) {
            existingLibrary.setPublisher(updates.getPublisher());
        }
        if (updates.getFhirLibraryId() != null) {
            existingLibrary.setFhirLibraryId(updates.getFhirLibraryId());
        }

        CqlLibrary updatedLibrary = libraryRepository.save(existingLibrary);
        logger.info("Updated CQL library: {}", libraryId);
        return updatedLibrary;
    }

    /**
     * Get a library by ID
     */
    @Transactional(readOnly = true)
    public Optional<CqlLibrary> getLibraryById(UUID libraryId) {
        return libraryRepository.findById(libraryId);
    }

    /**
     * Get a library by tenant and ID
     */
    @Transactional(readOnly = true)
    public Optional<CqlLibrary> getLibraryByIdAndTenant(UUID libraryId, String tenantId) {
        return libraryRepository.findById(libraryId)
                .filter(library -> library.getTenantId().equals(tenantId))
                .filter(CqlLibrary::getActive);
    }

    /**
     * Get a specific library by name and version
     */
    @Transactional(readOnly = true)
    public Optional<CqlLibrary> getLibraryByNameAndVersion(
            String tenantId, String libraryName, String version) {
        return libraryRepository.findByTenantIdAndLibraryNameAndVersionAndActiveTrue(
                tenantId, libraryName, version);
    }

    /**
     * Get the latest version of a library
     */
    @Transactional(readOnly = true)
    public Optional<CqlLibrary> getLatestLibraryVersion(String tenantId, String libraryName) {
        return libraryRepository.findLatestVersionByName(tenantId, libraryName);
    }

    /**
     * Get all versions of a library
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getAllLibraryVersions(String tenantId, String libraryName) {
        return libraryRepository.findByTenantIdAndLibraryNameAndActiveTrueOrderByVersionDesc(
                tenantId, libraryName);
    }

    /**
     * Get all active libraries for a tenant
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getAllLibraries(String tenantId) {
        return libraryRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    /**
     * Get all libraries for a tenant with pagination
     */
    @Transactional(readOnly = true)
    public Page<CqlLibrary> getAllLibraries(String tenantId, Pageable pageable) {
        return libraryRepository.findByTenantIdAndActiveTrue(tenantId, pageable);
    }

    /**
     * Get libraries by status
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getLibrariesByStatus(String tenantId, String status) {
        return libraryRepository.findByTenantIdAndStatusAndActiveTrue(tenantId, status);
    }

    /**
     * Get all ACTIVE libraries (status = ACTIVE)
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getActiveLibraries(String tenantId) {
        return libraryRepository.findActiveLibraries(tenantId);
    }

    /**
     * Get libraries by publisher
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getLibrariesByPublisher(String tenantId, String publisher) {
        return libraryRepository.findByTenantIdAndPublisherAndActiveTrue(tenantId, publisher);
    }

    /**
     * Search libraries by name
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> searchLibrariesByName(String tenantId, String searchTerm) {
        return libraryRepository.searchByName(tenantId, searchTerm);
    }

    /**
     * Activate a library (set status to ACTIVE)
     */
    public CqlLibrary activateLibrary(UUID libraryId, String tenantId) {
        logger.info("Activating CQL library: {}", libraryId);

        CqlLibrary library = getLibraryByIdAndTenant(libraryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        library.setStatus("ACTIVE");
        CqlLibrary updatedLibrary = libraryRepository.save(library);

        logger.info("Activated CQL library: {}", libraryId);
        return updatedLibrary;
    }

    /**
     * Retire a library (set status to RETIRED)
     */
    public CqlLibrary retireLibrary(UUID libraryId, String tenantId) {
        logger.info("Retiring CQL library: {}", libraryId);

        CqlLibrary library = getLibraryByIdAndTenant(libraryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        library.setStatus("RETIRED");
        CqlLibrary updatedLibrary = libraryRepository.save(library);

        logger.info("Retired CQL library: {}", libraryId);
        return updatedLibrary;
    }

    /**
     * Soft delete a library (set active to false)
     */
    public void deleteLibrary(UUID libraryId, String tenantId) {
        logger.info("Soft deleting CQL library: {}", libraryId);

        CqlLibrary library = getLibraryByIdAndTenant(libraryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        library.setActive(false);
        libraryRepository.save(library);

        logger.info("Soft deleted CQL library: {}", libraryId);
    }

    /**
     * Count libraries for a tenant
     */
    @Transactional(readOnly = true)
    public long countLibraries(String tenantId) {
        return libraryRepository.countByTenantIdAndActiveTrue(tenantId);
    }

    /**
     * Count libraries by status
     */
    @Transactional(readOnly = true)
    public long countLibrariesByStatus(String tenantId, String status) {
        return libraryRepository.countByTenantIdAndStatusAndActiveTrue(tenantId, status);
    }

    /**
     * Check if a library exists
     */
    @Transactional(readOnly = true)
    public boolean libraryExists(String tenantId, String libraryName, String version) {
        return libraryRepository.existsByTenantIdAndLibraryNameAndVersionAndActiveTrue(
                tenantId, libraryName, version);
    }

    /**
     * Compile CQL to ELM (Expression Logical Model)
     * This is a placeholder - actual implementation would use CQL-to-ELM translator
     */
    public CqlLibrary compileLibrary(UUID libraryId, String tenantId) {
        logger.info("Compiling CQL library: {}", libraryId);

        CqlLibrary library = getLibraryByIdAndTenant(libraryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        if (library.getCqlContent() == null || library.getCqlContent().isEmpty()) {
            throw new IllegalArgumentException("Library has no CQL content to compile");
        }

        // TODO: Implement actual CQL-to-ELM compilation using org.cqframework.cql:cql-to-elm-translator
        // For now, this is a placeholder
        logger.warn("CQL compilation not yet implemented - placeholder called");

        return library;
    }

    /**
     * Validate CQL syntax
     * This is a placeholder - actual implementation would validate CQL
     */
    public boolean validateLibrary(UUID libraryId, String tenantId) {
        logger.info("Validating CQL library: {}", libraryId);

        CqlLibrary library = getLibraryByIdAndTenant(libraryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        if (library.getCqlContent() == null || library.getCqlContent().isEmpty()) {
            throw new IllegalArgumentException("Library has no CQL content to validate");
        }

        // TODO: Implement actual CQL validation
        logger.warn("CQL validation not yet implemented - placeholder called");

        return true;
    }
}

package com.healthdata.cql.service;

import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.repository.CqlLibraryRepository;
import org.cqframework.cql.cql2elm.CqlCompilerException;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.ModelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * Compile CQL to ELM (Expression Logical Model).
     * Uses the CQL-to-ELM translator to compile CQL source into ELM JSON and XML.
     *
     * @param libraryId the library ID to compile
     * @param tenantId the tenant ID
     * @return the updated library with ELM content
     * @throws IllegalArgumentException if library not found or has no CQL content
     * @throws IllegalStateException if compilation fails
     */
    public CqlLibrary compileLibrary(UUID libraryId, String tenantId) {
        logger.info("Compiling CQL library: {}", libraryId);

        CqlLibrary library = getLibraryByIdAndTenant(libraryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        if (library.getCqlContent() == null || library.getCqlContent().isEmpty()) {
            throw new IllegalArgumentException("Library has no CQL content to compile");
        }

        try {
            // Create model and library managers for the translator
            ModelManager modelManager = new ModelManager();
            LibraryManager libraryManager = new LibraryManager(modelManager);

            // Compile CQL to ELM
            CqlTranslator translator = CqlTranslator.fromText(
                    library.getCqlContent(),
                    libraryManager
            );

            // Check for compilation errors
            List<CqlCompilerException> errors = translator.getErrors();
            if (!errors.isEmpty()) {
                String errorMessages = errors.stream()
                        .map(CqlCompilerException::getMessage)
                        .collect(Collectors.joining("; "));
                throw new IllegalStateException("CQL compilation errors: " + errorMessages);
            }

            // Get ELM output in JSON format
            String elmJson = translator.toJson();

            // Update library with compiled ELM
            library.setElmJson(elmJson);
            library.setStatus("COMPILED");

            CqlLibrary savedLibrary = libraryRepository.save(library);
            logger.info("Successfully compiled CQL library {} to ELM", libraryId);

            return savedLibrary;

        } catch (IllegalStateException e) {
            // Re-throw compilation errors
            throw e;
        } catch (Exception e) {
            logger.error("CQL compilation failed for library {}: {}", libraryId, e.getMessage(), e);
            throw new IllegalStateException("CQL compilation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get libraries by category
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getLibrariesByCategory(String tenantId, String category) {
        return libraryRepository.findByTenantIdAndCategoryAndActiveTrue(tenantId, category);
    }

    /**
     * Get libraries by category with pagination
     */
    @Transactional(readOnly = true)
    public Page<CqlLibrary> getLibrariesByCategory(String tenantId, String category, Pageable pageable) {
        return libraryRepository.findByTenantIdAndCategory(tenantId, category, pageable);
    }

    /**
     * Get all HEDIS measures
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getHedisMeasures(String tenantId) {
        return libraryRepository.findHedisMeasures(tenantId);
    }

    /**
     * Get libraries with Java measure implementations
     */
    @Transactional(readOnly = true)
    public List<CqlLibrary> getLibrariesWithJavaImplementation(String tenantId) {
        return libraryRepository.findLibrariesWithJavaImplementation(tenantId);
    }

    /**
     * Get all unique measure categories
     */
    @Transactional(readOnly = true)
    public List<String> getAllCategories(String tenantId) {
        return libraryRepository.findDistinctCategories(tenantId);
    }

    /**
     * Count libraries by category
     */
    @Transactional(readOnly = true)
    public long countLibrariesByCategory(String tenantId, String category) {
        return libraryRepository.countByTenantIdAndCategory(tenantId, category);
    }

    /**
     * Validate CQL syntax without full compilation.
     * Uses the CQL-to-ELM translator parser to check syntax.
     *
     * @param libraryId the library ID to validate
     * @param tenantId the tenant ID
     * @return true if CQL syntax is valid, false otherwise
     * @throws IllegalArgumentException if library not found or has no CQL content
     */
    public boolean validateLibrary(UUID libraryId, String tenantId) {
        logger.info("Validating CQL library: {}", libraryId);

        CqlLibrary library = getLibraryByIdAndTenant(libraryId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Library not found: " + libraryId));

        if (library.getCqlContent() == null || library.getCqlContent().isEmpty()) {
            throw new IllegalArgumentException("Library has no CQL content to validate");
        }

        try {
            // Create model and library managers for the translator
            ModelManager modelManager = new ModelManager();
            LibraryManager libraryManager = new LibraryManager(modelManager);

            // Attempt to translate (validates syntax)
            CqlTranslator translator = CqlTranslator.fromText(
                    library.getCqlContent(),
                    libraryManager
            );

            // Check for errors - return false if any exist
            boolean isValid = translator.getErrors().isEmpty();

            if (!isValid) {
                String errorMessages = translator.getErrors().stream()
                        .map(CqlCompilerException::getMessage)
                        .collect(Collectors.joining("; "));
                logger.warn("CQL validation failed for library {}: {}", libraryId, errorMessages);
            } else {
                logger.info("CQL library {} validated successfully", libraryId);
            }

            return isValid;

        } catch (Exception e) {
            logger.warn("CQL validation failed for library {}: {}", libraryId, e.getMessage());
            return false;
        }
    }
}

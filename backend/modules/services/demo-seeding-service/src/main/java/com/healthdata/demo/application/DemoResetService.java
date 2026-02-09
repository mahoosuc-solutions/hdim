package com.healthdata.demo.application;

import com.healthdata.demo.domain.model.DemoSnapshot;
import com.healthdata.demo.domain.repository.DemoSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for resetting demo data and managing snapshots.
 *
 * Responsibilities:
 * - Reset demo data to baseline state
 * - Create and restore database snapshots
 * - Manage snapshot lifecycle (create, restore, cleanup)
 */
@Service
@Transactional
public class DemoResetService {

    private static final Logger logger = LoggerFactory.getLogger(DemoResetService.class);

    private final DemoSnapshotRepository snapshotRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${demo.snapshots.directory:./demo-snapshots}")
    private String snapshotsDirectory;

    @Value("${demo.database.name:healthdata_demo}")
    private String databaseName;

    public DemoResetService(
            DemoSnapshotRepository snapshotRepository,
            JdbcTemplate jdbcTemplate) {
        this.snapshotRepository = snapshotRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Reset demo data for a specific tenant.
     *
     * @param tenantId Tenant to reset
     */
    public void resetDemoData(String tenantId) {
        logger.info("Resetting demo data for tenant: {}", tenantId);
        long startTime = System.currentTimeMillis();

        try {
            // Clear FHIR resources for tenant
            clearFhirResources(tenantId);

            // Clear care gaps for tenant
            clearCareGaps(tenantId);

            // Clear quality evaluations for tenant
            clearQualityEvaluations(tenantId);

            // Reset any cached data
            clearCaches(tenantId);
            clearSessionProgress(tenantId);

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("Demo data reset complete for tenant {} in {}ms", tenantId, elapsed);

        } catch (Exception e) {
            logger.error("Failed to reset demo data for tenant: {}", tenantId, e);
            throw new RuntimeException("Failed to reset demo data", e);
        }
    }

    /**
     * Full reset of all demo data.
     */
    public ResetResult fullReset() {
        logger.info("Performing full demo reset");
        long startTime = System.currentTimeMillis();

        ResetResult result = new ResetResult();
        String demoTenantClause = "tenant_id LIKE 'demo-%' OR tenant_id IN ('demo-tenant', 'acme-health', 'summit-care', 'valley-health', 'blue-shield-demo', 'united-demo')";

        try {
            // Clear all demo tables (gracefully handle missing tables)
            int patientsDeleted = safeDeleteWithClause("patients", demoTenantClause);
            result.setPatientsDeleted(patientsDeleted);

            int conditionsDeleted = safeDeleteWithClause("conditions", demoTenantClause);
            result.setConditionsDeleted(conditionsDeleted);

            // Clear other FHIR resources for demo tenants
            String[] fhirTables = {
                "observations",
                "medication_requests",
                "encounters",
                "procedures",
                "immunizations",
                "allergy_intolerances",
                "diagnostic_reports",
                "document_references",
                "care_plans",
                "goals",
                "coverages"
            };
            for (String table : fhirTables) {
                safeDeleteWithClause(table, demoTenantClause);
            }

            int careGapsDeleted = safeDeleteWithClause("care_gaps", demoTenantClause);
            result.setCareGapsDeleted(careGapsDeleted);

            // Clear demo sessions (but keep scenarios) - this table should exist
            try {
                jdbcTemplate.update("DELETE FROM demo_sessions");
            } catch (Exception e) {
                logger.debug("demo_sessions table does not exist or is empty, skipping");
            }
            try {
                jdbcTemplate.update("DELETE FROM demo_session_progress");
            } catch (Exception e) {
                logger.debug("demo_session_progress table does not exist or is empty, skipping");
            }

            result.setSuccess(true);
            result.setResetTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Full reset complete: {} patients, {} conditions, {} care gaps deleted in {}ms",
                patientsDeleted, conditionsDeleted, careGapsDeleted, result.getResetTimeMs());

        } catch (Exception e) {
            logger.error("Failed to perform full reset", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * Create a database snapshot for quick restore.
     *
     * @param name Snapshot name
     * @param description Optional description
     * @return Created snapshot
     */
    public DemoSnapshot createSnapshot(String name, String description) {
        logger.info("Creating snapshot: {}", name);

        // Validate name doesn't exist
        if (snapshotRepository.existsByName(name)) {
            throw new IllegalArgumentException("Snapshot already exists: " + name);
        }

        // Create snapshots directory if needed
        Path snapshotsPath = Paths.get(snapshotsDirectory);
        try {
            Files.createDirectories(snapshotsPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create snapshots directory", e);
        }

        // Generate filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileName = String.format("%s-%s.sql", name, timestamp);
        Path filePath = snapshotsPath.resolve(fileName);

        // Create snapshot entity
        DemoSnapshot snapshot = new DemoSnapshot(name, null, filePath.toString());
        snapshot.setDescription(description);
        snapshot.setCreatedBy("demo-cli");
        snapshot = snapshotRepository.save(snapshot);

        try {
            // Execute pg_dump (simplified - in production would use proper process execution)
            long fileSize = executePgDump(filePath);

            snapshot.markReady(fileSize);
            snapshot = snapshotRepository.save(snapshot);

            logger.info("Snapshot created: {} ({}KB)", name, fileSize / 1024);

        } catch (Exception e) {
            logger.error("Failed to create snapshot: {}", name, e);
            snapshot.markFailed();
            snapshotRepository.save(snapshot);
            throw new RuntimeException("Failed to create snapshot", e);
        }

        return snapshot;
    }

    /**
     * Restore from a snapshot.
     *
     * @param snapshotId Snapshot ID to restore
     * @return Restore result
     */
    public RestoreResult restoreSnapshot(UUID snapshotId) {
        DemoSnapshot snapshot = snapshotRepository.findById(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        return restoreSnapshot(snapshot);
    }

    /**
     * Restore from a snapshot by name.
     *
     * @param name Snapshot name
     * @return Restore result
     */
    public RestoreResult restoreSnapshotByName(String name) {
        DemoSnapshot snapshot = snapshotRepository.findByName(name)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + name));

        return restoreSnapshot(snapshot);
    }

    /**
     * Restore from a snapshot.
     */
    private RestoreResult restoreSnapshot(DemoSnapshot snapshot) {
        logger.info("Restoring snapshot: {}", snapshot.getName());
        long startTime = System.currentTimeMillis();

        RestoreResult result = new RestoreResult();
        result.setSnapshotName(snapshot.getName());

        try {
            // Verify snapshot file exists
            Path filePath = Paths.get(snapshot.getFilePath());
            if (!Files.exists(filePath)) {
                throw new FileNotFoundException("Snapshot file not found: " + filePath);
            }

            // Execute restore
            executeRestore(filePath);

            // Update snapshot metadata
            snapshot.recordRestore();
            snapshotRepository.save(snapshot);

            result.setSuccess(true);
            result.setRestoreTimeMs(System.currentTimeMillis() - startTime);

            logger.info("Snapshot restored: {} in {}ms", snapshot.getName(), result.getRestoreTimeMs());

        } catch (Exception e) {
            logger.error("Failed to restore snapshot: {}", snapshot.getName(), e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * List all available snapshots.
     */
    @Transactional(readOnly = true)
    public List<DemoSnapshot> listSnapshots() {
        return snapshotRepository.findReadySnapshots();
    }

    /**
     * Delete a snapshot.
     */
    public void deleteSnapshot(String name) {
        DemoSnapshot snapshot = snapshotRepository.findByName(name)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + name));

        try {
            // Delete file
            Path filePath = Paths.get(snapshot.getFilePath());
            Files.deleteIfExists(filePath);

            // Mark as deleted
            snapshot.setStatus(DemoSnapshot.SnapshotStatus.DELETED);
            snapshotRepository.save(snapshot);

            logger.info("Snapshot deleted: {}", name);

        } catch (IOException e) {
            logger.error("Failed to delete snapshot file: {}", name, e);
            throw new RuntimeException("Failed to delete snapshot", e);
        }
    }

    /**
     * Clean up old snapshots.
     *
     * @param daysOld Delete snapshots older than this many days (not restored recently)
     * @return Number of snapshots deleted
     */
    public int cleanupOldSnapshots(int daysOld) {
        Instant cutoff = Instant.now().minusSeconds(daysOld * 24L * 60 * 60);
        List<DemoSnapshot> staleSnapshots = snapshotRepository.findStaleSnapshots(cutoff);

        int deleted = 0;
        for (DemoSnapshot snapshot : staleSnapshots) {
            try {
                deleteSnapshot(snapshot.getName());
                deleted++;
            } catch (Exception e) {
                logger.warn("Failed to delete stale snapshot: {}", snapshot.getName(), e);
            }
        }

        logger.info("Cleaned up {} old snapshots", deleted);
        return deleted;
    }

    // Private helper methods

    private void clearFhirResources(String tenantId) {
        // These would be the actual FHIR resource tables
        // In demo mode, these tables may not exist - skip gracefully
        String[] tables = {"patients", "conditions", "observations", "medications", "encounters", "procedures"};
        for (String table : tables) {
            safeDeleteFromTable(table, tenantId);
        }
    }

    private void clearCareGaps(String tenantId) {
        safeDeleteFromTable("care_gaps", tenantId);
    }

    private void clearQualityEvaluations(String tenantId) {
        safeDeleteFromTable("quality_evaluations", tenantId);
    }

    /**
     * Check if a table exists in the database.
     */
    private boolean tableExists(String tableName) {
        String sql = "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = ?)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, tableName);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Safely delete from a table that may not exist.
     * Returns number of rows deleted, or 0 if table doesn't exist.
     */
    private int safeDeleteFromTable(String tableName, String tenantId) {
        if (!tableExists(tableName)) {
            logger.debug("Table {} does not exist, skipping cleanup", tableName);
            return 0;
        }
        return jdbcTemplate.update("DELETE FROM " + tableName + " WHERE tenant_id = ?", tenantId);
    }

    /**
     * Safely delete from a table with custom WHERE clause.
     * Returns number of rows deleted, or 0 if table doesn't exist.
     */
    private int safeDeleteWithClause(String tableName, String whereClause) {
        if (!tableExists(tableName)) {
            logger.debug("Table {} does not exist, skipping cleanup", tableName);
            return 0;
        }
        return jdbcTemplate.update("DELETE FROM " + tableName + " WHERE " + whereClause);
    }

    private void clearCaches(String tenantId) {
        // Clear Redis cache entries for tenant
        // This would integrate with Redis cache manager
        logger.debug("Clearing cache for tenant: {}", tenantId);
    }

    private void clearSessionProgress(String tenantId) {
        if (!tableExists("demo_session_progress")) {
            logger.debug("demo_session_progress table does not exist, skipping cleanup");
            return;
        }
        jdbcTemplate.update("DELETE FROM demo_session_progress WHERE tenant_id = ?", tenantId);
    }

    private long executePgDump(Path filePath) throws IOException {
        // In production, would use ProcessBuilder to execute pg_dump
        // For now, create a placeholder file

        // Simplified: create a marker file
        // Real implementation would run: pg_dump -h localhost -p 5436 -U healthdata -d healthdata_demo > filePath
        Files.writeString(filePath, "-- Demo snapshot placeholder\n-- Created: " + Instant.now());

        return Files.size(filePath);
    }

    private void executeRestore(Path filePath) throws IOException {
        // In production, would use ProcessBuilder to execute psql restore
        // Real implementation would run: psql -h localhost -p 5436 -U healthdata -d healthdata_demo < filePath

        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Snapshot file not found: " + filePath);
        }

        logger.debug("Would restore from: {}", filePath);
    }

    // Result classes

    public static class ResetResult {
        private boolean success;
        private int patientsDeleted;
        private int conditionsDeleted;
        private int careGapsDeleted;
        private long resetTimeMs;
        private String errorMessage;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public int getPatientsDeleted() { return patientsDeleted; }
        public void setPatientsDeleted(int patientsDeleted) { this.patientsDeleted = patientsDeleted; }
        public int getConditionsDeleted() { return conditionsDeleted; }
        public void setConditionsDeleted(int conditionsDeleted) { this.conditionsDeleted = conditionsDeleted; }
        public int getCareGapsDeleted() { return careGapsDeleted; }
        public void setCareGapsDeleted(int careGapsDeleted) { this.careGapsDeleted = careGapsDeleted; }
        public long getResetTimeMs() { return resetTimeMs; }
        public void setResetTimeMs(long resetTimeMs) { this.resetTimeMs = resetTimeMs; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class RestoreResult {
        private String snapshotName;
        private boolean success;
        private long restoreTimeMs;
        private String errorMessage;

        // Getters and setters
        public String getSnapshotName() { return snapshotName; }
        public void setSnapshotName(String snapshotName) { this.snapshotName = snapshotName; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public long getRestoreTimeMs() { return restoreTimeMs; }
        public void setRestoreTimeMs(long restoreTimeMs) { this.restoreTimeMs = restoreTimeMs; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}

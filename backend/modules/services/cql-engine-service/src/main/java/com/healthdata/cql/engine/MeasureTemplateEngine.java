package com.healthdata.cql.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.cql.audit.DataFlowTracker;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.event.*;
import com.healthdata.cql.measure.HedisMeasure;
import com.healthdata.cql.measure.MeasureResult;
import com.healthdata.cql.registry.HedisMeasureRegistry;
import com.healthdata.cql.repository.CqlLibraryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Template-Driven Measure Evaluation Engine
 *
 * This engine provides dynamic, high-performance measure evaluation using
 * database-stored templates instead of hardcoded measure classes.
 *
 * Key Features:
 * - Dynamic template loading from database
 * - Redis-based template caching for performance
 * - Thread-safe concurrent evaluation
 * - FHIR-based data retrieval
 * - Supports all HEDIS and custom measures
 *
 * Architecture:
 * 1. Load measure template from database (or cache)
 * 2. Parse template into executable logic
 * 3. Fetch patient data via FHIR
 * 4. Execute measure logic against patient data
 * 5. Return structured results
 */
@Service
public class MeasureTemplateEngine {

    private static final Logger logger = LoggerFactory.getLogger(MeasureTemplateEngine.class);

    private final CqlLibraryRepository libraryRepository;
    private final FHIRDataProvider dataProvider;
    private final TemplateCacheService cacheService;
    private final EvaluationEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    private final DataFlowTracker dataFlowTracker;
    private final HedisMeasureRegistry measureRegistry;
    private final ExecutorService executorService;

    @Value("${visualization.batch-progress.emit-interval-seconds:5}")
    private int progressEmitIntervalSeconds;

    @Value("${visualization.batch-progress.emit-every-n-patients:10}")
    private int progressEmitEveryNPatients;

    public MeasureTemplateEngine(
            CqlLibraryRepository libraryRepository,
            FHIRDataProvider dataProvider,
            TemplateCacheService cacheService,
            EvaluationEventProducer eventProducer,
            ObjectMapper objectMapper,
            DataFlowTracker dataFlowTracker,
            HedisMeasureRegistry measureRegistry) {
        this.libraryRepository = libraryRepository;
        this.dataProvider = dataProvider;
        this.cacheService = cacheService;
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
        this.dataFlowTracker = dataFlowTracker;
        this.measureRegistry = measureRegistry;

        // Thread pool for concurrent evaluation
        int threadCount = Runtime.getRuntime().availableProcessors() * 2;
        this.executorService = Executors.newFixedThreadPool(
                threadCount,
                r -> {
                    Thread t = new Thread(r);
                    t.setName("measure-eval-" + t.threadId());
                    t.setDaemon(true);
                    return t;
                }
        );

        logger.info("MeasureTemplateEngine initialized with {} threads",
                Runtime.getRuntime().availableProcessors() * 2);
    }

    /**
     * Evaluate a measure for a single patient
     *
     * @param measureId Measure identifier (e.g., "HEDIS-CDC")
     * @param patientId Patient identifier
     * @param tenantId Tenant identifier
     * @return Measure evaluation result
     */
    public MeasureResult evaluateMeasure(String measureId, UUID patientId, String tenantId) {
        return evaluateMeasure(measureId, patientId, tenantId, null);
    }

    /**
     * Evaluate a measure for a single patient (internal method with batchId support)
     */
    private MeasureResult evaluateMeasure(String measureId, UUID patientId, String tenantId, String batchId) {
        long startTime = System.currentTimeMillis();
        UUID evaluationId = UUID.randomUUID();

        try {
            logger.debug("Evaluating measure {} for patient {} / tenant {}", measureId, patientId, tenantId);

            // First, check if we have a registered Java measure implementation
            Optional<HedisMeasure> registeredMeasure = measureRegistry.getMeasure(measureId);
            if (registeredMeasure.isPresent()) {
                HedisMeasure measure = registeredMeasure.get();
                String measureName = measure.getMeasureName();

                logger.debug("Using registered Java measure {} for evaluation", measure.getClass().getSimpleName());

                // Publish evaluation started event
                eventProducer.publishEvaluationStarted(
                        EvaluationStartedEvent.builder()
                                .evaluationId(evaluationId)
                                .tenantId(tenantId)
                                .measureId(measureId)
                                .measureName(measureName)
                                .patientId(patientId)
                                .batchId(batchId)
                                .build()
                );

                // Track that we're using a Java measure
                dataFlowTracker.recordStep(
                    "Load Measure Implementation",
                    "LOGIC_DECISION",
                    String.format("Using Java implementation: %s", measure.getClass().getSimpleName()),
                    String.format("Registered HEDIS measure found for %s, delegating to Java class", measureId)
                );

                // Execute measure using the registered Java implementation
                MeasureResult result = measure.evaluate(tenantId, patientId);

                long duration = System.currentTimeMillis() - startTime;
                logger.debug("Evaluated measure {} for patient {} using Java impl in {}ms", measureId, patientId, duration);

                // Publish evaluation completed event
                eventProducer.publishEvaluationCompleted(
                        EvaluationCompletedEvent.builder()
                                .evaluationId(evaluationId)
                                .tenantId(tenantId)
                                .measureId(measureId)
                                .measureName(measureName)
                                .patientId(patientId)
                                .batchId(batchId)
                                .inDenominator(result.isInDenominator())
                                .inNumerator(result.isInNumerator())
                                .exclusionReason(result.getExclusionReason())
                                .complianceRate(result.getComplianceRate() != null ? result.getComplianceRate() : 0.0)
                                .score(result.getScore() != null ? result.getScore() : 0.0)
                                .durationMs(duration)
                                .evidence(result.getEvidence())
                                .careGapCount(result.getCareGaps() != null ? result.getCareGaps().size() : 0)
                                .build()
                );

                return result;
            }

            // Fall back to template-based evaluation
            logger.debug("No registered Java measure for {}, using template-based evaluation", measureId);

            // Load template (from cache or database)
            MeasureTemplate template = loadTemplate(measureId, tenantId);
            if (template == null) {
                logger.warn("Template not found for measure {} / tenant {}", measureId, tenantId);
                return buildErrorResult(measureId, patientId, "Template not found");
            }

            if (!template.isActive()) {
                logger.warn("Template {} is not active", template.getTemplateId());
                return buildErrorResult(measureId, patientId, "Template not active");
            }

            // Publish evaluation started event
            eventProducer.publishEvaluationStarted(
                    EvaluationStartedEvent.builder()
                            .evaluationId(evaluationId)
                            .tenantId(tenantId)
                            .measureId(measureId)
                            .measureName(template.getMeasureName())
                            .patientId(patientId)
                            .batchId(batchId)
                            .build()
            );

            // Load patient context
            FHIRDataProvider.PatientContext context = dataProvider.getPatientContext(tenantId, patientId);

            // Execute measure logic
            MeasureResult result = executeMeasureLogic(template, context);

            long duration = System.currentTimeMillis() - startTime;
            logger.debug("Evaluated measure {} for patient {} in {}ms", measureId, patientId, duration);

            // Track result publication
            dataFlowTracker.recordStep(
                "Publish Evaluation Result",
                "DATA_TRANSFORM",
                String.format("Publishing result: %s (numerator=%s, denominator=%s)",
                    result.isInNumerator() ? "IN" : "OUT",
                    result.isInNumerator(),
                    result.isInDenominator()),
                String.format("Measure evaluation completed in %dms, publishing to Kafka", duration)
            );

            // Publish evaluation completed event
            eventProducer.publishEvaluationCompleted(
                    EvaluationCompletedEvent.builder()
                            .evaluationId(evaluationId)
                            .tenantId(tenantId)
                            .measureId(measureId)
                            .measureName(template.getMeasureName())
                            .patientId(patientId)
                            .batchId(batchId)
                            .inDenominator(result.isInDenominator())
                            .inNumerator(result.isInNumerator())
                            .exclusionReason(result.getExclusionReason())
                            .complianceRate(result.getComplianceRate() != null ? result.getComplianceRate() : 0.0)
                            .score(result.getScore() != null ? result.getScore() : 0.0)
                            .durationMs(duration)
                            .evidence(result.getEvidence())
                            .careGapCount(result.getCareGaps() != null ? result.getCareGaps().size() : 0)
                            .build()
            );

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Error evaluating measure {} for patient {}: {}", measureId, patientId, e.getMessage(), e);

            // Publish evaluation failed event
            eventProducer.publishEvaluationFailed(
                    EvaluationFailedEvent.builder()
                            .evaluationId(evaluationId)
                            .tenantId(tenantId)
                            .measureId(measureId)
                            .measureName(measureId)  // Use measureId as fallback
                            .patientId(patientId)
                            .batchId(batchId)
                            .errorMessage(e.getMessage())
                            .errorType(categorizeError(e))
                            .durationMs(duration)
                            .build()
            );

            return buildErrorResult(measureId, patientId, "Evaluation error: " + e.getMessage());
        }
    }

    /**
     * Evaluate a measure for multiple patients concurrently
     *
     * @param measureId Measure identifier
     * @param patientIds List of patient identifiers
     * @param tenantId Tenant identifier
     * @return Map of patient ID to evaluation result
     */
    public Map<UUID, MeasureResult> evaluateBatch(String measureId, List<UUID> patientIds, String tenantId) {
        long batchStartTime = System.currentTimeMillis();
        String batchId = UUID.randomUUID().toString();

        logger.info("Starting batch evaluation {} for measure {} with {} patients / tenant {}",
                batchId, measureId, patientIds.size(), tenantId);

        // Load template once for all patients
        MeasureTemplate template = loadTemplate(measureId, tenantId);
        if (template == null) {
            logger.error("Template not found for measure {} / tenant {}", measureId, tenantId);
            Map<UUID, MeasureResult> results = new ConcurrentHashMap<>();
            for (UUID patientId : patientIds) {
                results.put(patientId, buildErrorResult(measureId, patientId, "Template not found"));
            }
            return results;
        }

        // Progress tracking
        AtomicInteger completedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);
        AtomicInteger denominatorCount = new AtomicInteger(0);
        AtomicInteger numeratorCount = new AtomicInteger(0);
        AtomicLong totalDurationMs = new AtomicLong(0);

        Map<UUID, MeasureResult> results = new ConcurrentHashMap<>();

        // Schedule periodic progress reporting
        ScheduledExecutorService progressReporter = Executors.newSingleThreadScheduledExecutor();
        AtomicLong lastProgressEmitTime = new AtomicLong(System.currentTimeMillis());
        AtomicInteger lastEmittedCount = new AtomicInteger(0);

        Runnable progressTask = () -> {
            emitBatchProgress(batchId, measureId, template.getMeasureName(), tenantId,
                    patientIds.size(), completedCount.get(), successCount.get(),
                    failedCount.get(), denominatorCount.get(), numeratorCount.get(),
                    totalDurationMs.get(), batchStartTime);
            lastProgressEmitTime.set(System.currentTimeMillis());
            lastEmittedCount.set(completedCount.get());
        };

        // Start periodic progress reporting
        progressReporter.scheduleAtFixedRate(progressTask,
                progressEmitIntervalSeconds, progressEmitIntervalSeconds, TimeUnit.SECONDS);

        // Create concurrent evaluation tasks
        List<CompletableFuture<Map.Entry<UUID, MeasureResult>>> futures = new ArrayList<>();

        for (UUID patientId : patientIds) {
            CompletableFuture<Map.Entry<UUID, MeasureResult>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    long evalStartTime = System.currentTimeMillis();
                    MeasureResult result = evaluateMeasure(measureId, patientId, tenantId, batchId);
                    long evalDuration = System.currentTimeMillis() - evalStartTime;

                    // Update counters
                    completedCount.incrementAndGet();
                    totalDurationMs.addAndGet(evalDuration);

                    if (result.getExclusionReason() == null || !result.getExclusionReason().startsWith("Error:")) {
                        successCount.incrementAndGet();
                        if (result.isInDenominator()) {
                            denominatorCount.incrementAndGet();
                            if (result.isInNumerator()) {
                                numeratorCount.incrementAndGet();
                            }
                        }
                    } else {
                        failedCount.incrementAndGet();
                    }

                    // Emit progress if we've completed N patients since last emit
                    int completed = completedCount.get();
                    if (completed - lastEmittedCount.get() >= progressEmitEveryNPatients) {
                        emitBatchProgress(batchId, measureId, template.getMeasureName(), tenantId,
                                patientIds.size(), completed, successCount.get(),
                                failedCount.get(), denominatorCount.get(), numeratorCount.get(),
                                totalDurationMs.get(), batchStartTime);
                        lastEmittedCount.set(completed);
                        lastProgressEmitTime.set(System.currentTimeMillis());
                    }

                    return Map.entry(patientId, result);
                } catch (Exception e) {
                    logger.error("Error in batch evaluation for patient {}: {}", patientId, e.getMessage());
                    completedCount.incrementAndGet();
                    failedCount.incrementAndGet();
                    return Map.entry(patientId, buildErrorResult(measureId, patientId, e.getMessage()));
                }
            }, executorService);

            futures.add(future);
        }

        // Wait for all evaluations to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            allOf.get(5, TimeUnit.MINUTES); // 5 minute timeout for batch

            // Collect results
            for (CompletableFuture<Map.Entry<UUID, MeasureResult>> future : futures) {
                Map.Entry<UUID, MeasureResult> entry = future.get();
                results.put(entry.getKey(), entry.getValue());
            }

        } catch (TimeoutException e) {
            logger.error("Batch evaluation timed out after 5 minutes");
        } catch (Exception e) {
            logger.error("Error in batch evaluation: {}", e.getMessage(), e);
        } finally {
            // Stop progress reporter
            progressReporter.shutdown();

            // Emit final progress event
            emitBatchProgress(batchId, measureId, template.getMeasureName(), tenantId,
                    patientIds.size(), completedCount.get(), successCount.get(),
                    failedCount.get(), denominatorCount.get(), numeratorCount.get(),
                    totalDurationMs.get(), batchStartTime);

            dataProvider.clearCache();
        }

        long duration = System.currentTimeMillis() - batchStartTime;
        logger.info("Completed batch evaluation {} of {} patients in {}ms (avg {}ms per patient)",
                batchId, results.size(), duration, duration / Math.max(1, results.size()));

        return results;
    }

    /**
     * Emit a batch progress event
     */
    private void emitBatchProgress(String batchId, String measureId, String measureName, String tenantId,
                                    int totalPatients, int completedCount, int successCount, int failedCount,
                                    int denominatorCount, int numeratorCount,
                                    long totalDurationMs, long batchStartTime) {
        int pendingCount = totalPatients - completedCount;
        double avgDurationMs = completedCount > 0 ? (double) totalDurationMs / completedCount : 0.0;
        long elapsedTimeMs = System.currentTimeMillis() - batchStartTime;
        double currentThroughput = elapsedTimeMs > 0 ? (completedCount * 1000.0 / elapsedTimeMs) : 0.0;
        long estimatedTimeRemainingMs = currentThroughput > 0 ? (long) (pendingCount / currentThroughput * 1000) : 0;
        double cumulativeComplianceRate = denominatorCount > 0 ? (numeratorCount * 100.0 / denominatorCount) : 0.0;

        BatchProgressEvent progressEvent = BatchProgressEvent.builder()
                .batchId(batchId)
                .tenantId(tenantId)
                .measureId(measureId)
                .measureName(measureName)
                .totalPatients(totalPatients)
                .completedCount(completedCount)
                .successCount(successCount)
                .failedCount(failedCount)
                .pendingCount(pendingCount)
                .avgDurationMs(avgDurationMs)
                .currentThroughput(currentThroughput)
                .elapsedTimeMs(elapsedTimeMs)
                .estimatedTimeRemainingMs(estimatedTimeRemainingMs)
                .denominatorCount(denominatorCount)
                .numeratorCount(numeratorCount)
                .cumulativeComplianceRate(cumulativeComplianceRate)
                .build();

        eventProducer.publishBatchProgress(progressEvent);
    }

    /**
     * Load a measure template (from cache or database)
     *
     * @param measureId Measure identifier
     * @param tenantId Tenant identifier
     * @return Parsed measure template
     */
    public MeasureTemplate loadTemplate(String measureId, String tenantId) {
        // Try cache first
        MeasureTemplate cached = cacheService.getTemplateByMeasureId(measureId, tenantId);
        if (cached != null) {
            // Track cache hit
            dataFlowTracker.recordStep(
                "Load Measure Template",
                "CACHE_LOOKUP",
                String.format("Template loaded for measure %s from cache", measureId),
                "Retrieved cached template - no database query needed"
            );
            return cached;
        }

        // Load from database - get latest version
        logger.debug("Loading template for measure {} / tenant {} from database", measureId, tenantId);

        Optional<CqlLibrary> libraryOpt = libraryRepository.findLatestVersionByName(tenantId, measureId);

        if (libraryOpt.isEmpty()) {
            logger.warn("No active library found for measure {} / tenant {}", measureId, tenantId);
            return null;
        }

        CqlLibrary library = libraryOpt.get();

        // Parse library into template
        MeasureTemplate template = parseTemplate(library);

        // Cache for future use
        if (template != null) {
            cacheService.putTemplate(template);

            // Track database load
            dataFlowTracker.recordStep(
                "Load Measure Template",
                "DATA_FETCH",
                String.format("Template loaded for measure %s from database and cached", measureId),
                String.format("Retrieved library %s v%s, parsed into template",
                    library.getName(), library.getVersion())
            );
        }

        return template;
    }

    /**
     * Parse a CqlLibrary entity into a MeasureTemplate
     *
     * @param library CQL library entity
     * @return Parsed measure template
     */
    private MeasureTemplate parseTemplate(CqlLibrary library) {
        try {
            // Parse ELM JSON if available
            Map<String, Object> denominatorCriteria = new HashMap<>();
            Map<String, Object> numeratorCriteria = new HashMap<>();
            Map<String, Object> exclusionCriteria = new HashMap<>();

            if (library.getElmJson() != null && !library.getElmJson().isBlank()) {
                JsonNode elmRoot = objectMapper.readTree(library.getElmJson());

                // Extract criteria from ELM
                // This is a simplified parser - real implementation would use full ELM parsing
                if (elmRoot.has("library") && elmRoot.get("library").has("statements")) {
                    JsonNode statements = elmRoot.get("library").get("statements").get("def");
                    if (statements.isArray()) {
                        for (JsonNode statement : statements) {
                            String name = statement.has("name") ? statement.get("name").asText() : "";

                            if (name.toLowerCase().contains("denominator")) {
                                denominatorCriteria.put(name, statement);
                            } else if (name.toLowerCase().contains("numerator")) {
                                numeratorCriteria.put(name, statement);
                            } else if (name.toLowerCase().contains("exclusion")) {
                                exclusionCriteria.put(name, statement);
                            }
                        }
                    }
                }
            }

            return MeasureTemplate.builder()
                    .templateId(library.getId())
                    .measureId(library.getLibraryName())
                    .measureName(library.getDescription() != null ? library.getDescription() : library.getLibraryName())
                    .version(library.getVersion())
                    .cqlContent(library.getCqlContent())
                    .elmJson(library.getElmJson())
                    .denominatorCriteria(denominatorCriteria)
                    .numeratorCriteria(numeratorCriteria)
                    .exclusionCriteria(exclusionCriteria)
                    .requiredResourceTypes(new String[]{"Patient", "Observation", "Condition", "Procedure", "MedicationRequest", "Encounter"})
                    .valueSets(new HashMap<>())
                    .metadata(new HashMap<>())
                    .tenantId(library.getTenantId())
                    .active(library.getActive() != null && library.getActive())
                    .build();

        } catch (Exception e) {
            logger.error("Error parsing template from library {}: {}", library.getId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Execute measure logic against patient context
     *
     * This is a simplified implementation that demonstrates the template-driven approach.
     * A full implementation would:
     * - Parse and execute ELM expressions
     * - Evaluate complex CQL logic
     * - Handle value set lookups
     * - Support all CQL operators and functions
     *
     * @param template Measure template
     * @param context Patient context with FHIR data
     * @return Measure result
     */
    private MeasureResult executeMeasureLogic(MeasureTemplate template, FHIRDataProvider.PatientContext context) {
        try {
            // Calculate patient age
            Integer patientAge = calculateAge(context.getPatient());

            // Determine if patient is in denominator (eligible)
            boolean inDenominator = evaluateDenominator(template, context, patientAge);

            // Track denominator evaluation
            dataFlowTracker.recordStep(
                "Evaluate Denominator (Eligibility)",
                "LOGIC_DECISION",
                String.format("Patient %s in denominator", inDenominator ? "IS" : "NOT"),
                String.format("Measure: %s, Patient age: %d, Eligible: %s",
                    template.getMeasureId(), patientAge, inDenominator)
            );

            // Determine if patient is in numerator (compliant)
            boolean inNumerator = false;
            String exclusionReason = null;

            if (inDenominator) {
                // Check exclusions
                exclusionReason = evaluateExclusions(template, context, patientAge);

                // Track exclusion evaluation
                dataFlowTracker.recordStep(
                    "Evaluate Exclusions",
                    "LOGIC_DECISION",
                    exclusionReason != null ? "Patient EXCLUDED" : "No exclusions",
                    exclusionReason != null ?
                        String.format("Exclusion reason: %s", exclusionReason) :
                        "Patient meets all inclusion criteria, no exclusions found"
                );

                if (exclusionReason == null) {
                    // Evaluate numerator
                    inNumerator = evaluateNumerator(template, context, patientAge);

                    // Track numerator evaluation
                    dataFlowTracker.recordStep(
                        "Evaluate Numerator (Compliance)",
                        "LOGIC_DECISION",
                        String.format("Patient %s in numerator", inNumerator ? "IS" : "NOT"),
                        String.format("Measure: %s, Compliant: %s",
                            template.getMeasureId(), inNumerator)
                    );
                }
            }

            // Build result
            MeasureResult.MeasureResultBuilder builder = MeasureResult.builder()
                    .measureId(template.getMeasureId())
                    .measureName(template.getMeasureName())
                    .patientId(context.getPatientId())
                    .evaluationDate(LocalDate.now())
                    .inDenominator(inDenominator)
                    .inNumerator(inNumerator)
                    .exclusionReason(exclusionReason);

            // Calculate compliance rate
            if (inDenominator && exclusionReason == null) {
                builder.complianceRate(inNumerator ? 1.0 : 0.0);
                builder.score(inNumerator ? 100.0 : 0.0);
            }

            // Add evidence
            Map<String, Object> evidence = new HashMap<>();
            evidence.put("patientAge", patientAge);
            evidence.put("observationCount", context.getObservations().size());
            evidence.put("conditionCount", context.getConditions().size());
            evidence.put("procedureCount", context.getProcedures().size());
            builder.evidence(evidence);

            return builder.build();

        } catch (Exception e) {
            logger.error("Error executing measure logic: {}", e.getMessage(), e);
            return buildErrorResult(template.getMeasureId(), context.getPatientId(), e.getMessage());
        }
    }

    /**
     * Evaluate denominator criteria (eligibility)
     */
    private boolean evaluateDenominator(MeasureTemplate template, FHIRDataProvider.PatientContext context, Integer patientAge) {
        // Simplified logic - real implementation would parse and execute ELM
        // Most HEDIS measures have age-based eligibility
        if (patientAge == null) {
            return false;
        }

        String measureId = template.getMeasureId();

        // CDC (Comprehensive Diabetes Care): Age 18-75 with diabetes
        if (measureId.contains("CDC")) {
            boolean inAgeRange = patientAge >= 18 && patientAge <= 75;
            boolean hasDiabetes = hasDiabetesDiagnosis(context);
            return inAgeRange && hasDiabetes;
        }

        // CBP (Controlling Blood Pressure): Age 18-85 with hypertension
        if (measureId.contains("CBP")) {
            boolean inAgeRange = patientAge >= 18 && patientAge <= 85;
            boolean hasHypertension = hasHypertensionDiagnosis(context);
            return inAgeRange && hasHypertension;
        }

        // COL (Colorectal Cancer Screening): Age 50-75
        if (measureId.contains("COL")) {
            return patientAge >= 50 && patientAge <= 75;
        }

        // BCS (Breast Cancer Screening): Age 50-74, female, no bilateral mastectomy
        if (measureId.contains("BCS")) {
            boolean inAgeRange = patientAge >= 50 && patientAge <= 74;
            boolean isFemale = isFemalePatient(context);
            boolean hasMastectomy = hasBilateralMastectomy(context);
            return inAgeRange && isFemale && !hasMastectomy;
        }

        // CIS (Childhood Immunization Status): Children who turned 2 during measurement period
        if (measureId.contains("CIS")) {
            // Age 24-35 months (turned 2 within the year)
            return patientAge >= 2 && patientAge <= 3;
        }

        // AWC (Adolescent Well-Care Visits): Age 12-21
        if (measureId.contains("AWC")) {
            return patientAge >= 12 && patientAge <= 21;
        }

        // Default: eligible if we have patient data
        return context.getPatient() != null;
    }

    /**
     * Evaluate numerator criteria (compliance)
     */
    private boolean evaluateNumerator(MeasureTemplate template, FHIRDataProvider.PatientContext context, Integer patientAge) {
        // Simplified logic - real implementation would parse and execute ELM
        String measureId = template.getMeasureId();

        // Example logic for common measures
        if (measureId.contains("CDC")) {
            // Diabetes care: HbA1c < 8% or BP < 140/90
            return hasRecentHbA1cControl(context) || hasRecentBPControl(context);
        } else if (measureId.contains("CBP")) {
            // Blood pressure control: BP < 140/90
            return hasRecentBPControl(context);
        } else if (measureId.contains("COL")) {
            // Colorectal screening: colonoscopy in past 10 years
            return hasRecentColonoscopy(context);
        } else if (measureId.contains("BCS")) {
            // Breast cancer screening: mammogram in last 27 months
            return hasRecentMammogram(context);
        } else if (measureId.contains("CIS")) {
            // Childhood immunizations: check for required vaccines
            return hasRequiredImmunizations(context, patientAge);
        } else if (measureId.contains("AWC")) {
            // Adolescent well-care: check for annual visit
            return hasWellCareVisit(context);
        }

        // Default: not compliant (measures must prove compliance)
        return false;
    }

    /**
     * Evaluate exclusion criteria
     */
    private String evaluateExclusions(MeasureTemplate template, FHIRDataProvider.PatientContext context, Integer patientAge) {
        // Simplified logic - real implementation would parse and execute ELM
        // Common exclusions: hospice care, end-stage disease, etc.

        // Check for hospice care condition
        for (JsonNode condition : context.getConditions()) {
            if (hasCode(condition, Arrays.asList("385763009"))) { // SNOMED: Hospice care
                return "Patient in hospice care";
            }
        }

        return null; // No exclusions
    }

    // Helper methods for measure-specific logic

    private boolean hasRecentHbA1cControl(FHIRDataProvider.PatientContext context) {
        for (JsonNode obs : context.getObservations()) {
            if (hasCode(obs, Arrays.asList("4548-4", "17856-6"))) { // LOINC: HbA1c
                if (isWithinMonths(getEffectiveDate(obs), 12)) {
                    Double value = getObservationValue(obs);
                    if (value != null && value < 8.0) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasRecentBPControl(FHIRDataProvider.PatientContext context) {
        for (JsonNode obs : context.getObservations()) {
            if (hasCode(obs, Arrays.asList("85354-9"))) { // LOINC: Blood pressure panel
                if (isWithinMonths(getEffectiveDate(obs), 12)) {
                    // Check systolic < 140 and diastolic < 90
                    return true; // Simplified - would check actual values
                }
            }
        }
        return false;
    }

    private boolean hasRecentColonoscopy(FHIRDataProvider.PatientContext context) {
        for (JsonNode proc : context.getProcedures()) {
            if (hasCode(proc, Arrays.asList("73761001", "446521004"))) { // SNOMED: Colonoscopy
                if (isWithinMonths(getEffectiveDate(proc), 120)) { // 10 years
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasDiabetesDiagnosis(FHIRDataProvider.PatientContext context) {
        for (JsonNode condition : context.getConditions()) {
            // Check for Type 1 (46635009) or Type 2 (44054006) diabetes
            if (hasCode(condition, Arrays.asList("44054006", "46635009"))) {
                // Verify condition is active
                if (isActiveCondition(condition)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasHypertensionDiagnosis(FHIRDataProvider.PatientContext context) {
        for (JsonNode condition : context.getConditions()) {
            // Check for essential hypertension (38341003)
            if (hasCode(condition, Arrays.asList("38341003"))) {
                if (isActiveCondition(condition)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isActiveCondition(JsonNode condition) {
        if (condition.has("clinicalStatus")) {
            JsonNode status = condition.get("clinicalStatus");
            if (status.has("coding")) {
                for (JsonNode coding : status.get("coding")) {
                    String code = coding.has("code") ? coding.get("code").asText() : null;
                    if ("active".equals(code)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if patient is female
     */
    private boolean isFemalePatient(FHIRDataProvider.PatientContext context) {
        JsonNode patient = context.getPatient();
        if (patient != null && patient.has("gender")) {
            String gender = patient.get("gender").asText();
            return "female".equalsIgnoreCase(gender);
        }
        return false;
    }

    /**
     * Check if patient has had bilateral mastectomy or two unilateral mastectomies
     */
    private boolean hasBilateralMastectomy(FHIRDataProvider.PatientContext context) {
        // Check for bilateral mastectomy procedure
        for (JsonNode procedure : context.getProcedures()) {
            // SNOMED codes for bilateral mastectomy
            if (hasCode(procedure, Arrays.asList("428571003", "27865001", "456903003"))) {
                if (isCompletedProcedure(procedure)) {
                    return true;
                }
            }
        }

        // Check for two unilateral mastectomies
        int unilateralCount = 0;
        for (JsonNode procedure : context.getProcedures()) {
            // SNOMED codes for unilateral mastectomy
            if (hasCode(procedure, Arrays.asList("172043006", "384723003", "234254000"))) {
                if (isCompletedProcedure(procedure)) {
                    unilateralCount++;
                }
            }
        }

        return unilateralCount >= 2;
    }

    /**
     * Check if patient has had mammogram in last 27 months
     */
    private boolean hasRecentMammogram(FHIRDataProvider.PatientContext context) {
        for (JsonNode procedure : context.getProcedures()) {
            // CPT codes: 77067 (screening mammography), 77063 (digital tomosynthesis)
            // SNOMED codes: 24623002 (screening mammography), 241055006 (mammography screening)
            if (hasCode(procedure, Arrays.asList("77067", "77063", "24623002", "241055006"))) {
                if (isCompletedProcedure(procedure) && isWithinMonths(getEffectiveDate(procedure), 27)) {
                    return true;
                }
            }
        }

        // Also check observations for mammogram results
        for (JsonNode obs : context.getObservations()) {
            // LOINC code: 24604-1 (Mammogram)
            if (hasCode(obs, Arrays.asList("24604-1"))) {
                if (isWithinMonths(getEffectiveDate(obs), 27)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if a procedure has completed status
     */
    private boolean isCompletedProcedure(JsonNode procedure) {
        if (procedure != null && procedure.has("status")) {
            String status = procedure.get("status").asText();
            return "completed".equalsIgnoreCase(status);
        }
        return false;
    }

    /**
     * Check if child has required immunizations (CIS measure)
     * Simplified logic - checks for presence of immunization records
     */
    private boolean hasRequiredImmunizations(FHIRDataProvider.PatientContext context, Integer patientAge) {
        // CIS measure requires 10 different vaccine series
        // For this placeholder implementation, we'll check if the child has at least some immunization records

        // In a real implementation, this would check for specific vaccine codes and counts:
        // - 4 DTaP (CVX codes: 20, 106, 107, 110, 120, 130)
        // - 3 IPV (CVX codes: 10, 89, 110, 120, 130)
        // - 1 MMR (CVX codes: 03, 94)
        // - 3 HiB (CVX codes: 17, 46, 47, 48, 49, 120)
        // - 3 HepB (CVX codes: 08, 44, 110)
        // - 1 VZV (CVX codes: 21, 94)
        // - 4 PCV (CVX codes: 100, 133, 152)
        // - 1 HepA (CVX codes: 83, 85)
        // - 2 RV (CVX codes: 116, 119, 122)
        // - 2 Flu (CVX codes: 88, 135, 140, 141, 150, 153, 155, 161)

        // For now, simplified check: look for immunization records with relevant CVX codes
        int immunizationCount = 0;

        // Get all procedures (immunizations may be recorded as procedures or immunization resources)
        for (JsonNode proc : context.getProcedures()) {
            // Check for common vaccine CVX codes
            if (hasCode(proc, Arrays.asList(
                "20", "106", "107",  // DTaP
                "10", "89",          // IPV
                "03", "94",          // MMR
                "17", "46", "47",    // HiB
                "08", "44",          // HepB
                "21",                // VZV
                "133", "152",        // PCV
                "83", "85",          // HepA
                "116", "119",        // RV
                "88", "140", "141"   // Flu
            ))) {
                if (isCompletedProcedure(proc)) {
                    immunizationCount++;
                }
            }
        }

        // Simplified compliance: Consider compliant if child has at least 10 immunization records
        // (representing completion of all vaccine series)
        return immunizationCount >= 10;
    }

    /**
     * Check if adolescent has had a well-care visit (AWC measure)
     * Simplified logic - checks for preventive care encounters
     */
    private boolean hasWellCareVisit(FHIRDataProvider.PatientContext context) {
        // AWC measure requires at least one comprehensive well-care visit in the past year
        // This includes preventive medicine visits, annual wellness visits, etc.

        // In a real implementation, this would check for specific CPT/HCPCS codes:
        // - 99384, 99385, 99394, 99395 (Preventive medicine visits)
        // - G0438, G0439 (Annual wellness visits)
        // - Office visits (99201-99215) with preventive care reason codes
        // - SNOMED codes: 410620009 (Well child visit), 185349003 (Check up)

        // For this placeholder, look for encounters with preventive/wellness indicators
        for (JsonNode encounter : context.getEncounters()) {
            // Check for preventive care CPT codes or wellness visit indicators
            if (hasCode(encounter, Arrays.asList(
                "99384", "99385",      // Preventive medicine - initial
                "99394", "99395",      // Preventive medicine - periodic
                "G0438", "G0439",      // Annual wellness visit
                "410620009",           // SNOMED: Well child visit
                "185349003",           // SNOMED: Check up
                "308335008"            // SNOMED: Patient encounter
            ))) {
                // Check if encounter is recent (within last 12 months)
                if (isWithinMonths(getEncounterDate(encounter), 12)) {
                    if (isCompletedEncounter(encounter)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Get encounter date from encounter resource
     */
    private String getEncounterDate(JsonNode encounter) {
        if (encounter.has("period") && encounter.get("period").has("start")) {
            return encounter.get("period").get("start").asText();
        }
        return null;
    }

    /**
     * Check if encounter is completed
     */
    private boolean isCompletedEncounter(JsonNode encounter) {
        if (encounter != null && encounter.has("status")) {
            String status = encounter.get("status").asText();
            return "finished".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status);
        }
        return false;
    }

    // Utility methods

    private Integer calculateAge(JsonNode patient) {
        try {
            if (patient != null && patient.has("birthDate")) {
                String birthDate = patient.get("birthDate").asText();
                LocalDate birth = LocalDate.parse(birthDate);
                return Period.between(birth, LocalDate.now()).getYears();
            }
        } catch (Exception e) {
            logger.warn("Could not calculate patient age: {}", e.getMessage());
        }
        return null;
    }

    private boolean hasCode(JsonNode resource, List<String> targetCodes) {
        if (resource == null || !resource.has("code")) {
            return false;
        }

        JsonNode code = resource.get("code");
        if (code.has("coding")) {
            for (JsonNode coding : code.get("coding")) {
                String codeValue = coding.has("code") ? coding.get("code").asText() : null;
                if (codeValue != null && targetCodes.contains(codeValue)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isWithinMonths(String dateStr, int months) {
        if (dateStr == null) return false;
        try {
            LocalDate date = LocalDate.parse(dateStr.substring(0, 10));
            LocalDate cutoff = LocalDate.now().minusMonths(months);
            return date.isAfter(cutoff);
        } catch (Exception e) {
            return false;
        }
    }

    private String getEffectiveDate(JsonNode resource) {
        if (resource.has("effectiveDateTime")) {
            return resource.get("effectiveDateTime").asText();
        } else if (resource.has("performedDateTime")) {
            return resource.get("performedDateTime").asText();
        } else if (resource.has("recordedDate")) {
            return resource.get("recordedDate").asText();
        }
        return null;
    }

    private Double getObservationValue(JsonNode observation) {
        try {
            if (observation.has("valueQuantity") && observation.get("valueQuantity").has("value")) {
                return observation.get("valueQuantity").get("value").asDouble();
            }
        } catch (Exception e) {
            logger.debug("Could not extract observation value: {}", e.getMessage());
        }
        return null;
    }

    private MeasureResult buildErrorResult(String measureId, UUID patientId, String error) {
        return MeasureResult.builder()
                .measureId(measureId)
                .patientId(patientId)
                .evaluationDate(LocalDate.now())
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Error: " + error)
                .build();
    }

    /**
     * Categorize error for event publishing
     */
    private String categorizeError(Exception e) {
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        String exceptionType = e.getClass().getSimpleName();

        if (errorMessage.contains("fhir") || errorMessage.contains("patient") || errorMessage.contains("resource")) {
            return "FHIR_FETCH_ERROR";
        } else if (errorMessage.contains("template") || errorMessage.contains("cql") || errorMessage.contains("parse")) {
            return "CQL_PARSE_ERROR";
        } else if (errorMessage.contains("timeout")) {
            return "TIMEOUT_ERROR";
        } else if (exceptionType.contains("NullPointer")) {
            return "NULL_POINTER_ERROR";
        } else if (exceptionType.contains("IllegalArgument")) {
            return "VALIDATION_ERROR";
        } else {
            return "RUNTIME_ERROR";
        }
    }

    /**
     * Shutdown the executor service
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

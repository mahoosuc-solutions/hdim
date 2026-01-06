package com.healthdata.demo.config;

import com.healthdata.demo.application.ScenarioLoaderService;
import com.healthdata.demo.domain.repository.DemoScenarioRepository;
import com.healthdata.demo.domain.repository.DemoSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Warms up caches on application startup.
 *
 * Pre-loads commonly accessed demo data to ensure fast response times
 * for the first demo interactions. This is especially important for
 * sales demos where performance is critical.
 */
@Component
@ConditionalOnProperty(name = "demo.cache.warmup.enabled", havingValue = "true", matchIfMissing = true)
public class CacheWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final DemoScenarioRepository scenarioRepository;
    private final DemoSnapshotRepository snapshotRepository;
    private final ScenarioLoaderService scenarioLoaderService;

    public CacheWarmupRunner(
            DemoScenarioRepository scenarioRepository,
            DemoSnapshotRepository snapshotRepository,
            ScenarioLoaderService scenarioLoaderService) {
        this.scenarioRepository = scenarioRepository;
        this.snapshotRepository = snapshotRepository;
        this.scenarioLoaderService = scenarioLoaderService;
    }

    @Override
    @Async("demoTaskExecutor")
    public void run(ApplicationArguments args) {
        log.info("Starting cache warmup for demo data...");
        long startTime = System.currentTimeMillis();

        try {
            warmupScenarios();
            warmupSnapshots();
            warmupCurrentSession();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warmup completed in {}ms", duration);
        } catch (Exception e) {
            log.warn("Cache warmup failed (non-fatal): {}", e.getMessage());
        }
    }

    private void warmupScenarios() {
        try {
            log.debug("Warming up scenario cache...");
            // Fetch all scenarios to populate cache
            long count = scenarioRepository.count();
            scenarioRepository.findAll();
            log.debug("Scenario cache warmed up ({} scenarios)", count);
        } catch (Exception e) {
            log.warn("Failed to warm scenario cache: {}", e.getMessage());
        }
    }

    private void warmupSnapshots() {
        try {
            log.debug("Warming up snapshot cache...");
            // Fetch all snapshots to populate cache
            long count = snapshotRepository.count();
            snapshotRepository.findAll();
            log.debug("Snapshot cache warmed up ({} snapshots)", count);
        } catch (Exception e) {
            log.warn("Failed to warm snapshot cache: {}", e.getMessage());
        }
    }

    private void warmupCurrentSession() {
        try {
            log.debug("Warming up current session cache...");
            // Trigger current scenario lookup to warm query cache
            scenarioLoaderService.getCurrentScenario();
            log.debug("Current session cache warmed up");
        } catch (Exception e) {
            log.warn("Failed to warm current session cache: {}", e.getMessage());
        }
    }
}

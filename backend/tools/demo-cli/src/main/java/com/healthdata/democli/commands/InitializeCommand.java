package com.healthdata.democli.commands;

import com.healthdata.democli.DemoApiClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Initialize the demo environment with default data.
 */
@Component
@Command(
    name = "initialize",
    description = "Initialize demo environment with default scenarios and data",
    mixinStandardHelpOptions = true
)
public class InitializeCommand implements Callable<Integer> {

    private final DemoApiClient apiClient;

    @Option(names = {"--scenario"}, description = "Scenario to load after initialization (default: hedis-evaluation)", defaultValue = "hedis-evaluation")
    private String scenario;

    @Option(names = {"--patients"}, description = "Number of patients to generate (default: 5000)", defaultValue = "5000")
    private int patientCount;

    @Option(names = {"--skip-patients"}, description = "Skip patient generation")
    private boolean skipPatients;

    @Option(names = {"--create-snapshot"}, description = "Create a baseline snapshot after initialization")
    private boolean createSnapshot;

    public InitializeCommand(DemoApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        System.out.println("""
            ╔═══════════════════════════════════════════════════════════════╗
            ║           HDIM Demo Platform - Initialization                 ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);

        if (!apiClient.isServiceAvailable()) {
            System.err.println("ERROR: Demo seeding service is not available.");
            System.err.println();
            System.err.println("Please start the services first:");
            System.err.println("  docker compose -f docker-compose.demo.yml up -d");
            System.err.println();
            System.err.println("Or for development:");
            System.err.println("  cd backend && ./gradlew :modules:services:demo-seeding-service:bootRun");
            return 1;
        }

        try {
            // Step 1: Initialize
            System.out.println("Step 1/4: Initializing demo database...");
            Map<String, Object> initResult = apiClient.initialize();
            System.out.println("  ✓ Database initialized");

            if (initResult != null && initResult.containsKey("scenariosCreated")) {
                System.out.println("    Scenarios created: " + initResult.get("scenariosCreated"));
            }

            // Step 2: Load scenario
            System.out.println();
            System.out.println("Step 2/4: Loading scenario: " + scenario);
            Map<String, Object> scenarioResult = apiClient.loadScenario(scenario);
            System.out.println("  ✓ Scenario loaded");

            if (scenarioResult != null && scenarioResult.containsKey("scenarioName")) {
                System.out.println("    Name: " + scenarioResult.get("scenarioName"));
            }

            // Step 3: Generate patients
            if (!skipPatients) {
                System.out.println();
                System.out.println("Step 3/4: Generating " + patientCount + " synthetic patients...");
                System.out.println("  (This may take a few minutes)");

                long startTime = System.currentTimeMillis();
                Map<String, Object> patientsResult = apiClient.generatePatients(patientCount, "demo", "CARE_GAPS");
                long duration = System.currentTimeMillis() - startTime;

                System.out.println("  ✓ Patients generated");
                System.out.println("    Duration: " + duration + "ms");

                if (patientsResult != null && patientsResult.containsKey("patientsCreated")) {
                    System.out.println("    Patients: " + patientsResult.get("patientsCreated"));
                }
            } else {
                System.out.println();
                System.out.println("Step 3/4: Skipping patient generation (--skip-patients)");
            }

            // Step 4: Create snapshot
            if (createSnapshot) {
                System.out.println();
                System.out.println("Step 4/4: Creating baseline snapshot...");
                String snapshotName = "baseline-" + scenario;
                apiClient.createSnapshot(snapshotName, "Initial " + scenario + " baseline");
                System.out.println("  ✓ Snapshot created: " + snapshotName);
            } else {
                System.out.println();
                System.out.println("Step 4/4: Skipping snapshot (use --create-snapshot to enable)");
            }

            // Summary
            System.out.println();
            System.out.println("""
                ╔═══════════════════════════════════════════════════════════════╗
                ║              Initialization Complete!                         ║
                ╠═══════════════════════════════════════════════════════════════╣
                ║                                                               ║
                ║  Demo is ready for use!                                       ║
                ║                                                               ║
                ║  Access the demo UI:                                          ║
                ║    http://localhost:4200?demo=true                            ║
                ║                                                               ║
                ║  Login credentials:                                           ║
                ║    Email:    demo_evaluator@acmehealth.com                    ║
                ║    Password: Demo2026!                                        ║
                ║                                                               ║
                ╚═══════════════════════════════════════════════════════════════╝
                """);

            System.out.println("Quick commands:");
            System.out.println("  Check status:     demo-cli status");
            System.out.println("  Create snapshot:  demo-cli snapshot create before-recording");
            System.out.println("  Restore snapshot: demo-cli snapshot restore before-recording");
            System.out.println("  Reset all data:   demo-cli reset");

            return 0;

        } catch (Exception e) {
            System.err.println();
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}

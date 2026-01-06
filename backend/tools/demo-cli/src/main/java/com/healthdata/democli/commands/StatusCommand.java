package com.healthdata.democli.commands;

import com.healthdata.democli.DemoApiClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Check demo system status.
 */
@Component
@Command(
    name = "status",
    description = "Check demo system status",
    mixinStandardHelpOptions = true
)
public class StatusCommand implements Callable<Integer> {

    private final DemoApiClient apiClient;

    @Option(names = {"--verbose", "-v"}, description = "Show detailed status information")
    private boolean verbose;

    public StatusCommand(DemoApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        System.out.println(">>> HDIM Demo - System Status");
        System.out.println("════════════════════════════════════════════════════════════════════");

        // Check service availability
        System.out.println();
        System.out.println("Services:");
        System.out.println("─────────────────────────────────────────────────────────────────────");

        boolean demoServiceUp = apiClient.isServiceAvailable();
        System.out.printf("  Demo Seeding Service:  %s%n", demoServiceUp ? "✓ Running" : "✗ Not available");

        if (!demoServiceUp) {
            System.out.println();
            System.out.println("ERROR: Demo seeding service is not available.");
            System.out.println("Please start the service first:");
            System.out.println("  docker compose up -d demo-seeding-service");
            return 1;
        }

        try {
            Map<String, Object> status = apiClient.getStatus();

            if (status == null) {
                System.out.println("  Unable to retrieve status information");
                return 1;
            }

            // Database status
            if (status.containsKey("database")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> db = (Map<String, Object>) status.get("database");
                System.out.printf("  Database:              %s%n",
                    Boolean.TRUE.equals(db.get("connected")) ? "✓ Connected" : "✗ Disconnected");
            }

            // FHIR service status
            if (status.containsKey("fhirService")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fhir = (Map<String, Object>) status.get("fhirService");
                System.out.printf("  FHIR Service:          %s%n",
                    Boolean.TRUE.equals(fhir.get("connected")) ? "✓ Connected" : "✗ Disconnected");
            }

            // Demo Data Status
            System.out.println();
            System.out.println("Demo Data:");
            System.out.println("─────────────────────────────────────────────────────────────────────");

            if (status.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) status.get("data");
                System.out.println("  Patients:       " + data.getOrDefault("patientCount", 0));
                System.out.println("  Conditions:     " + data.getOrDefault("conditionCount", 0));
                System.out.println("  Observations:   " + data.getOrDefault("observationCount", 0));
                System.out.println("  Medications:    " + data.getOrDefault("medicationCount", 0));
                System.out.println("  Procedures:     " + data.getOrDefault("procedureCount", 0));
                System.out.println("  Encounters:     " + data.getOrDefault("encounterCount", 0));
                System.out.println("  Care Gaps:      " + data.getOrDefault("careGapCount", 0));
            }

            // Active Scenario
            System.out.println();
            System.out.println("Current Scenario:");
            System.out.println("─────────────────────────────────────────────────────────────────────");

            if (status.containsKey("activeScenario") && status.get("activeScenario") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> scenario = (Map<String, Object>) status.get("activeScenario");
                System.out.println("  ID:          " + scenario.getOrDefault("id", "N/A"));
                System.out.println("  Name:        " + scenario.getOrDefault("name", "N/A"));
                System.out.println("  Loaded at:   " + scenario.getOrDefault("loadedAt", "N/A"));
            } else {
                System.out.println("  No scenario loaded");
                System.out.println();
                System.out.println("  To load a scenario, run:");
                System.out.println("    demo-cli load-scenario hedis-evaluation");
            }

            // Snapshots
            if (verbose && status.containsKey("snapshots")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> snapshots = (Map<String, Object>) status.get("snapshots");
                System.out.println();
                System.out.println("Snapshots:");
                System.out.println("─────────────────────────────────────────────────────────────────────");
                System.out.println("  Available:   " + snapshots.getOrDefault("count", 0));
                System.out.println("  Storage:     " + snapshots.getOrDefault("storageUsed", "N/A"));
            }

            // Tenants
            if (verbose && status.containsKey("tenants")) {
                System.out.println();
                System.out.println("Tenants:");
                System.out.println("─────────────────────────────────────────────────────────────────────");
                @SuppressWarnings("unchecked")
                java.util.List<String> tenants = (java.util.List<String>) status.get("tenants");
                for (String tenant : tenants) {
                    System.out.println("  - " + tenant);
                }
            }

            System.out.println();
            System.out.println("════════════════════════════════════════════════════════════════════");
            System.out.println("Demo UI: http://localhost:4200?demo=true");

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}

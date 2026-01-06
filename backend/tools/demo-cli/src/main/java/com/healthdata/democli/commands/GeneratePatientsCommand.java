package com.healthdata.democli.commands;

import com.healthdata.democli.DemoApiClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Generate synthetic patients for demo purposes.
 */
@Component
@Command(
    name = "generate-patients",
    description = "Generate synthetic patients for demo",
    mixinStandardHelpOptions = true
)
public class GeneratePatientsCommand implements Callable<Integer> {

    private final DemoApiClient apiClient;

    @Option(names = {"-c", "--count"}, description = "Number of patients to generate (default: 1000)", defaultValue = "1000")
    private int count;

    @Option(names = {"-t", "--tenant"}, description = "Tenant ID (default: demo)", defaultValue = "demo")
    private String tenantId;

    @Option(names = {"--risk-profile"}, description = "Risk distribution profile: MIXED, LOW_RISK, HIGH_RISK, CARE_GAPS (default: MIXED)", defaultValue = "MIXED")
    private String riskProfile;

    @Option(names = {"--with-care-gaps"}, description = "Ensure patients have care gaps for demo measures")
    private boolean withCareGaps;

    public GeneratePatientsCommand(DemoApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        System.out.println(">>> HDIM Demo - Generate Synthetic Patients");
        System.out.println("────────────────────────────────────────");
        System.out.println("  Count:        " + count);
        System.out.println("  Tenant:       " + tenantId);
        System.out.println("  Risk Profile: " + riskProfile);
        if (withCareGaps) {
            System.out.println("  Care Gaps:    Yes");
        }
        System.out.println();

        if (!apiClient.isServiceAvailable()) {
            System.err.println("ERROR: Demo seeding service is not available.");
            System.err.println("Please ensure the service is running on localhost:8098");
            return 1;
        }

        if (count < 1 || count > 50000) {
            System.err.println("ERROR: Count must be between 1 and 50,000");
            return 1;
        }

        try {
            System.out.println("Generating patients...");
            System.out.println();

            // Progress indicator for large batches
            if (count > 1000) {
                System.out.println("This may take a few minutes for " + count + " patients...");
            }

            long startTime = System.currentTimeMillis();
            String profile = withCareGaps ? "CARE_GAPS" : riskProfile;
            Map<String, Object> result = apiClient.generatePatients(count, tenantId, profile);
            long duration = System.currentTimeMillis() - startTime;

            System.out.println();
            System.out.println("SUCCESS: Patients generated");
            System.out.println("────────────────────────────────────────");

            if (result != null) {
                if (result.containsKey("patientsCreated")) {
                    System.out.println("  Patients created: " + result.get("patientsCreated"));
                }
                if (result.containsKey("conditionsCreated")) {
                    System.out.println("  Conditions:       " + result.get("conditionsCreated"));
                }
                if (result.containsKey("observationsCreated")) {
                    System.out.println("  Observations:     " + result.get("observationsCreated"));
                }
                if (result.containsKey("medicationsCreated")) {
                    System.out.println("  Medications:      " + result.get("medicationsCreated"));
                }
                if (result.containsKey("proceduresCreated")) {
                    System.out.println("  Procedures:       " + result.get("proceduresCreated"));
                }
                if (result.containsKey("encountersCreated")) {
                    System.out.println("  Encounters:       " + result.get("encountersCreated"));
                }
                if (result.containsKey("careGapsCreated")) {
                    System.out.println("  Care gaps:        " + result.get("careGapsCreated"));
                }
            }

            System.out.println("  Duration:         " + duration + "ms");
            System.out.println("  Rate:             " + String.format("%.1f", count / (duration / 1000.0)) + " patients/sec");

            System.out.println();
            System.out.println("Risk Distribution:");
            if (result != null && result.containsKey("riskDistribution")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dist = (Map<String, Object>) result.get("riskDistribution");
                System.out.println("  Low risk:    " + dist.getOrDefault("lowRisk", "N/A"));
                System.out.println("  Medium risk: " + dist.getOrDefault("mediumRisk", "N/A"));
                System.out.println("  High risk:   " + dist.getOrDefault("highRisk", "N/A"));
            }

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}

package com.healthdata.democli.commands;

import com.healthdata.democli.DemoApiClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Load a specific demo scenario.
 */
@Component
@Command(
    name = "load-scenario",
    description = "Load a specific demo scenario",
    mixinStandardHelpOptions = true
)
public class LoadScenarioCommand implements Callable<Integer> {

    private final DemoApiClient apiClient;

    @Parameters(index = "0", description = "Scenario ID (e.g., hedis-evaluation, patient-journey)")
    private String scenarioId;

    @Option(names = {"--reset-first"}, description = "Reset existing data before loading scenario")
    private boolean resetFirst;

    @Option(names = {"--patients"}, description = "Number of patients to generate (default: use scenario default)", defaultValue = "-1")
    private int patientCount;

    public LoadScenarioCommand(DemoApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        System.out.println(">>> HDIM Demo - Load Scenario: " + scenarioId);
        System.out.println("────────────────────────────────────────");

        if (!apiClient.isServiceAvailable()) {
            System.err.println("ERROR: Demo seeding service is not available.");
            System.err.println("Please ensure the service is running on localhost:8098");
            return 1;
        }

        try {
            if (resetFirst) {
                System.out.println("Resetting existing data...");
                apiClient.reset();
                System.out.println("Done.");
                System.out.println();
            }

            System.out.println("Loading scenario: " + scenarioId);
            if (patientCount > 0) {
                System.out.println("  Patient count: " + patientCount);
            }

            Map<String, Object> result = apiClient.loadScenario(scenarioId);

            System.out.println();
            System.out.println("SUCCESS: Scenario loaded");
            System.out.println("────────────────────────────────────────");

            if (result != null) {
                if (result.containsKey("scenarioName")) {
                    System.out.println("  Scenario:      " + result.get("scenarioName"));
                }
                if (result.containsKey("description")) {
                    System.out.println("  Description:   " + result.get("description"));
                }
                if (result.containsKey("patientCount")) {
                    System.out.println("  Patients:      " + result.get("patientCount"));
                }
                if (result.containsKey("measureCount")) {
                    System.out.println("  Measures:      " + result.get("measureCount"));
                }
                if (result.containsKey("duration")) {
                    System.out.println("  Duration:      " + result.get("duration") + "ms");
                }
                if (result.containsKey("tenants")) {
                    System.out.println("  Tenants:       " + result.get("tenants"));
                }
            }

            System.out.println();
            System.out.println("Demo ready! Access at: http://localhost:4200?demo=true");

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}

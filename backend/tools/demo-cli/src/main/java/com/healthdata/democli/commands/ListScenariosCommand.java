package com.healthdata.democli.commands;

import com.healthdata.democli.DemoApiClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * List available demo scenarios.
 */
@Component
@Command(
    name = "list-scenarios",
    description = "List available demo scenarios",
    mixinStandardHelpOptions = true
)
public class ListScenariosCommand implements Callable<Integer> {

    private final DemoApiClient apiClient;

    @Option(names = {"--detailed", "-d"}, description = "Show detailed scenario information")
    private boolean detailed;

    public ListScenariosCommand(DemoApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        System.out.println(">>> HDIM Demo - Available Scenarios");
        System.out.println("════════════════════════════════════════════════════════════════════");

        if (!apiClient.isServiceAvailable()) {
            System.err.println("ERROR: Demo seeding service is not available.");
            System.err.println("Please ensure the service is running on localhost:8098");
            return 1;
        }

        try {
            List<Map<String, Object>> scenarios = apiClient.listScenarios();

            if (scenarios == null || scenarios.isEmpty()) {
                System.out.println("No scenarios available.");
                System.out.println();
                System.out.println("Hint: Initialize the demo environment first:");
                System.out.println("  demo-cli initialize");
                return 0;
            }

            // Get current scenario
            Map<String, Object> current = null;
            try {
                current = apiClient.getCurrentScenario();
            } catch (Exception ignored) {
            }
            String currentId = current != null ? (String) current.get("id") : null;

            for (Map<String, Object> scenario : scenarios) {
                String id = (String) scenario.get("id");
                String name = (String) scenario.get("name");
                String description = (String) scenario.get("description");
                boolean isActive = id != null && id.equals(currentId);

                String indicator = isActive ? " [ACTIVE]" : "";
                System.out.println();
                System.out.printf("  %-25s %s%n", id, indicator);
                System.out.println("  ─────────────────────────────────────────────────────────────");
                System.out.println("  Name:        " + name);

                if (detailed) {
                    System.out.println("  Description: " + (description != null ? description : "N/A"));

                    if (scenario.containsKey("defaultPatientCount")) {
                        System.out.println("  Patients:    " + scenario.get("defaultPatientCount"));
                    }
                    if (scenario.containsKey("duration")) {
                        System.out.println("  Duration:    " + scenario.get("duration") + " minutes");
                    }
                    if (scenario.containsKey("valueProp")) {
                        System.out.println("  Value Prop:  " + scenario.get("valueProp"));
                    }
                    if (scenario.containsKey("tenants")) {
                        System.out.println("  Tenants:     " + scenario.get("tenants"));
                    }
                }
            }

            System.out.println();
            System.out.println("════════════════════════════════════════════════════════════════════");
            System.out.println("Total: " + scenarios.size() + " scenarios");
            System.out.println();
            System.out.println("Usage: demo-cli load-scenario <scenario-id>");
            if (!detailed) {
                System.out.println("       Use -d flag for detailed information");
            }

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}

package com.healthdata.democli.commands;

import com.healthdata.democli.DemoApiClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Reset all demo data to a clean state.
 */
@Component
@Command(
    name = "reset",
    description = "Reset all demo data to initial state",
    mixinStandardHelpOptions = true
)
public class ResetCommand implements Callable<Integer> {

    private final DemoApiClient apiClient;

    @Option(names = {"-f", "--force"}, description = "Skip confirmation prompt")
    private boolean force;

    @Option(names = {"--preserve-snapshots"}, description = "Preserve existing snapshots")
    private boolean preserveSnapshots;

    public ResetCommand(DemoApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        System.out.println(">>> HDIM Demo Reset");
        System.out.println("────────────────────────────────────────");

        if (!apiClient.isServiceAvailable()) {
            System.err.println("ERROR: Demo seeding service is not available.");
            System.err.println("Please ensure the service is running on localhost:8098");
            return 1;
        }

        if (!force) {
            System.out.println("WARNING: This will delete all demo data!");
            if (preserveSnapshots) {
                System.out.println("         (Snapshots will be preserved)");
            }
            System.out.println();
            System.out.print("Are you sure you want to continue? [y/N]: ");
            try {
                int response = System.in.read();
                if (response != 'y' && response != 'Y') {
                    System.out.println("Aborted.");
                    return 0;
                }
            } catch (Exception e) {
                System.out.println("Aborted.");
                return 0;
            }
        }

        try {
            System.out.println();
            System.out.println("Resetting demo data...");

            Map<String, Object> result = apiClient.reset();

            System.out.println();
            System.out.println("SUCCESS: Demo data has been reset");
            System.out.println("────────────────────────────────────────");

            if (result != null) {
                if (result.containsKey("patientsDeleted")) {
                    System.out.println("  Patients deleted:  " + result.get("patientsDeleted"));
                }
                if (result.containsKey("scenariosReset")) {
                    System.out.println("  Scenarios reset:   " + result.get("scenariosReset"));
                }
                if (result.containsKey("sessionsCleared")) {
                    System.out.println("  Sessions cleared:  " + result.get("sessionsCleared"));
                }
                if (result.containsKey("duration")) {
                    System.out.println("  Duration:          " + result.get("duration") + "ms");
                }
            }

            System.out.println();
            System.out.println("Next steps:");
            System.out.println("  1. Load a scenario:  demo-cli load-scenario hedis-evaluation");
            System.out.println("  2. Generate patients: demo-cli generate-patients --count 5000");

            return 0;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            return 1;
        }
    }
}

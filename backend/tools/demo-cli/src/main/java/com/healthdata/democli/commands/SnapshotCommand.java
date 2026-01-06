package com.healthdata.democli.commands;

import com.healthdata.democli.DemoApiClient;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Manage database snapshots for demo recording.
 */
@Component
@Command(
    name = "snapshot",
    description = "Manage database snapshots",
    mixinStandardHelpOptions = true,
    subcommands = {
        SnapshotCommand.CreateSnapshot.class,
        SnapshotCommand.ListSnapshots.class,
        SnapshotCommand.RestoreSnapshot.class,
        SnapshotCommand.DeleteSnapshot.class
    }
)
public class SnapshotCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("""
            Snapshot Commands:
              create   - Create a new snapshot
              list     - List available snapshots
              restore  - Restore from a snapshot
              delete   - Delete a snapshot

            Usage: demo-cli snapshot <command> [options]
            """);
        return 0;
    }

    @Component
    @Command(name = "create", description = "Create a database snapshot")
    public static class CreateSnapshot implements Callable<Integer> {

        private final DemoApiClient apiClient;

        @Parameters(index = "0", description = "Snapshot name")
        private String name;

        @Option(names = {"-d", "--description"}, description = "Snapshot description")
        private String description;

        public CreateSnapshot(DemoApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            System.out.println(">>> HDIM Demo - Create Snapshot");
            System.out.println("────────────────────────────────────────");

            if (!apiClient.isServiceAvailable()) {
                System.err.println("ERROR: Demo seeding service is not available.");
                return 1;
            }

            try {
                System.out.println("Creating snapshot: " + name);
                if (description != null) {
                    System.out.println("Description: " + description);
                }
                System.out.println();

                Map<String, Object> result = apiClient.createSnapshot(name, description);

                System.out.println("SUCCESS: Snapshot created");
                System.out.println("────────────────────────────────────────");

                if (result != null) {
                    System.out.println("  ID:          " + result.getOrDefault("id", name));
                    System.out.println("  Name:        " + result.getOrDefault("name", name));
                    System.out.println("  Size:        " + result.getOrDefault("size", "N/A"));
                    System.out.println("  Created at:  " + result.getOrDefault("createdAt", "N/A"));
                }

                System.out.println();
                System.out.println("To restore this snapshot:");
                System.out.println("  demo-cli snapshot restore " + name);

                return 0;

            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "list", description = "List available snapshots")
    public static class ListSnapshots implements Callable<Integer> {

        private final DemoApiClient apiClient;

        public ListSnapshots(DemoApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            System.out.println(">>> HDIM Demo - Available Snapshots");
            System.out.println("════════════════════════════════════════════════════════════════════");

            if (!apiClient.isServiceAvailable()) {
                System.err.println("ERROR: Demo seeding service is not available.");
                return 1;
            }

            try {
                List<Map<String, Object>> snapshots = apiClient.listSnapshots();

                if (snapshots == null || snapshots.isEmpty()) {
                    System.out.println();
                    System.out.println("No snapshots available.");
                    System.out.println();
                    System.out.println("Create one with:");
                    System.out.println("  demo-cli snapshot create <name>");
                    return 0;
                }

                System.out.println();
                System.out.printf("  %-20s %-30s %-15s %-20s%n", "ID", "Name", "Size", "Created");
                System.out.println("  ──────────────────────────────────────────────────────────────────");

                for (Map<String, Object> snapshot : snapshots) {
                    System.out.printf("  %-20s %-30s %-15s %-20s%n",
                            snapshot.getOrDefault("id", "N/A"),
                            snapshot.getOrDefault("name", "N/A"),
                            snapshot.getOrDefault("size", "N/A"),
                            snapshot.getOrDefault("createdAt", "N/A"));
                }

                System.out.println();
                System.out.println("Total: " + snapshots.size() + " snapshot(s)");
                System.out.println();
                System.out.println("Usage:");
                System.out.println("  Restore: demo-cli snapshot restore <name>");
                System.out.println("  Delete:  demo-cli snapshot delete <name>");

                return 0;

            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "restore", description = "Restore from a snapshot")
    public static class RestoreSnapshot implements Callable<Integer> {

        private final DemoApiClient apiClient;

        @Parameters(index = "0", description = "Snapshot name or ID")
        private String snapshotId;

        @Option(names = {"-f", "--force"}, description = "Skip confirmation")
        private boolean force;

        public RestoreSnapshot(DemoApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            System.out.println(">>> HDIM Demo - Restore Snapshot");
            System.out.println("────────────────────────────────────────");

            if (!apiClient.isServiceAvailable()) {
                System.err.println("ERROR: Demo seeding service is not available.");
                return 1;
            }

            if (!force) {
                System.out.println("WARNING: This will replace all current demo data!");
                System.out.print("Are you sure you want to restore '" + snapshotId + "'? [y/N]: ");
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
                System.out.println("Restoring snapshot: " + snapshotId);

                long startTime = System.currentTimeMillis();
                Map<String, Object> result = apiClient.restoreSnapshot(snapshotId);
                long duration = System.currentTimeMillis() - startTime;

                System.out.println();
                System.out.println("SUCCESS: Snapshot restored");
                System.out.println("────────────────────────────────────────");
                System.out.println("  Duration:    " + duration + "ms");

                if (result != null) {
                    if (result.containsKey("patientsRestored")) {
                        System.out.println("  Patients:    " + result.get("patientsRestored"));
                    }
                }

                System.out.println();
                System.out.println("Demo is ready for recording!");

                return 0;

            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "delete", description = "Delete a snapshot")
    public static class DeleteSnapshot implements Callable<Integer> {

        private final DemoApiClient apiClient;

        @Parameters(index = "0", description = "Snapshot name or ID")
        private String snapshotId;

        @Option(names = {"-f", "--force"}, description = "Skip confirmation")
        private boolean force;

        public DeleteSnapshot(DemoApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            System.out.println(">>> HDIM Demo - Delete Snapshot");
            System.out.println("────────────────────────────────────────");

            if (!apiClient.isServiceAvailable()) {
                System.err.println("ERROR: Demo seeding service is not available.");
                return 1;
            }

            if (!force) {
                System.out.print("Are you sure you want to delete '" + snapshotId + "'? [y/N]: ");
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
                apiClient.deleteSnapshot(snapshotId);
                System.out.println();
                System.out.println("SUCCESS: Snapshot '" + snapshotId + "' deleted");

                return 0;

            } catch (Exception e) {
                System.err.println("ERROR: " + e.getMessage());
                return 1;
            }
        }
    }
}

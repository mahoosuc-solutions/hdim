package com.healthdata.migration.cli.command;

import java.util.UUID;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import com.healthdata.migration.cli.client.MigrationApiClient;
import com.healthdata.migration.cli.output.TableRenderer;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.MigrationJobResponse;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Job management commands
 */
@Component
@Command(
    name = "job",
    description = "Manage migration jobs",
    subcommands = {
        JobCommand.Create.class,
        JobCommand.List.class,
        JobCommand.Status.class,
        JobCommand.Start.class,
        JobCommand.Pause.class,
        JobCommand.Resume.class,
        JobCommand.Cancel.class,
        JobCommand.Delete.class
    }
)
public class JobCommand implements Runnable {

    @ParentCommand
    private com.healthdata.migration.cli.MigrationCommand parent;

    @Override
    public void run() {
        System.out.println("Use 'job --help' for available subcommands");
    }

    @Component
    @Command(name = "create", description = "Create a new migration job")
    public static class Create implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;

        @Option(names = {"--name", "-n"}, required = true, description = "Job name")
        String name;

        @Option(names = {"--source", "-s"}, required = true, description = "Source type: FILE, SFTP, MLLP")
        SourceType sourceType;

        @Option(names = {"--path", "-p"}, description = "Path or pattern for files")
        String path;

        @Option(names = {"--type", "-t"}, required = true, description = "Data type: HL7V2, CDA, FHIR_BUNDLE")
        DataType dataType;

        @Option(names = {"--host"}, description = "SFTP host")
        String host;

        @Option(names = {"--port"}, description = "Port number")
        Integer port;

        @Option(names = {"--user"}, description = "Username for SFTP")
        String username;

        @Option(names = {"--password"}, description = "Password for SFTP")
        String password;

        @Option(names = {"--batch-size"}, defaultValue = "100", description = "Batch size")
        int batchSize;

        @Option(names = {"--no-fhir"}, description = "Skip FHIR conversion")
        boolean noFhir;

        @Option(names = {"--stop-on-error"}, description = "Stop on first error")
        boolean stopOnError;

        public Create(MigrationApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            try {
                SourceConfig sourceConfig = buildSourceConfig();

                MigrationJobRequest request = MigrationJobRequest.builder()
                        .jobName(name)
                        .sourceType(sourceType)
                        .sourceConfig(sourceConfig)
                        .dataType(dataType)
                        .batchSize(batchSize)
                        .convertToFhir(!noFhir)
                        .continueOnError(!stopOnError)
                        .build();

                MigrationJobResponse job = apiClient.createJob(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        request
                );

                System.out.println("Created migration job:");
                System.out.println("  ID: " + job.getId());
                System.out.println("  Name: " + job.getJobName());
                System.out.println("  Status: " + job.getStatus());
                System.out.println();
                System.out.println("To start the job, run:");
                System.out.println("  hdim-migrate job start " + job.getId());

                return 0;
            } catch (Exception e) {
                System.err.println("Error creating job: " + e.getMessage());
                return 1;
            }
        }

        private SourceConfig buildSourceConfig() {
            return switch (sourceType) {
                case FILE -> SourceConfig.forFile(path, "*", true);
                case SFTP -> SourceConfig.forSftpPassword(
                        host, port != null ? port : 22, username, password, path);
                case MLLP -> SourceConfig.forMllp(port != null ? port : 2575, false);
            };
        }
    }

    @Component
    @Command(name = "list", description = "List migration jobs")
    public static class List implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;
        private final TableRenderer tableRenderer;

        @Option(names = {"--status"}, description = "Filter by status")
        JobStatus status;

        @Option(names = {"--limit"}, defaultValue = "20", description = "Number of jobs to show")
        int limit;

        public List(MigrationApiClient apiClient, TableRenderer tableRenderer) {
            this.apiClient = apiClient;
            this.tableRenderer = tableRenderer;
        }

        @Override
        public Integer call() {
            try {
                var jobs = apiClient.listJobs(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        status,
                        limit
                );

                if (jobs.isEmpty()) {
                    System.out.println("No jobs found");
                    return 0;
                }

                tableRenderer.renderJobList(jobs);
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing jobs: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "status", description = "Get job status")
    public static class Status implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;

        @Parameters(index = "0", description = "Job ID")
        UUID jobId;

        public Status(MigrationApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            try {
                MigrationJobResponse job = apiClient.getJob(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        jobId
                );

                System.out.println("Job: " + job.getJobName());
                System.out.println("ID: " + job.getId());
                System.out.println("Status: " + job.getStatus());
                System.out.println();
                System.out.println("Progress:");
                System.out.printf("  Total:     %d%n", job.getTotalRecords());
                System.out.printf("  Processed: %d%n", job.getProcessedCount());
                System.out.printf("  Success:   %d%n", job.getSuccessCount());
                System.out.printf("  Failed:    %d%n", job.getFailureCount());
                System.out.printf("  Skipped:   %d%n", job.getSkippedCount());
                System.out.println();
                System.out.printf("Completion: %.1f%%%n", job.getCompletionPercentage());
                System.out.printf("Success Rate: %.1f%%%n", job.getSuccessRate());

                return 0;
            } catch (Exception e) {
                System.err.println("Error getting job status: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "start", description = "Start a migration job")
    public static class Start implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;

        @Parameters(index = "0", description = "Job ID")
        UUID jobId;

        public Start(MigrationApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            try {
                MigrationJobResponse job = apiClient.startJob(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        jobId
                );

                System.out.println("Started job: " + job.getJobName());
                System.out.println("Status: " + job.getStatus());
                System.out.println();
                System.out.println("To watch progress, run:");
                System.out.println("  hdim-migrate watch " + jobId);

                return 0;
            } catch (Exception e) {
                System.err.println("Error starting job: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "pause", description = "Pause a running job")
    public static class Pause implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;

        @Parameters(index = "0", description = "Job ID")
        UUID jobId;

        public Pause(MigrationApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            try {
                MigrationJobResponse job = apiClient.pauseJob(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        jobId
                );

                System.out.println("Paused job: " + job.getJobName());
                System.out.println("Status: " + job.getStatus());
                System.out.println("Processed: " + job.getProcessedCount() + " records");

                return 0;
            } catch (Exception e) {
                System.err.println("Error pausing job: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "resume", description = "Resume a paused job")
    public static class Resume implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;

        @Parameters(index = "0", description = "Job ID")
        UUID jobId;

        public Resume(MigrationApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            try {
                MigrationJobResponse job = apiClient.resumeJob(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        jobId
                );

                System.out.println("Resumed job: " + job.getJobName());
                System.out.println("Status: " + job.getStatus());

                return 0;
            } catch (Exception e) {
                System.err.println("Error resuming job: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "cancel", description = "Cancel a job")
    public static class Cancel implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;

        @Parameters(index = "0", description = "Job ID")
        UUID jobId;

        public Cancel(MigrationApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            try {
                MigrationJobResponse job = apiClient.cancelJob(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        jobId
                );

                System.out.println("Cancelled job: " + job.getJobName());
                System.out.println("Final status: " + job.getStatus());
                System.out.println("Processed: " + job.getProcessedCount() + " records");

                return 0;
            } catch (Exception e) {
                System.err.println("Error cancelling job: " + e.getMessage());
                return 1;
            }
        }
    }

    @Component
    @Command(name = "delete", description = "Delete a job")
    public static class Delete implements Callable<Integer> {

        @ParentCommand
        private JobCommand parent;

        private final MigrationApiClient apiClient;

        @Parameters(index = "0", description = "Job ID")
        UUID jobId;

        @Option(names = {"--force", "-f"}, description = "Force delete without confirmation")
        boolean force;

        public Delete(MigrationApiClient apiClient) {
            this.apiClient = apiClient;
        }

        @Override
        public Integer call() {
            try {
                if (!force) {
                    System.out.print("Are you sure you want to delete job " + jobId + "? [y/N] ");
                    int response = System.in.read();
                    if (response != 'y' && response != 'Y') {
                        System.out.println("Cancelled");
                        return 0;
                    }
                }

                apiClient.deleteJob(
                        parent.parent.apiUrl,
                        parent.parent.tenantId,
                        jobId
                );

                System.out.println("Deleted job: " + jobId);
                return 0;
            } catch (Exception e) {
                System.err.println("Error deleting job: " + e.getMessage());
                return 1;
            }
        }
    }
}

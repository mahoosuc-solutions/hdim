package com.healthdata.migration.cli.command;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import com.healthdata.migration.cli.client.MigrationApiClient;
import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.MigrationJobResponse;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Run a migration directly without creating a persistent job
 */
@Component
@Command(name = "run", description = "Run a migration directly")
public class RunCommand implements Callable<Integer> {

    @ParentCommand
    private com.healthdata.migration.cli.MigrationCommand parent;

    private final MigrationApiClient apiClient;

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

    @Option(names = {"--fhir"}, description = "Convert to FHIR")
    boolean convertToFhir = true;

    @Option(names = {"--batch-size"}, defaultValue = "100", description = "Batch size")
    int batchSize;

    public RunCommand(MigrationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        try {
            System.out.println("Starting direct migration...");
            System.out.println("Source: " + sourceType);
            System.out.println("Data Type: " + dataType);
            System.out.println();

            SourceConfig sourceConfig = buildSourceConfig();

            String jobName = "Direct-" + System.currentTimeMillis();
            MigrationJobRequest request = MigrationJobRequest.builder()
                    .jobName(jobName)
                    .sourceType(sourceType)
                    .sourceConfig(sourceConfig)
                    .dataType(dataType)
                    .batchSize(batchSize)
                    .convertToFhir(convertToFhir)
                    .continueOnError(true)
                    .build();

            // Create and start job
            MigrationJobResponse job = apiClient.createJob(
                    parent.apiUrl,
                    parent.tenantId,
                    request
            );

            System.out.println("Created job: " + job.getId());

            job = apiClient.startJob(parent.apiUrl, parent.tenantId, job.getId());
            System.out.println("Started job, status: " + job.getStatus());
            System.out.println();
            System.out.println("To watch progress, run:");
            System.out.println("  hdim-migrate watch " + job.getId());

            return 0;
        } catch (Exception e) {
            System.err.println("Error running migration: " + e.getMessage());
            if (parent.verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private SourceConfig buildSourceConfig() {
        SourceConfig config = switch (sourceType) {
            case FILE -> SourceConfig.forFile(path, "*", true);
            case SFTP -> SourceConfig.forSftpPassword(
                    host, port != null ? port : 22, username, password, path);
            case MLLP -> SourceConfig.forMllp(port != null ? port : 2575, false);
        };
        config.setDataType(dataType);
        return config;
    }
}

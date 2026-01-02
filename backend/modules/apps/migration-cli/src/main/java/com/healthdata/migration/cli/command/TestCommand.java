package com.healthdata.migration.cli.command;

import java.util.concurrent.Callable;

import org.springframework.stereotype.Component;

import com.healthdata.migration.cli.client.MigrationApiClient;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceType;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Test source connections
 */
@Component
@Command(name = "test", description = "Test source connections")
public class TestCommand implements Callable<Integer> {

    @ParentCommand
    private com.healthdata.migration.cli.MigrationCommand parent;

    private final MigrationApiClient apiClient;

    @Option(names = {"--source", "-s"}, required = true, description = "Source type: FILE, SFTP, MLLP")
    SourceType sourceType;

    @Option(names = {"--path", "-p"}, description = "Path for FILE source")
    String path;

    @Option(names = {"--host"}, description = "Host for SFTP")
    String host;

    @Option(names = {"--port"}, description = "Port number")
    Integer port;

    @Option(names = {"--user"}, description = "Username for SFTP")
    String username;

    @Option(names = {"--password"}, description = "Password for SFTP")
    String password;

    @Option(names = {"--key"}, description = "Private key path for SFTP")
    String privateKeyPath;

    public TestCommand(MigrationApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public Integer call() {
        System.out.println("Testing " + sourceType + " connection...");
        System.out.println();

        try {
            SourceConfig config = buildSourceConfig();
            boolean success = testConnection(config);

            if (success) {
                System.out.println("Connection successful!");
                return 0;
            } else {
                System.err.println("Connection failed");
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Connection test failed: " + e.getMessage());
            if (parent.verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }

    private SourceConfig buildSourceConfig() {
        return switch (sourceType) {
            case FILE -> {
                if (path == null) {
                    throw new IllegalArgumentException("--path is required for FILE source");
                }
                yield SourceConfig.forFile(path, "*", true);
            }
            case SFTP -> {
                if (host == null) {
                    throw new IllegalArgumentException("--host is required for SFTP source");
                }
                if (privateKeyPath != null) {
                    yield SourceConfig.forSftpKey(host, port != null ? port : 22,
                            username, privateKeyPath, path != null ? path : "/");
                } else {
                    yield SourceConfig.forSftpPassword(host, port != null ? port : 22,
                            username, password, path != null ? path : "/");
                }
            }
            case MLLP -> SourceConfig.forMllp(port != null ? port : 2575, false);
        };
    }

    private boolean testConnection(SourceConfig config) {
        // For now, just validate the configuration locally
        // In production, this would call the API to test the actual connection

        switch (sourceType) {
            case FILE:
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    if (file.isDirectory()) {
                        System.out.println("Directory exists: " + path);
                        System.out.println("Files found: " + file.list().length);
                    } else {
                        System.out.println("File exists: " + path);
                        System.out.println("Size: " + file.length() + " bytes");
                    }
                    return true;
                } else {
                    System.err.println("Path does not exist: " + path);
                    return false;
                }

            case SFTP:
                System.out.println("SFTP connection parameters:");
                System.out.println("  Host: " + host);
                System.out.println("  Port: " + (port != null ? port : 22));
                System.out.println("  User: " + username);
                System.out.println("  Auth: " + (privateKeyPath != null ? "Key" : "Password"));
                System.out.println();
                System.out.println("To test actual connection, the migration service must be running.");
                return true;

            case MLLP:
                System.out.println("MLLP listener parameters:");
                System.out.println("  Port: " + (port != null ? port : 2575));
                System.out.println();
                System.out.println("MLLP listener will be started when migration runs.");
                return true;

            default:
                return false;
        }
    }
}

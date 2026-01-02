package com.healthdata.migration.dto;

import java.util.Map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for migration data sources.
 * Supports FILE, SFTP, and MLLP source types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceConfig {

    @NotNull
    private SourceType sourceType;

    // Data type for the source
    private DataType dataType;

    // Common settings
    private String path;
    private String filePattern;
    private String encoding;
    private boolean recursive;

    // Compression
    private CompressionType compression;

    // SFTP settings
    private String host;
    private Integer port;
    private String username;
    private AuthType authType;
    private String password;
    private String privateKeyPath;
    private String remotePath;
    private String localCacheDir;

    // MLLP settings
    private String bindAddress;
    private Integer maxConnections;
    private Integer bufferSize;
    private boolean sendAck;
    private boolean tlsEnabled;
    private String tlsCertPath;
    private String tlsKeyPath;

    // Additional properties for extensibility
    private Map<String, Object> additionalProperties;

    public enum CompressionType {
        NONE, GZIP
    }

    public enum AuthType {
        PASSWORD, KEY
    }

    /**
     * Create a file source configuration
     */
    public static SourceConfig forFile(String path, String pattern, boolean recursive) {
        return SourceConfig.builder()
                .sourceType(SourceType.FILE)
                .path(path)
                .filePattern(pattern)
                .recursive(recursive)
                .encoding("UTF-8")
                .compression(CompressionType.NONE)
                .build();
    }

    /**
     * Create an SFTP source configuration with password auth
     */
    public static SourceConfig forSftpPassword(String host, int port, String username,
                                                String password, String remotePath) {
        return SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host(host)
                .port(port)
                .username(username)
                .password(password)
                .authType(AuthType.PASSWORD)
                .remotePath(remotePath)
                .build();
    }

    /**
     * Create an SFTP source configuration with key auth
     */
    public static SourceConfig forSftpKey(String host, int port, String username,
                                          String privateKeyPath, String remotePath) {
        return SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host(host)
                .port(port)
                .username(username)
                .privateKeyPath(privateKeyPath)
                .authType(AuthType.KEY)
                .remotePath(remotePath)
                .build();
    }

    /**
     * Create an MLLP listener configuration
     */
    public static SourceConfig forMllp(int port, boolean tlsEnabled) {
        return SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .bindAddress("0.0.0.0")
                .port(port)
                .maxConnections(10)
                .bufferSize(1000)
                .sendAck(true)
                .tlsEnabled(tlsEnabled)
                .build();
    }
}

package com.healthdata.migration.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Wrapper for an individual record read from a source
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceRecord {

    // Unique identifier for this record
    private String recordId;

    // Source file/location info
    private String sourceFile;
    private long offset;
    private int recordNumber;

    // Raw content
    private String content;
    private DataType dataType;

    // Metadata
    private long sizeBytes;
    private Instant readAt;

    /**
     * Create a record from raw content
     */
    public static SourceRecord of(String content, DataType dataType, String sourceFile, long offset) {
        return SourceRecord.builder()
                .recordId(String.format("%s:%d", sourceFile, offset))
                .content(content)
                .dataType(dataType)
                .sourceFile(sourceFile)
                .offset(offset)
                .sizeBytes(content != null ? content.length() : 0)
                .readAt(Instant.now())
                .build();
    }
}

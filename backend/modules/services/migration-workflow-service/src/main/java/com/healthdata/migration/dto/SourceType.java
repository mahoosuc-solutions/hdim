package com.healthdata.migration.dto;

/**
 * Types of data sources for migration
 */
public enum SourceType {
    FILE,   // Local filesystem or NFS
    SFTP,   // SFTP/FTPS remote server
    MLLP    // HL7 MLLP listener
}

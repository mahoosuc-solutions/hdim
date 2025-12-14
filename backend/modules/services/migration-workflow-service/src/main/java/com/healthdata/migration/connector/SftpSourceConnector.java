package com.healthdata.migration.connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * Source connector for reading files from SFTP servers.
 * Supports password and key-based authentication.
 */
public class SftpSourceConnector extends AbstractSourceConnector {

    private JSch jsch;
    private Session session;
    private ChannelSftp sftpChannel;
    private List<String> remoteFiles;
    private int currentFileIndex = 0;
    private long recordsRead = 0;

    @Override
    public SourceType getType() {
        return SourceType.SFTP;
    }

    @Override
    protected void doConnect() throws IOException {
        try {
            jsch = new JSch();

            // Configure authentication
            if (config.getAuthType() == SourceConfig.AuthType.KEY && config.getPrivateKeyPath() != null) {
                jsch.addIdentity(config.getPrivateKeyPath());
            }

            // Create session
            int port = config.getPort() != null ? config.getPort() : 22;
            session = jsch.getSession(config.getUsername(), config.getHost(), port);

            if (config.getAuthType() == SourceConfig.AuthType.PASSWORD && config.getPassword() != null) {
                session.setPassword(config.getPassword());
            }

            // Skip host key verification (in production, use known_hosts)
            session.setConfig("StrictHostKeyChecking", "no");

            // Connect with timeout
            session.connect(30000);

            // Open SFTP channel
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect(30000);

            // List files in remote path
            collectRemoteFiles();

            log.info("Connected to SFTP {}:{}, found {} files",
                    config.getHost(), port, remoteFiles.size());

        } catch (JSchException | SftpException e) {
            throw new IOException("Failed to connect to SFTP: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private void collectRemoteFiles() throws SftpException {
        remoteFiles = new ArrayList<>();
        String remotePath = config.getRemotePath() != null ? config.getRemotePath() : "/";
        String pattern = config.getFilePattern() != null ? config.getFilePattern() : "*";

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);

        Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(remotePath);
        for (ChannelSftp.LsEntry entry : entries) {
            if (!entry.getAttrs().isDir()) {
                String fileName = entry.getFilename();
                if (matcher.matches(Paths.get(fileName))) {
                    String fullPath = remotePath.endsWith("/") ?
                            remotePath + fileName : remotePath + "/" + fileName;
                    remoteFiles.add(fullPath);
                }
            }
        }

        Collections.sort(remoteFiles);
    }

    @Override
    protected void doDisconnect() {
        if (sftpChannel != null) {
            sftpChannel.disconnect();
        }
        if (session != null) {
            session.disconnect();
        }
    }

    @Override
    public boolean testConnection() {
        return session != null && session.isConnected() &&
               sftpChannel != null && sftpChannel.isConnected();
    }

    @Override
    public long countRecords() throws IOException {
        ensureConnected();
        return remoteFiles.size(); // For SFTP, we count files
    }

    @Override
    public Iterator<SourceRecord> readRecords(int batchSize) throws IOException {
        ensureConnected();
        return new SftpRecordIterator(batchSize);
    }

    @Override
    public void seek(long offset) throws IOException {
        ensureConnected();
        // For SFTP, offset represents file index
        currentFileIndex = (int) Math.min(offset, remoteFiles.size());
        recordsRead = currentFileIndex;
        currentPosition = recordsRead;
    }

    @Override
    protected void addCheckpointData(Map<String, Object> checkpoint) {
        checkpoint.put("fileIndex", currentFileIndex);
        checkpoint.put("recordsRead", recordsRead);
    }

    @Override
    protected void doRestoreFromCheckpoint(Map<String, Object> checkpointData) throws IOException {
        if (checkpointData.containsKey("fileIndex")) {
            currentFileIndex = ((Number) checkpointData.get("fileIndex")).intValue();
        }
        if (checkpointData.containsKey("recordsRead")) {
            recordsRead = ((Number) checkpointData.get("recordsRead")).longValue();
            currentPosition = recordsRead;
        }
    }

    /**
     * Iterator that reads records from SFTP files
     */
    private class SftpRecordIterator implements Iterator<SourceRecord> {
        private final int batchSize;
        private SourceRecord nextRecord;

        SftpRecordIterator(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public boolean hasNext() {
            if (nextRecord != null) {
                return true;
            }
            try {
                nextRecord = readNextFile();
                return nextRecord != null;
            } catch (Exception e) {
                throw new RuntimeException("Error reading next record", e);
            }
        }

        @Override
        public SourceRecord next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            SourceRecord record = nextRecord;
            nextRecord = null;
            recordsRead++;
            currentPosition = recordsRead;
            return record;
        }

        private SourceRecord readNextFile() throws SftpException, IOException {
            while (currentFileIndex < remoteFiles.size()) {
                String remotePath = remoteFiles.get(currentFileIndex);
                currentFile = remotePath;
                currentFileIndex++;

                try (InputStream is = sftpChannel.get(remotePath)) {
                    String content = readContent(is);
                    DataType dataType = detectDataType(remotePath);
                    return SourceRecord.of(content, dataType, remotePath, recordsRead);
                }
            }
            return null;
        }

        private String readContent(InputStream is) throws IOException {
            String encoding = config.getEncoding() != null ? config.getEncoding() : "UTF-8";
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, Charset.forName(encoding)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        }

        private DataType detectDataType(String path) {
            if (config.getDataType() != null) {
                return config.getDataType();
            }

            String fileName = path.toLowerCase();
            if (fileName.endsWith(".hl7")) {
                return DataType.HL7V2;
            } else if (fileName.endsWith(".xml")) {
                return DataType.CDA;
            } else if (fileName.endsWith(".json")) {
                return DataType.FHIR_BUNDLE;
            }
            return DataType.HL7V2;
        }
    }
}

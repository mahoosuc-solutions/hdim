package com.healthdata.migration.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;

@ExtendWith(MockitoExtension.class)
@DisplayName("SftpSourceConnector")
class SftpSourceConnectorTest {

    @Mock
    private JSch jsch;

    @Mock
    private Session session;

    @Mock
    private ChannelSftp sftpChannel;

    private TestSftpSourceConnector connector;

    @BeforeEach
    void setUp() throws Exception {
        connector = new TestSftpSourceConnector(jsch);
        when(jsch.getSession(eq("user"), eq("sftp.example"), eq(2222))).thenReturn(session);
        when(session.openChannel(eq("sftp"))).thenReturn(sftpChannel);
        doNothing().when(session).connect(anyInt());
        doNothing().when(sftpChannel).connect(anyInt());
        lenient().when(session.isConnected()).thenReturn(true);
        lenient().when(sftpChannel.isConnected()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        connector.disconnect();
    }

    @Test
    @DisplayName("Should connect and read records from SFTP")
    void shouldConnectAndReadRecords() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .password("secret")
                .authType(SourceConfig.AuthType.PASSWORD)
                .remotePath("/inbound")
                .filePattern("*")
                .build();

        setupSftpListing("/inbound", "a.hl7", "b.xml");
        when(sftpChannel.get(eq("/inbound/a.hl7")))
                .thenReturn(new ByteArrayInputStream("MSH|^~\\&|TEST".getBytes(StandardCharsets.UTF_8)));
        when(sftpChannel.get(eq("/inbound/b.xml")))
                .thenReturn(new ByteArrayInputStream("<ClinicalDocument/>".getBytes(StandardCharsets.UTF_8)));

        connector.connect(config);

        assertThat(connector.testConnection()).isTrue();
        assertThat(connector.countRecords()).isEqualTo(2);

        Iterator<SourceRecord> iterator = connector.readRecords(1);

        assertThat(iterator.hasNext()).isTrue();
        SourceRecord first = iterator.next();
        assertThat(first.getSourceFile()).isEqualTo("/inbound/a.hl7");
        assertThat(first.getDataType()).isEqualTo(DataType.HL7V2);

        assertThat(iterator.hasNext()).isTrue();
        SourceRecord second = iterator.next();
        assertThat(second.getSourceFile()).isEqualTo("/inbound/b.xml");
        assertThat(second.getDataType()).isEqualTo(DataType.CDA);
    }

    @Test
    @DisplayName("Should seek and restore from checkpoint")
    void shouldSeekAndRestoreFromCheckpoint() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .password("secret")
                .authType(SourceConfig.AuthType.PASSWORD)
                .remotePath("/inbound")
                .filePattern("*")
                .build();

        setupSftpListing("/inbound", "a.hl7", "b.xml");
        when(sftpChannel.get(eq("/inbound/b.xml")))
                .thenReturn(new ByteArrayInputStream("<ClinicalDocument/>".getBytes(StandardCharsets.UTF_8)));

        connector.connect(config);
        connector.seek(1);

        Map<String, Object> checkpoint = connector.getCheckpointData();
        connector.restoreFromCheckpoint(checkpoint);

        assertThat(connector.getCurrentPosition()).isEqualTo(1);

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        assertThat(iterator.hasNext()).isTrue();
        SourceRecord record = iterator.next();
        assertThat(record.getSourceFile()).isEqualTo("/inbound/b.xml");
    }

    @Test
    @DisplayName("Should disconnect cleanly")
    void shouldDisconnectCleanly() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .password("secret")
                .authType(SourceConfig.AuthType.PASSWORD)
                .remotePath("/inbound")
                .filePattern("*")
                .build();

        setupSftpListing("/inbound", "a.hl7");
        connector.connect(config);

        connector.disconnect();

        verify(sftpChannel).disconnect();
        verify(session).disconnect();
    }

    @Test
    @DisplayName("Should support key-based authentication and empty listings")
    void shouldSupportKeyAuthAndEmptyListings() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .authType(SourceConfig.AuthType.KEY)
                .privateKeyPath("/keys/id_rsa")
                .remotePath("/inbound")
                .filePattern("*")
                .build();

        when(sftpChannel.ls(eq("/inbound"))).thenReturn(new Vector<>());

        connector.connect(config);

        verify(jsch).addIdentity(eq("/keys/id_rsa"));
        verify(session).setConfig(eq("StrictHostKeyChecking"), eq("no"));

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Should honor file pattern and detect data type by extension")
    void shouldHonorFilePatternAndDetectDataType() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .password("secret")
                .authType(SourceConfig.AuthType.PASSWORD)
                .remotePath("/inbound")
                .filePattern("*.json")
                .build();

        setupSftpListing("/inbound", "a.hl7", "b.txt", "c.json");
        when(sftpChannel.get(eq("/inbound/c.json")))
                .thenReturn(new ByteArrayInputStream("{\"resourceType\":\"Bundle\"}"
                        .getBytes(StandardCharsets.UTF_8)));

        connector.connect(config);

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        assertThat(iterator.hasNext()).isTrue();
        SourceRecord record = iterator.next();
        assertThat(record.getSourceFile()).isEqualTo("/inbound/c.json");
        assertThat(record.getDataType()).isEqualTo(DataType.FHIR_BUNDLE);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Should default to HL7 when extension is unknown")
    void shouldDefaultToHl7ForUnknownExtension() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .password("secret")
                .authType(SourceConfig.AuthType.PASSWORD)
                .remotePath("/inbound")
                .filePattern("*")
                .build();

        setupSftpListing("/inbound", "unknown.data");
        when(sftpChannel.get(eq("/inbound/unknown.data")))
                .thenReturn(new ByteArrayInputStream("MSH|^~\\&|TEST".getBytes(StandardCharsets.UTF_8)));

        connector.connect(config);

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        SourceRecord record = iterator.next();
        assertThat(record.getDataType()).isEqualTo(DataType.HL7V2);
    }

    @Test
    @DisplayName("Should respect configured data type override")
    void shouldRespectDataTypeOverride() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .password("secret")
                .authType(SourceConfig.AuthType.PASSWORD)
                .remotePath("/inbound")
                .filePattern("*.hl7")
                .dataType(DataType.CDA)
                .build();

        setupSftpListing("/inbound", "a.hl7");
        when(sftpChannel.get(eq("/inbound/a.hl7")))
                .thenReturn(new ByteArrayInputStream("<ClinicalDocument/>"
                        .getBytes(StandardCharsets.UTF_8)));

        connector.connect(config);

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        SourceRecord record = iterator.next();
        assertThat(record.getDataType()).isEqualTo(DataType.CDA);
    }

    @Test
    @DisplayName("Should throw runtime when iterator encounters SFTP error")
    void shouldThrowWhenIteratorFails() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.SFTP)
                .host("sftp.example")
                .port(2222)
                .username("user")
                .password("secret")
                .authType(SourceConfig.AuthType.PASSWORD)
                .remotePath("/inbound")
                .filePattern("*")
                .build();

        setupSftpListing("/inbound", "a.hl7");
        when(sftpChannel.get(eq("/inbound/a.hl7"))).thenThrow(new com.jcraft.jsch.SftpException(4, "boom"));

        connector.connect(config);

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(iterator::hasNext)
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error reading next record");
    }

    private void setupSftpListing(String remotePath, String... files) throws Exception {
        Vector<ChannelSftp.LsEntry> entries = new Vector<>();
        for (String filename : files) {
            ChannelSftp.LsEntry entry = mock(ChannelSftp.LsEntry.class);
            SftpATTRS attrs = mock(SftpATTRS.class);
            when(attrs.isDir()).thenReturn(false);
            when(entry.getAttrs()).thenReturn(attrs);
            when(entry.getFilename()).thenReturn(filename);
            entries.add(entry);
        }
        when(sftpChannel.ls(eq(remotePath))).thenReturn(entries);
    }

    private static class TestSftpSourceConnector extends SftpSourceConnector {
        private final JSch jsch;

        private TestSftpSourceConnector(JSch jsch) {
            this.jsch = jsch;
        }

        @Override
        protected JSch createJsch() {
            return jsch;
        }
    }
}

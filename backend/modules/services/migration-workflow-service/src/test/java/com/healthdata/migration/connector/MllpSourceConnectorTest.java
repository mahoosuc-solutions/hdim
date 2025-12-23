package com.healthdata.migration.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.SourceConfig;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;

@DisplayName("MllpSourceConnector")
class MllpSourceConnectorTest {

    private static final byte START_BLOCK = 0x0B;
    private static final byte END_BLOCK = 0x1C;
    private static final byte CARRIAGE_RETURN = 0x0D;

    private final MllpSourceConnector connector = new MllpSourceConnector();

    @AfterEach
    void tearDown() {
        connector.disconnect();
    }

    @Test
    @DisplayName("Should receive message and send ACK")
    void shouldReceiveMessageAndAck() throws Exception {
        int port = findFreePort();
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .host("localhost")
                .port(port)
                .bufferSize(10)
                .sendAck(true)
                .build();

        connector.connect(config);

        String message = "MSH|^~\\&|APP|FAC|REC|FAC|20240101000000||ADT^A01|MSG0001|P|2.5\r"
                + "PID|1||12345^^^MRN||DOE^JOHN\r";

        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(2000);
            sendMllpMessage(socket.getOutputStream(), message);

            String ack = readMllpMessage(socket.getInputStream());
            assertThat(ack).contains("MSA|AA|MSG0001");
        }

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        assertThat(iterator.hasNext()).isTrue();
        SourceRecord record = iterator.next();

        assertThat(record.getContent()).isEqualTo(message);
        assertThat(record.getDataType()).isEqualTo(DataType.HL7V2);
    }

    @Test
    @DisplayName("Should report unknown count for streaming source")
    void shouldReportUnknownCount() throws IOException {
        assertThat(connector.countRecords()).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should generate ACK with UNKNOWN for missing MSH")
    void shouldGenerateAckWithUnknown() throws Exception {
        int port = findFreePort();
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .host("localhost")
                .port(port)
                .bufferSize(10)
                .sendAck(true)
                .build();

        connector.connect(config);

        String message = "PID|1||12345^^^MRN||DOE^JOHN\r";

        try (Socket socket = new Socket("localhost", port)) {
            socket.setSoTimeout(2000);
            sendMllpMessage(socket.getOutputStream(), message);

            String ack = readMllpMessage(socket.getInputStream());
            assertThat(ack).contains("MSA|AA|UNKNOWN");
        }
    }

    @Test
    @DisplayName("Should send NAK when queue is full")
    void shouldSendNakWhenQueueFull() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .sendAck(true)
                .build();

        BlockingQueue<String> queue = mockStringQueue();
        Mockito.when(queue.offer(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenReturn(false);

        ReflectionTestUtils.setField(connector, "config", config);
        ReflectionTestUtils.setField(connector, "messageQueue", queue);
        ReflectionTestUtils.setField(connector, "messagesReceived", new AtomicLong(0));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ReflectionTestUtils.invokeMethod(connector, "processMessage",
                "MSH|^~\\&|APP|FAC|REC|FAC|20240101000000||ADT^A01|MSG0002|P|2.5\r",
                outputStream);

        String response = unwrapMllpPayload(outputStream.toByteArray());
        assertThat(response).contains("MSA|AE|MSG0002");
    }

    @Test
    @DisplayName("Should not send ACK when disabled")
    void shouldNotSendAckWhenDisabled() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .sendAck(false)
                .build();

        BlockingQueue<String> queue = mockStringQueue();
        Mockito.when(queue.offer(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenReturn(true);

        ReflectionTestUtils.setField(connector, "config", config);
        ReflectionTestUtils.setField(connector, "messageQueue", queue);
        ReflectionTestUtils.setField(connector, "messagesReceived", new AtomicLong(0));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ReflectionTestUtils.invokeMethod(connector, "processMessage",
                "MSH|^~\\&|APP|FAC|REC|FAC|20240101000000||ADT^A01|MSG0003|P|2.5\r",
                outputStream);

        assertThat(outputStream.toByteArray()).isEmpty();
    }

    @Test
    @DisplayName("Should bind to configured address and track active connections")
    void shouldBindToConfiguredAddress() throws Exception {
        int port = findFreePort();
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .host("localhost")
                .bindAddress("127.0.0.1")
                .port(port)
                .bufferSize(10)
                .sendAck(false)
                .maxConnections(1)
                .build();

        connector.connect(config);

        ServerSocket serverSocket = (ServerSocket) ReflectionTestUtils.getField(connector, "serverSocket");
        assertThat(serverSocket).isNotNull();
        assertThat(serverSocket.getInetAddress().getHostAddress()).isEqualTo("127.0.0.1");

        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(2000);
            assertThat(ReflectionTestUtils.getField(connector, "maxConnections")).isEqualTo(1);
            assertThat(awaitActiveConnections(1)).isTrue();
        }
    }

    @Test
    @DisplayName("Should include messagesReceived in checkpoint data")
    void shouldIncludeMessagesReceivedInCheckpoint() {
        ReflectionTestUtils.setField(connector, "messagesReceived", new AtomicLong(3));
        Map<String, Object> checkpoint = connector.getCheckpointData();
        assertThat(checkpoint).containsEntry("messagesReceived", 3L);
    }

    @Test
    @DisplayName("Should return false when queue is empty")
    void shouldReturnFalseWhenQueueEmpty() throws Exception {
        BlockingQueue<String> queue = mockStringQueue();
        Mockito.when(queue.poll(Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenReturn(null);

        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .host("localhost")
                .port(2575)
                .build();

        ReflectionTestUtils.setField(connector, "config", config);
        ReflectionTestUtils.setField(connector, "messageQueue", queue);
        ReflectionTestUtils.setField(connector, "messagesReceived", new AtomicLong(0));
        ReflectionTestUtils.setField(connector, "running", new java.util.concurrent.atomic.AtomicBoolean(true));
        ReflectionTestUtils.setField(connector, "connected", true);

        Iterator<SourceRecord> iterator = connector.readRecords(1);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Should handle non-CR byte after end block")
    void shouldHandleNonCrAfterEndBlock() throws Exception {
        BlockingQueue<String> queue = new java.util.concurrent.LinkedBlockingQueue<>();
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .sendAck(false)
                .build();

        ReflectionTestUtils.setField(connector, "config", config);
        ReflectionTestUtils.setField(connector, "messageQueue", queue);
        ReflectionTestUtils.setField(connector, "messagesReceived", new AtomicLong(0));

        byte[] payload = "MSH|^~\\&|APP|FAC\r".getBytes(StandardCharsets.UTF_8);
        byte[] data = new byte[payload.length + 3];
        data[0] = START_BLOCK;
        System.arraycopy(payload, 0, data, 1, payload.length);
        data[data.length - 2] = END_BLOCK;
        data[data.length - 1] = 'X';

        Socket socket = Mockito.mock(Socket.class);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Mockito.when(socket.getInputStream()).thenReturn(inputStream);
        Mockito.when(socket.getOutputStream()).thenReturn(outputStream);

        ReflectionTestUtils.invokeMethod(connector, "handleConnection", socket);

        String queued = queue.poll(1, TimeUnit.SECONDS);
        assertThat(queued).contains("MSH|^~\\&|APP|FAC").contains("X");
    }

    @Test
    @DisplayName("Should handle interrupted queue offer")
    void shouldHandleInterruptedQueueOffer() throws Exception {
        SourceConfig config = SourceConfig.builder()
                .sourceType(SourceType.MLLP)
                .sendAck(true)
                .build();

        BlockingQueue<String> queue = mockStringQueue();
        Mockito.when(queue.offer(Mockito.anyString(), Mockito.anyLong(), Mockito.any(TimeUnit.class)))
                .thenThrow(new InterruptedException("interrupted"));

        ReflectionTestUtils.setField(connector, "config", config);
        ReflectionTestUtils.setField(connector, "messageQueue", queue);
        ReflectionTestUtils.setField(connector, "messagesReceived", new AtomicLong(0));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(connector, "processMessage",
                "MSH|^~\\&|APP|FAC|REC|FAC|20240101000000||ADT^A01|MSG0004|P|2.5\r",
                outputStream)).doesNotThrowAnyException();

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        Thread.interrupted();
    }

    @Test
    @DisplayName("Should handle disconnect when server socket close fails")
    void shouldHandleDisconnectWhenServerSocketCloseFails() throws Exception {
        ServerSocket serverSocket = Mockito.mock(ServerSocket.class);
        Mockito.when(serverSocket.isClosed()).thenReturn(false);
        Mockito.doThrow(new IOException("close failed")).when(serverSocket).close();

        ReflectionTestUtils.setField(connector, "serverSocket", serverSocket);
        ReflectionTestUtils.setField(connector, "running", new java.util.concurrent.atomic.AtomicBoolean(true));
        ReflectionTestUtils.setField(connector, "listenerThread", new Thread());
        ReflectionTestUtils.setField(connector, "connected", true);

        assertThatCode(connector::disconnect).doesNotThrowAnyException();
    }

    private static void sendMllpMessage(OutputStream outputStream, String message) throws IOException {
        outputStream.write(START_BLOCK);
        outputStream.write(message.getBytes(StandardCharsets.UTF_8));
        outputStream.write(END_BLOCK);
        outputStream.write(CARRIAGE_RETURN);
        outputStream.flush();
    }

    private static String readMllpMessage(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        boolean inMessage = false;
        int value;
        while ((value = inputStream.read()) != -1) {
            if (!inMessage) {
                if (value == START_BLOCK) {
                    inMessage = true;
                }
                continue;
            }
            if (value == END_BLOCK) {
                inputStream.read(); // consume trailing CR
                break;
            }
            buffer.write(value);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static String unwrapMllpPayload(byte[] data) {
        if (data.length == 0) {
            return "";
        }
        int start = data[0] == START_BLOCK ? 1 : 0;
        int end = data.length;
        if (end >= 2 && data[end - 2] == END_BLOCK) {
            end -= 2;
        }
        return new String(data, start, Math.max(0, end - start), StandardCharsets.UTF_8);
    }

    private static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @SuppressWarnings("unchecked")
    private static BlockingQueue<String> mockStringQueue() {
        return Mockito.mock(BlockingQueue.class);
    }

    private boolean awaitActiveConnections(int expected) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            Object value = ReflectionTestUtils.getField(connector, "activeConnections");
            if (value instanceof java.util.concurrent.atomic.AtomicInteger active &&
                active.get() >= expected) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }
}

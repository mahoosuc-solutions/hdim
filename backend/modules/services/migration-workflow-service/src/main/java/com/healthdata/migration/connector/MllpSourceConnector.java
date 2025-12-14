package com.healthdata.migration.connector;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.healthdata.migration.dto.DataType;
import com.healthdata.migration.dto.SourceRecord;
import com.healthdata.migration.dto.SourceType;

/**
 * Source connector for receiving HL7 v2 messages via MLLP (Minimal Lower Layer Protocol).
 * Acts as a server that listens for incoming connections.
 */
public class MllpSourceConnector extends AbstractSourceConnector {

    // MLLP framing characters
    private static final byte START_BLOCK = 0x0B;  // VT (vertical tab)
    private static final byte END_BLOCK = 0x1C;    // FS (file separator)
    private static final byte CARRIAGE_RETURN = 0x0D;

    private ServerSocket serverSocket;
    private BlockingQueue<String> messageQueue;
    private AtomicBoolean running;
    private AtomicLong messagesReceived;
    private Thread listenerThread;

    @Override
    public SourceType getType() {
        return SourceType.MLLP;
    }

    @Override
    protected void doConnect() throws IOException {
        int port = config.getPort() != null ? config.getPort() : 2575;
        String bindAddress = config.getBindAddress() != null ? config.getBindAddress() : "0.0.0.0";
        int bufferSize = config.getBufferSize() != null ? config.getBufferSize() : 1000;

        messageQueue = new LinkedBlockingQueue<>(bufferSize);
        running = new AtomicBoolean(true);
        messagesReceived = new AtomicLong(0);

        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);

        // Start listener thread
        listenerThread = new Thread(this::runListener, "mllp-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        log.info("MLLP listener started on {}:{}", bindAddress, port);
    }

    private void runListener() {
        int maxConnections = config.getMaxConnections() != null ? config.getMaxConnections() : 10;

        while (running.get()) {
            try {
                Socket clientSocket = serverSocket.accept();
                log.debug("MLLP connection from {}", clientSocket.getRemoteSocketAddress());

                // Handle connection in a new thread
                new Thread(() -> handleConnection(clientSocket), "mllp-handler").start();

            } catch (IOException e) {
                if (running.get()) {
                    log.error("Error accepting MLLP connection", e);
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try {
            var inputStream = socket.getInputStream();
            var outputStream = socket.getOutputStream();
            StringBuilder messageBuffer = new StringBuilder();
            boolean inMessage = false;

            int b;
            while ((b = inputStream.read()) != -1) {
                if (b == START_BLOCK) {
                    inMessage = true;
                    messageBuffer.setLength(0);
                } else if (b == END_BLOCK && inMessage) {
                    // Read trailing CR if present
                    int next = inputStream.read();
                    if (next != CARRIAGE_RETURN) {
                        // Put it back if it's not CR
                        messageBuffer.append((char) next);
                    }

                    // Complete message received
                    String message = messageBuffer.toString();
                    processMessage(message, outputStream);
                    inMessage = false;
                } else if (inMessage) {
                    messageBuffer.append((char) b);
                }
            }
        } catch (IOException e) {
            log.debug("MLLP connection closed: {}", e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    private void processMessage(String message, java.io.OutputStream outputStream) throws IOException {
        try {
            // Add message to queue (blocking if full)
            boolean added = messageQueue.offer(message, 5, TimeUnit.SECONDS);
            if (added) {
                messagesReceived.incrementAndGet();

                // Send ACK if configured
                if (config.isSendAck()) {
                    String ack = generateAck(message);
                    sendMllpMessage(outputStream, ack);
                }
            } else {
                log.warn("Message queue full, dropping message");
                // Send NAK
                String nak = generateNak(message, "Queue full");
                sendMllpMessage(outputStream, nak);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendMllpMessage(java.io.OutputStream outputStream, String message) throws IOException {
        outputStream.write(START_BLOCK);
        outputStream.write(message.getBytes());
        outputStream.write(END_BLOCK);
        outputStream.write(CARRIAGE_RETURN);
        outputStream.flush();
    }

    private String generateAck(String message) {
        // Simple ACK - in production, parse MSH and create proper ACK
        String msgControlId = extractMsgControlId(message);
        return "MSH|^~\\&|ACK|HDIM|SENDER|FACILITY|" +
               java.time.Instant.now().toString().replace(":", "").replace("-", "") +
               "||ACK|" + msgControlId + "|P|2.5\r" +
               "MSA|AA|" + msgControlId + "\r";
    }

    private String generateNak(String message, String reason) {
        String msgControlId = extractMsgControlId(message);
        return "MSH|^~\\&|NAK|HDIM|SENDER|FACILITY|" +
               java.time.Instant.now().toString().replace(":", "").replace("-", "") +
               "||ACK|" + msgControlId + "|P|2.5\r" +
               "MSA|AE|" + msgControlId + "|" + reason + "\r";
    }

    private String extractMsgControlId(String message) {
        // Extract MSH-10 (Message Control ID)
        String[] segments = message.split("\r");
        if (segments.length > 0 && segments[0].startsWith("MSH")) {
            String[] fields = segments[0].split("\\|");
            if (fields.length > 9) {
                return fields[9];
            }
        }
        return "UNKNOWN";
    }

    @Override
    protected void doDisconnect() {
        running.set(false);

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.warn("Error closing MLLP server socket", e);
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    @Override
    public boolean testConnection() {
        return serverSocket != null && !serverSocket.isClosed();
    }

    @Override
    public long countRecords() throws IOException {
        // For MLLP, we don't know the total count in advance
        return -1;
    }

    @Override
    public Iterator<SourceRecord> readRecords(int batchSize) throws IOException {
        ensureConnected();
        return new MllpRecordIterator();
    }

    @Override
    public void seek(long offset) throws IOException {
        // MLLP is a streaming source, seeking not supported
        log.warn("Seek not supported for MLLP source");
    }

    @Override
    protected void addCheckpointData(Map<String, Object> checkpoint) {
        checkpoint.put("messagesReceived", messagesReceived.get());
    }

    /**
     * Iterator that reads messages from the MLLP queue
     */
    private class MllpRecordIterator implements Iterator<SourceRecord> {
        private SourceRecord nextRecord;
        private long recordIndex = 0;

        @Override
        public boolean hasNext() {
            if (nextRecord != null) {
                return true;
            }
            try {
                // Wait for next message with timeout
                String message = messageQueue.poll(5, TimeUnit.SECONDS);
                if (message != null) {
                    nextRecord = SourceRecord.of(
                            message,
                            DataType.HL7V2,
                            "mllp:" + config.getHost() + ":" + config.getPort(),
                            recordIndex++
                    );
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        }

        @Override
        public SourceRecord next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            SourceRecord record = nextRecord;
            nextRecord = null;
            currentPosition++;
            return record;
        }
    }
}

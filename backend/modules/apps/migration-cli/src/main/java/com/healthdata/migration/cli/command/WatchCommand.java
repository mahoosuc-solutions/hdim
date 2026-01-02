package com.healthdata.migration.cli.command;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.migration.cli.output.ProgressRenderer;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Watch real-time migration progress
 */
@Component
@Command(name = "watch", description = "Watch real-time progress of a migration job")
public class WatchCommand implements Callable<Integer> {

    @ParentCommand
    private com.healthdata.migration.cli.MigrationCommand parent;

    private final ProgressRenderer progressRenderer;
    private final ObjectMapper objectMapper;

    @Parameters(index = "0", description = "Job ID")
    UUID jobId;

    private volatile boolean running = true;
    private CountDownLatch completionLatch = new CountDownLatch(1);

    public WatchCommand(ProgressRenderer progressRenderer) {
        this.progressRenderer = progressRenderer;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Integer call() {
        System.out.println("Connecting to migration progress stream...");
        System.out.println("Press Ctrl+C to stop watching (job will continue running)");
        System.out.println();

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            completionLatch.countDown();
        }));

        try {
            String wsUrl = parent.apiUrl.replace("http://", "ws://")
                    .replace("https://", "wss://")
                    + "/api/v1/migrations/" + jobId + "/stream";

            StandardWebSocketClient client = new StandardWebSocketClient();
            WebSocketSession session = client.execute(new ProgressWebSocketHandler(), wsUrl).get();

            // Wait for completion or interrupt
            completionLatch.await();

            if (session.isOpen()) {
                session.close();
            }

            return 0;
        } catch (Exception e) {
            System.err.println("Error watching job: " + e.getMessage());
            return 1;
        }
    }

    private class ProgressWebSocketHandler extends TextWebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            System.out.println("Connected to progress stream");
            System.out.println();
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            try {
                JsonNode event = objectMapper.readTree(message.getPayload());
                String type = event.has("type") ? event.get("type").asText() : "unknown";

                switch (type) {
                    case "subscribed":
                        System.out.println("Subscribed to job " + jobId);
                        break;

                    case "PROGRESS_UPDATE":
                        JsonNode payload = event.get("payload");
                        progressRenderer.renderProgress(
                                payload.get("processedCount").asLong(),
                                payload.get("totalRecords").asLong(),
                                payload.get("successCount").asLong(),
                                payload.get("failureCount").asLong(),
                                payload.has("recordsPerSecond") ? payload.get("recordsPerSecond").asDouble() : 0,
                                payload.has("estimatedTimeRemainingMs") ? payload.get("estimatedTimeRemainingMs").asLong() : 0
                        );
                        break;

                    case "STATUS_CHANGED":
                        String status = event.get("payload").get("status").asText();
                        System.out.println("\nStatus changed: " + status);
                        break;

                    case "ERROR_OCCURRED":
                        String error = event.get("payload").get("error").asText();
                        System.err.println("\nError: " + error);
                        break;

                    case "JOB_COMPLETED":
                        System.out.println("\n\nJob completed!");
                        JsonNode summary = event.get("payload");
                        if (summary != null) {
                            System.out.println("Final status: " + summary.get("finalStatus").asText());
                            System.out.println("Total processed: " + summary.get("totalRecords").asLong());
                            System.out.println("Success: " + summary.get("successCount").asLong());
                            System.out.println("Failed: " + summary.get("failureCount").asLong());
                        }
                        running = false;
                        completionLatch.countDown();
                        break;

                    default:
                        if (parent.verbose) {
                            System.out.println("Event: " + type);
                        }
                }
            } catch (Exception e) {
                System.err.println("Error parsing event: " + e.getMessage());
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            System.out.println("\nConnection closed: " + status.getReason());
            completionLatch.countDown();
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            System.err.println("Connection error: " + exception.getMessage());
            completionLatch.countDown();
        }
    }
}

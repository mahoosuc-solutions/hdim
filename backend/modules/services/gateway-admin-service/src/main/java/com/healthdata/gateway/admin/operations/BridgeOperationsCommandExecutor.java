package com.healthdata.gateway.admin.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BridgeOperationsCommandExecutor implements OperationsCommandExecutor {

    private static final Pattern SCHEDULE_MODE_PATTERN = Pattern.compile("SEED_SCHEDULE_MODE=([a-z-]+)");

    private final OperationsProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public BridgeOperationsCommandExecutor(OperationsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public CommandResult execute(UUID runId, String commandText) {
        try {
            Map<String, Object> payload = mapCommand(commandText);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl() + "/ops/command"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(properties.getBridge().getTimeoutSeconds()))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new CommandResult(1, "Bridge command failed with HTTP " + response.statusCode() + "\n" + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode lastCommand = root.path("lastCommand");
            int exitCode = lastCommand.path("exitCode").asInt(1);
            String output = renderOutput(lastCommand, root);
            return new CommandResult(exitCode, output);
        } catch (Exception e) {
            return new CommandResult(1, "Bridge command execution error: " + e.getMessage());
        }
    }

    @Override
    public void cancel(UUID runId) {
        // Current ops bridge does not support remote cancellation.
    }

    private String renderOutput(JsonNode lastCommand, JsonNode root) {
        StringBuilder output = new StringBuilder();
        output.append("Bridge action: ").append(lastCommand.path("action").asText("unknown")).append('\n');
        output.append("Duration(ms): ").append(lastCommand.path("durationMs").asLong(0)).append('\n');

        JsonNode tail = lastCommand.path("outputTail");
        if (tail.isArray()) {
            for (JsonNode line : tail) {
                output.append(line.asText()).append('\n');
            }
        }

        JsonNode services = root.path("services");
        if (services.isArray() && services.size() > 0) {
            output.append("\nCompose services:\n");
            for (JsonNode service : services) {
                output.append("- ")
                    .append(service.path("name").asText("unknown"))
                    .append(": ")
                    .append(service.path("state").asText("unknown"));
                if (!service.path("health").asText("").isBlank()) {
                    output.append(" (").append(service.path("health").asText()).append(')');
                }
                output.append('\n');
            }
        }

        return output.toString();
    }

    private Map<String, Object> mapCommand(String commandText) {
        String trimmed = commandText == null ? "" : commandText.trim();

        if (trimmed.equals(properties.getStackStartCommand())) {
            return Map.of("action", "start");
        }
        if (trimmed.equals(properties.getStackStopCommand())) {
            return Map.of("action", "stop");
        }
        if (trimmed.equals(properties.getValidateCommand())) {
            return Map.of("action", "validate");
        }

        if (trimmed.contains("seed-fhir-schedule.sh") && !trimmed.contains("seed-all-demo-data.sh")) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", "seed-schedule");
            payload.put("scheduleMode", parseScheduleMode(trimmed));
            return payload;
        }

        if (trimmed.contains("seed-all-demo-data.sh")) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("action", "seed");
            payload.put("profile", trimmed.contains("SEED_PROFILE=full") ? "full" : "smoke");
            payload.put("scheduleMode", parseScheduleMode(trimmed));
            return payload;
        }

        throw new IllegalArgumentException("Unsupported bridge command: " + trimmed);
    }

    private String parseScheduleMode(String commandText) {
        Matcher matcher = SCHEDULE_MODE_PATTERN.matcher(commandText);
        if (!matcher.find()) {
            return "none";
        }

        String mode = matcher.group(1);
        return switch (mode) {
            case "none", "appointment-task", "encounter", "both" -> mode;
            default -> "none";
        };
    }

    private String baseUrl() {
        String raw = properties.getBridge().getBaseUrl();
        if (raw.endsWith("/")) {
            return raw.substring(0, raw.length() - 1);
        }
        return raw;
    }
}

package com.healthdata.gateway.admin.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BridgeOperationsCommandExecutor.
 * Tests command mapping logic via reflection and error handling through execute().
 */
@Tag("unit")
@DisplayName("BridgeOperationsCommandExecutor")
class BridgeOperationsCommandExecutorTest {

    private OperationsProperties properties;
    private BridgeOperationsCommandExecutor executor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        properties = new OperationsProperties();
        executor = new BridgeOperationsCommandExecutor(properties, objectMapper);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeMapCommand(String commandText) throws Exception {
        Method method = BridgeOperationsCommandExecutor.class.getDeclaredMethod("mapCommand", String.class);
        method.setAccessible(true);
        return (Map<String, Object>) method.invoke(executor, commandText);
    }

    @Test
    @DisplayName("should map stack start command to start action")
    void shouldMapStackStartCommand_ToStartAction() throws Exception {
        Map<String, Object> result = invokeMapCommand(properties.getStackStartCommand());

        assertThat(result).containsEntry("action", "start");
    }

    @Test
    @DisplayName("should map stack stop command to stop action")
    void shouldMapStackStopCommand_ToStopAction() throws Exception {
        Map<String, Object> result = invokeMapCommand(properties.getStackStopCommand());

        assertThat(result).containsEntry("action", "stop");
    }

    @Test
    @DisplayName("should map seed command to seed action with full profile")
    void shouldMapSeedCommand_ToSeedActionWithFullProfile() throws Exception {
        String command = "SEED_PROFILE=full ./scripts/seed-all-demo-data.sh";

        Map<String, Object> result = invokeMapCommand(command);

        assertThat(result)
            .containsEntry("action", "seed")
            .containsEntry("profile", "full");
    }

    @Test
    @DisplayName("should parse schedule mode when present in seed command")
    void shouldParseScheduleMode_WhenPresent() throws Exception {
        String command = "SEED_SCHEDULE_MODE=encounter ./scripts/seed-all-demo-data.sh";

        Map<String, Object> result = invokeMapCommand(command);

        assertThat(result)
            .containsEntry("action", "seed")
            .containsEntry("scheduleMode", "encounter");
    }

    @Test
    @DisplayName("should return exitCode 1 when bridge URL is unreachable")
    void shouldReturnExitCode1_WhenExceptionOccurs() {
        // Default bridge URL (http://ops-service:4710) is unreachable in test
        OperationsCommandExecutor.CommandResult result = executor.execute(
            UUID.randomUUID(),
            properties.getStackStartCommand()
        );

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.output()).containsIgnoringCase("error");
    }
}

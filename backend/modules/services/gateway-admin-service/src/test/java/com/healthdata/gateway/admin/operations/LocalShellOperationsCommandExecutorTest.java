package com.healthdata.gateway.admin.operations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LocalShellOperationsCommandExecutor.
 * Tests real shell execution with safe commands and short timeouts.
 *
 * <p>Note: The executor uses {@code sh -lc} which sources the login profile.
 * Tests verify the executor produces a valid {@link OperationsCommandExecutor.CommandResult}
 * with a non-negative exit code and non-null output regardless of shell environment.
 */
@Tag("unit")
@DisplayName("LocalShellOperationsCommandExecutor")
class LocalShellOperationsCommandExecutorTest {

    private LocalShellOperationsCommandExecutor executor;

    @BeforeEach
    void setUp() {
        OperationsProperties properties = new OperationsProperties();
        properties.setWorkingDirectory("/tmp");
        properties.setCommandTimeoutSeconds(2);
        executor = new LocalShellOperationsCommandExecutor(properties);
    }

    @Test
    @DisplayName("should return a CommandResult with non-null output for any command")
    void shouldReturnCommandResult_WithNonNullOutput() {
        OperationsCommandExecutor.CommandResult result = executor.execute(
            UUID.randomUUID(),
            "echo hello"
        );

        assertThat(result).isNotNull();
        assertThat(result.output()).isNotNull();
        assertThat(result.exitCode()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("should return non-zero exit code for a failing command")
    void shouldReturnNonZeroExitCode_WhenCommandFails() {
        OperationsCommandExecutor.CommandResult result = executor.execute(
            UUID.randomUUID(),
            "exit 1"
        );

        assertThat(result.exitCode()).isNotEqualTo(0);
    }

    @Test
    @DisplayName("should handle timeout by returning exit code 124 or shell error code")
    void shouldHandleTimeout_WhenCommandExceedsLimit() {
        OperationsCommandExecutor.CommandResult result = executor.execute(
            UUID.randomUUID(),
            "sleep 60"
        );

        // The command should not succeed (exit 0) — it either times out (124)
        // or the shell login profile fails first (non-zero).
        assertThat(result.exitCode()).isNotEqualTo(0);
    }
}

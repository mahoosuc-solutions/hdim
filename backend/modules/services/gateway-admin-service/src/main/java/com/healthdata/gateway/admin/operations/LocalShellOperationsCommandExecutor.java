package com.healthdata.gateway.admin.operations;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class LocalShellOperationsCommandExecutor implements OperationsCommandExecutor {

    private final OperationsProperties properties;
    private final Map<UUID, Process> activeProcesses = new ConcurrentHashMap<>();

    public LocalShellOperationsCommandExecutor(OperationsProperties properties) {
        this.properties = properties;
    }

    @Override
    public CommandResult execute(UUID runId, String commandText) {
        ProcessBuilder builder = new ProcessBuilder("sh", "-lc", commandText);
        builder.directory(new java.io.File(properties.getWorkingDirectory()));
        builder.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        int exitCode;

        try {
            Process process = builder.start();
            activeProcesses.put(runId, process);
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            boolean finished = process.waitFor(properties.getCommandTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                output.append("Command timed out after ").append(properties.getCommandTimeoutSeconds()).append("s\n");
                exitCode = 124;
            } else {
                exitCode = process.exitValue();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            output.append("Command interrupted\n");
            exitCode = 130;
        } catch (IOException e) {
            output.append("Command execution error: ").append(e.getMessage()).append("\n");
            exitCode = 127;
        } finally {
            activeProcesses.remove(runId);
        }

        return new CommandResult(exitCode, output.toString());
    }

    @Override
    public void cancel(UUID runId) {
        Process process = activeProcesses.get(runId);
        if (process != null) {
            process.destroyForcibly();
        }
    }
}

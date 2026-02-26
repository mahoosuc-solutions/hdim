package com.healthdata.gateway.admin.operations;

import java.util.UUID;

public interface OperationsCommandExecutor {
    CommandResult execute(UUID runId, String commandText);
    void cancel(UUID runId);

    record CommandResult(int exitCode, String output) {}
}

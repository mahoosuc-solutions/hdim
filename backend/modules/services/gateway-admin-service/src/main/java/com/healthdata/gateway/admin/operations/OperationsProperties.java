package com.healthdata.gateway.admin.operations;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "operations")
public class OperationsProperties {

    public enum ExecutionMode {
        LOCAL,
        BRIDGE
    }

    private String workingDirectory = "/workspace";
    private long commandTimeoutSeconds = 3600;
    private String stackStartCommand = "docker compose -f docker-compose.demo.yml up -d";
    private String stackStopCommand = "docker compose -f docker-compose.demo.yml down";
    private String seedCommand = "./scripts/seed-all-demo-data.sh";
    private String seedFullCommand = "SEED_PROFILE=full ./scripts/seed-all-demo-data.sh";
    private String seedScheduleCommand = "SEED_SCHEDULE_MODE=both ./scripts/seed-fhir-schedule.sh";
    private String validateCommand = "./validate-system.sh";
    private ExecutionMode executionMode = ExecutionMode.LOCAL;
    private Bridge bridge = new Bridge();

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public long getCommandTimeoutSeconds() {
        return commandTimeoutSeconds;
    }

    public void setCommandTimeoutSeconds(long commandTimeoutSeconds) {
        this.commandTimeoutSeconds = commandTimeoutSeconds;
    }

    public String getStackStartCommand() {
        return stackStartCommand;
    }

    public void setStackStartCommand(String stackStartCommand) {
        this.stackStartCommand = stackStartCommand;
    }

    public String getStackStopCommand() {
        return stackStopCommand;
    }

    public void setStackStopCommand(String stackStopCommand) {
        this.stackStopCommand = stackStopCommand;
    }

    public String getSeedCommand() {
        return seedCommand;
    }

    public void setSeedCommand(String seedCommand) {
        this.seedCommand = seedCommand;
    }

    public String getSeedFullCommand() {
        return seedFullCommand;
    }

    public void setSeedFullCommand(String seedFullCommand) {
        this.seedFullCommand = seedFullCommand;
    }

    public String getSeedScheduleCommand() {
        return seedScheduleCommand;
    }

    public void setSeedScheduleCommand(String seedScheduleCommand) {
        this.seedScheduleCommand = seedScheduleCommand;
    }

    public String getValidateCommand() {
        return validateCommand;
    }

    public void setValidateCommand(String validateCommand) {
        this.validateCommand = validateCommand;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public Bridge getBridge() {
        return bridge;
    }

    public void setBridge(Bridge bridge) {
        this.bridge = bridge;
    }

    public static class Bridge {
        private String baseUrl = "http://ops-service:4710";
        private long timeoutSeconds = 3600;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public long getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}

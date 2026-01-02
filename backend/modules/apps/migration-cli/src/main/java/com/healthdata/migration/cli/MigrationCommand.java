package com.healthdata.migration.cli;

import org.springframework.stereotype.Component;

import com.healthdata.migration.cli.command.JobCommand;
import com.healthdata.migration.cli.command.ReportCommand;
import com.healthdata.migration.cli.command.RunCommand;
import com.healthdata.migration.cli.command.TestCommand;
import com.healthdata.migration.cli.command.WatchCommand;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Main CLI command with subcommands
 */
@Component
@Command(
    name = "hdim-migrate",
    description = "Healthcare Data Migration CLI Tool",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    subcommands = {
        JobCommand.class,
        RunCommand.class,
        WatchCommand.class,
        ReportCommand.class,
        TestCommand.class
    }
)
public class MigrationCommand implements Runnable {

    @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
    public boolean verbose;

    @Option(names = {"--api-url"}, description = "Migration service URL", defaultValue = "http://localhost:8092")
    public String apiUrl;

    @Option(names = {"--tenant"}, description = "Tenant ID", defaultValue = "default")
    public String tenantId;

    @Override
    public void run() {
        // Show help if no subcommand specified
        System.out.println("Healthcare Data Migration CLI");
        System.out.println("Use --help for usage information");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  job      Manage migration jobs (create, list, start, pause, resume, cancel)");
        System.out.println("  run      Run a migration directly without creating a job");
        System.out.println("  watch    Watch real-time progress of a running job");
        System.out.println("  report   Generate reports for completed migrations");
        System.out.println("  test     Test source connections");
    }
}

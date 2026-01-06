package com.healthdata.democli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Demo CLI Application
 *
 * A command-line tool for managing HDIM demo data and scenarios.
 *
 * Usage:
 *   ./demo-cli reset                     # Reset all demo data
 *   ./demo-cli load-scenario <name>      # Load specific scenario
 *   ./demo-cli list-scenarios            # List available scenarios
 *   ./demo-cli generate-patients         # Generate synthetic patients
 *   ./demo-cli status                    # Check demo system status
 *   ./demo-cli snapshot create <name>    # Create database snapshot
 *   ./demo-cli snapshot restore <name>   # Restore from snapshot
 *   ./demo-cli snapshot list             # List available snapshots
 */
@SpringBootApplication
public class DemoCliApplication implements CommandLineRunner, ExitCodeGenerator {

    private final IFactory factory;
    private final DemoCliCommand demoCliCommand;
    private int exitCode;

    public DemoCliApplication(IFactory factory, DemoCliCommand demoCliCommand) {
        this.factory = factory;
        this.demoCliCommand = demoCliCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(DemoCliApplication.class, args)));
    }

    @Override
    public void run(String... args) {
        exitCode = new CommandLine(demoCliCommand, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}

package com.healthdata.democli;

import com.healthdata.democli.commands.*;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

/**
 * Main CLI command group for HDIM Demo Platform.
 *
 * Provides commands for:
 * - Resetting demo data
 * - Loading demo scenarios
 * - Generating synthetic patients
 * - Managing snapshots
 * - Checking system status
 */
@Component
@Command(
    name = "demo-cli",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "HDIM Demo Platform CLI - Manage demo data and scenarios",
    subcommands = {
        ResetCommand.class,
        LoadScenarioCommand.class,
        ListScenariosCommand.class,
        GeneratePatientsCommand.class,
        StatusCommand.class,
        SnapshotCommand.class,
        InitializeCommand.class
    }
)
public class DemoCliCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("""
            ╔═══════════════════════════════════════════════════════════════╗
            ║             HDIM Demo Platform CLI v1.0.0                     ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║  Commands:                                                    ║
            ║    reset              Reset all demo data                     ║
            ║    load-scenario      Load a specific demo scenario           ║
            ║    list-scenarios     List available demo scenarios           ║
            ║    generate-patients  Generate synthetic patients             ║
            ║    status             Check demo system status                ║
            ║    snapshot           Manage database snapshots               ║
            ║    initialize         Initialize demo environment             ║
            ║                                                               ║
            ║  Use --help with any command for more details                 ║
            ║  Example: demo-cli load-scenario --help                       ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);
    }
}

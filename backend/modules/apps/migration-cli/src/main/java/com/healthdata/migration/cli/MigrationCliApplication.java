package com.healthdata.migration.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Migration CLI Application
 *
 * Command-line tool for managing healthcare data migrations.
 * Communicates with the Migration Workflow Service via REST API.
 */
@SpringBootApplication
public class MigrationCliApplication implements CommandLineRunner, ExitCodeGenerator {

    private final IFactory factory;
    private final MigrationCommand migrationCommand;
    private int exitCode;

    public MigrationCliApplication(IFactory factory, MigrationCommand migrationCommand) {
        this.factory = factory;
        this.migrationCommand = migrationCommand;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(MigrationCliApplication.class, args)));
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(migrationCommand, factory)
                .setColorScheme(createColorScheme())
                .execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    private CommandLine.Help.ColorScheme createColorScheme() {
        return new CommandLine.Help.ColorScheme.Builder()
                .commands(CommandLine.Help.Ansi.Style.bold)
                .options(CommandLine.Help.Ansi.Style.fg_yellow)
                .parameters(CommandLine.Help.Ansi.Style.fg_cyan)
                .optionParams(CommandLine.Help.Ansi.Style.italic)
                .build();
    }
}

package com.healthdata.migration.cli.output;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.springframework.stereotype.Component;

/**
 * Terminal progress bar renderer with ANSI colors
 */
@Component
public class ProgressRenderer {

    private static final int BAR_WIDTH = 40;
    private static final char FILLED = '\u2588';  // Full block
    private static final char EMPTY = '\u2591';   // Light shade

    private long lastUpdate = 0;
    private static final long MIN_UPDATE_INTERVAL_MS = 100;

    public void renderProgress(long processed, long total, long success, long failure,
                               double recordsPerSecond, long estimatedTimeRemainingMs) {
        // Throttle updates to prevent flicker
        long now = System.currentTimeMillis();
        if (now - lastUpdate < MIN_UPDATE_INTERVAL_MS) {
            return;
        }
        lastUpdate = now;

        double percentage = total > 0 ? (double) processed / total * 100 : 0;
        int filledWidth = (int) (percentage / 100 * BAR_WIDTH);

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < BAR_WIDTH; i++) {
            if (i < filledWidth) {
                bar.append(Ansi.ansi().fg(Color.GREEN).a(FILLED).reset());
            } else {
                bar.append(EMPTY);
            }
        }
        bar.append("]");

        // Build status line
        String statusLine = String.format(
                "\r%s %5.1f%% | %s%d%s / %d | %s%d%s ok | %s%d%s err | %s%.1f/s%s | ETA: %s",
                bar.toString(),
                percentage,
                Ansi.ansi().fg(Color.WHITE).bold(),
                processed,
                Ansi.ansi().reset(),
                total,
                Ansi.ansi().fg(Color.GREEN),
                success,
                Ansi.ansi().reset(),
                failure > 0 ? Ansi.ansi().fg(Color.RED) : Ansi.ansi().fg(Color.WHITE),
                failure,
                Ansi.ansi().reset(),
                Ansi.ansi().fg(Color.CYAN),
                recordsPerSecond,
                Ansi.ansi().reset(),
                formatEta(estimatedTimeRemainingMs)
        );

        // Print without newline, overwriting previous line
        System.out.print(statusLine);
        System.out.flush();
    }

    public void renderComplete(long total, long success, long failure, long skipped, long durationMs) {
        System.out.println();
        System.out.println();

        String successRate = total > 0 ? String.format("%.1f%%", (double) success / total * 100) : "N/A";

        System.out.println(Ansi.ansi().fg(Color.GREEN).bold().a("Migration Complete!").reset());
        System.out.println();
        System.out.println("Summary:");
        System.out.println("--------");
        System.out.printf("  Total Records:    %d%n", total);
        System.out.printf("  Successful:       %s%d%s%n",
                Ansi.ansi().fg(Color.GREEN), success, Ansi.ansi().reset());
        if (failure > 0) {
            System.out.printf("  Failed:           %s%d%s%n",
                    Ansi.ansi().fg(Color.RED), failure, Ansi.ansi().reset());
        } else {
            System.out.printf("  Failed:           %d%n", failure);
        }
        System.out.printf("  Skipped:          %d%n", skipped);
        System.out.printf("  Success Rate:     %s%n", successRate);
        System.out.printf("  Duration:         %s%n", formatDuration(durationMs));
        System.out.printf("  Throughput:       %.1f records/sec%n",
                durationMs > 0 ? (double) total / durationMs * 1000 : 0);
        System.out.println();
    }

    public void renderError(String message) {
        System.out.println();
        System.out.println(Ansi.ansi().fg(Color.RED).bold().a("Error: ").reset().a(message));
    }

    public void renderWarning(String message) {
        System.out.println(Ansi.ansi().fg(Color.YELLOW).a("Warning: ").reset().a(message));
    }

    public void renderInfo(String message) {
        System.out.println(Ansi.ansi().fg(Color.CYAN).a("Info: ").reset().a(message));
    }

    public void renderHeader(String title) {
        System.out.println();
        System.out.println(Ansi.ansi().fg(Color.WHITE).bold().a(title).reset());
        System.out.println("=".repeat(title.length()));
        System.out.println();
    }

    private String formatEta(long milliseconds) {
        if (milliseconds <= 0) {
            return "calculating...";
        }

        long seconds = milliseconds / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) {
            return String.format("%dm %ds", minutes, seconds);
        }

        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format("%dh %dm", hours, minutes);
    }

    private String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;

        if (seconds < 60) {
            return String.format("%d.%ds", seconds, (milliseconds % 1000) / 100);
        }

        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes < 60) {
            return String.format("%dm %ds", minutes, seconds);
        }

        long hours = minutes / 60;
        minutes = minutes % 60;
        return String.format("%dh %dm %ds", hours, minutes, seconds);
    }

    public void clearLine() {
        System.out.print("\r" + " ".repeat(120) + "\r");
        System.out.flush();
    }

    public void newLine() {
        System.out.println();
    }
}

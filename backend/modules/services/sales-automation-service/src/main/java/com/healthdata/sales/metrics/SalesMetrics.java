package com.healthdata.sales.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom metrics for the Sales Automation Service.
 * Exposes business metrics to Prometheus/Micrometer.
 */
@Component
@Getter
public class SalesMetrics {

    private final Counter leadsCreated;
    private final Counter leadsConverted;
    private final Counter opportunitiesWon;
    private final Counter opportunitiesLost;
    private final Counter emailsSent;
    private final Counter emailsOpened;
    private final Counter emailsClicked;
    private final Counter emailsBounced;
    private final Counter zohoSyncSuccess;
    private final Counter zohoSyncFailure;

    private final Timer leadCaptureTimer;
    private final Timer leadConversionTimer;
    private final Timer zohoSyncTimer;

    private final AtomicLong activePipelineValue;
    private final AtomicLong activeLeadsCount;
    private final AtomicLong activeEnrollmentsCount;

    public SalesMetrics(MeterRegistry registry) {
        // Counters for lead operations
        this.leadsCreated = Counter.builder("sales.leads.created")
            .description("Number of leads created")
            .register(registry);

        this.leadsConverted = Counter.builder("sales.leads.converted")
            .description("Number of leads converted to opportunities")
            .register(registry);

        // Counters for opportunity outcomes
        this.opportunitiesWon = Counter.builder("sales.opportunities.won")
            .description("Number of opportunities closed won")
            .register(registry);

        this.opportunitiesLost = Counter.builder("sales.opportunities.lost")
            .description("Number of opportunities closed lost")
            .register(registry);

        // Counters for email engagement
        this.emailsSent = Counter.builder("sales.emails.sent")
            .description("Number of emails sent")
            .register(registry);

        this.emailsOpened = Counter.builder("sales.emails.opened")
            .description("Number of emails opened")
            .register(registry);

        this.emailsClicked = Counter.builder("sales.emails.clicked")
            .description("Number of email links clicked")
            .register(registry);

        this.emailsBounced = Counter.builder("sales.emails.bounced")
            .description("Number of emails bounced")
            .register(registry);

        // Counters for Zoho sync
        this.zohoSyncSuccess = Counter.builder("sales.zoho.sync.success")
            .description("Number of successful Zoho syncs")
            .register(registry);

        this.zohoSyncFailure = Counter.builder("sales.zoho.sync.failure")
            .description("Number of failed Zoho syncs")
            .register(registry);

        // Timers for operation latency
        this.leadCaptureTimer = Timer.builder("sales.leads.capture.duration")
            .description("Time taken to capture a lead")
            .register(registry);

        this.leadConversionTimer = Timer.builder("sales.leads.conversion.duration")
            .description("Time taken to convert a lead")
            .register(registry);

        this.zohoSyncTimer = Timer.builder("sales.zoho.sync.duration")
            .description("Time taken for Zoho sync operations")
            .register(registry);

        // Gauges for current state
        this.activePipelineValue = new AtomicLong(0);
        Gauge.builder("sales.pipeline.value", activePipelineValue, AtomicLong::get)
            .description("Total value of active pipeline")
            .register(registry);

        this.activeLeadsCount = new AtomicLong(0);
        Gauge.builder("sales.leads.active", activeLeadsCount, AtomicLong::get)
            .description("Number of active leads")
            .register(registry);

        this.activeEnrollmentsCount = new AtomicLong(0);
        Gauge.builder("sales.sequences.active_enrollments", activeEnrollmentsCount, AtomicLong::get)
            .description("Number of active sequence enrollments")
            .register(registry);
    }

    // Helper methods for incrementing counters
    public void incrementLeadsCreated() {
        leadsCreated.increment();
    }

    public void incrementLeadsConverted() {
        leadsConverted.increment();
    }

    public void incrementOpportunitiesWon() {
        opportunitiesWon.increment();
    }

    public void incrementOpportunitiesLost() {
        opportunitiesLost.increment();
    }

    public void incrementEmailsSent() {
        emailsSent.increment();
    }

    public void incrementEmailsOpened() {
        emailsOpened.increment();
    }

    public void incrementEmailsClicked() {
        emailsClicked.increment();
    }

    public void incrementEmailsBounced() {
        emailsBounced.increment();
    }

    public void incrementZohoSyncSuccess() {
        zohoSyncSuccess.increment();
    }

    public void incrementZohoSyncFailure() {
        zohoSyncFailure.increment();
    }

    // Helper methods for updating gauges
    public void updateActivePipelineValue(long value) {
        activePipelineValue.set(value);
    }

    public void updateActiveLeadsCount(long count) {
        activeLeadsCount.set(count);
    }

    public void updateActiveEnrollmentsCount(long count) {
        activeEnrollmentsCount.set(count);
    }

    // Helper method for recording timed operations
    public Timer.Sample startTimer() {
        return Timer.start();
    }

    public void recordLeadCapture(Timer.Sample sample) {
        sample.stop(leadCaptureTimer);
    }

    public void recordLeadConversion(Timer.Sample sample) {
        sample.stop(leadConversionTimer);
    }

    public void recordZohoSync(Timer.Sample sample) {
        sample.stop(zohoSyncTimer);
    }
}

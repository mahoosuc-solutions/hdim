package com.healthdata.healthixadapter.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Adapter-specific Prometheus metrics for the Healthix adapter.
 *
 * Tracks FHIR notifications, CCDA documents, MPI queries,
 * HL7 messages/errors, and associated latencies with the
 * {@code adapter=healthix} tag.
 */
@Component
public class AdapterMetrics {

    private static final String TAG_ADAPTER = "adapter";
    private static final String ADAPTER_NAME = "healthix";

    private final Counter fhirNotificationsCounter;
    private final Counter ccdaDocumentsCounter;
    private final Counter mpiQueriesCounter;
    private final Counter hl7MessagesCounter;
    private final Counter hl7ErrorsCounter;
    private final Timer fhirLatencyTimer;
    private final Timer mpiLatencyTimer;
    private final Timer ccdaLatencyTimer;

    public AdapterMetrics(MeterRegistry registry) {
        this.fhirNotificationsCounter = Counter.builder("hdim.adapter.healthix.fhir.notifications.total")
                .description("Total FHIR subscription notifications received from Healthix")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.ccdaDocumentsCounter = Counter.builder("hdim.adapter.healthix.ccda.documents.total")
                .description("Total CCDA documents processed from Healthix")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.mpiQueriesCounter = Counter.builder("hdim.adapter.healthix.mpi.queries.total")
                .description("Total MPI patient-matching queries sent to Healthix")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.hl7MessagesCounter = Counter.builder("hdim.adapter.healthix.hl7.messages.total")
                .description("Total HL7 v2.x messages processed from Healthix")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.hl7ErrorsCounter = Counter.builder("hdim.adapter.healthix.hl7.errors.total")
                .description("Total HL7 v2.x message processing errors")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.fhirLatencyTimer = Timer.builder("hdim.adapter.healthix.fhir.latency")
                .description("Latency of Healthix FHIR operations")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.mpiLatencyTimer = Timer.builder("hdim.adapter.healthix.mpi.latency")
                .description("Latency of Healthix MPI queries")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);

        this.ccdaLatencyTimer = Timer.builder("hdim.adapter.healthix.ccda.latency")
                .description("Latency of Healthix CCDA document processing")
                .tag(TAG_ADAPTER, ADAPTER_NAME)
                .register(registry);
    }

    public void recordFhirNotification() {
        fhirNotificationsCounter.increment();
    }

    public void recordCcdaDocument() {
        ccdaDocumentsCounter.increment();
    }

    public void recordMpiQuery() {
        mpiQueriesCounter.increment();
    }

    public void recordHl7Message() {
        hl7MessagesCounter.increment();
    }

    public void recordHl7Error() {
        hl7ErrorsCounter.increment();
    }

    public void recordFhirLatency(Duration duration) {
        fhirLatencyTimer.record(duration);
    }

    public void recordMpiLatency(Duration duration) {
        mpiLatencyTimer.record(duration);
    }

    public void recordCcdaLatency(Duration duration) {
        ccdaLatencyTimer.record(duration);
    }
}

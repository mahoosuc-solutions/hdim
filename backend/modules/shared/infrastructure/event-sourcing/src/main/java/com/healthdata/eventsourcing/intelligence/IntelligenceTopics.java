package com.healthdata.eventsourcing.intelligence;

/**
 * Topic taxonomy for intelligence pipeline event flow.
 */
public final class IntelligenceTopics {

    private IntelligenceTopics() {
        // Constants holder
    }

    public static final String INGEST_RAW = "ingest.raw";
    public static final String INGEST_NORMALIZED = "ingest.normalized";
    public static final String VALIDATION_FINDINGS = "validation.findings";
    public static final String INTELLIGENCE_SIGNALS = "intelligence.signals";
    public static final String RECOMMENDATIONS_GENERATED = "recommendations.generated";
    public static final String RECOMMENDATIONS_REVIEWED = "recommendations.reviewed";
}

package com.healthdata.eventsourcing.intelligence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IntelligenceTopics Tests")
class IntelligenceTopicsTest {

    @Test
    @DisplayName("topic constants should match taxonomy")
    void topicConstantsShouldMatchTaxonomy() {
        assertThat(IntelligenceTopics.INGEST_RAW).isEqualTo("ingest.raw");
        assertThat(IntelligenceTopics.INGEST_NORMALIZED).isEqualTo("ingest.normalized");
        assertThat(IntelligenceTopics.VALIDATION_FINDINGS).isEqualTo("validation.findings");
        assertThat(IntelligenceTopics.INTELLIGENCE_SIGNALS).isEqualTo("intelligence.signals");
        assertThat(IntelligenceTopics.RECOMMENDATIONS_GENERATED).isEqualTo("recommendations.generated");
        assertThat(IntelligenceTopics.RECOMMENDATIONS_REVIEWED).isEqualTo("recommendations.reviewed");
    }
}

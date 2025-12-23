package com.healthdata.predictive.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatientFeaturesTest {

    @Test
    void shouldReturnZeroWhenFeatureVectorMissing() {
        PatientFeatures features = new PatientFeatures();

        assertThat(features.getFeatureVectorDimension()).isEqualTo(0);
    }

    @Test
    void shouldReturnFeatureVectorLength() {
        PatientFeatures features = PatientFeatures.builder()
            .featureVector(new double[] { 1.0, 2.0, 3.0 })
            .build();

        assertThat(features.getFeatureVectorDimension()).isEqualTo(3);
    }
}

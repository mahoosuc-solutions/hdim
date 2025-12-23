package com.healthdata.predictive.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class ReadmissionRiskModelTest {

    @Test
    void shouldThrowForNullOrEmptyFeatures() {
        ReadmissionRiskModel model = new ReadmissionRiskModel();

        assertThatThrownBy(() -> model.predict(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Features cannot be null or empty");
        assertThatThrownBy(() -> model.predict(new double[0]))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldClampProbabilityToOne() {
        ReadmissionRiskModel model = new ReadmissionRiskModel();
        double[] features = new double[17];
        features[0] = 80.0;  // age
        features[2] = 10.0;  // CCI
        features[4] = 5.0;   // hospitalizations
        features[5] = 6.0;   // ED visits
        features[7] = 12.0;  // meds
        features[15] = 10.0; // social risk
        features[16] = 8.0;  // length of stay

        double probability = model.predict(features);

        assertThat(probability).isEqualTo(1.0);
    }

    @Test
    void shouldApplyAgeAndLengthOfStayBands() {
        ReadmissionRiskModel model = new ReadmissionRiskModel();
        double[] features = new double[17];
        features[0] = 67.0;  // age >= 65
        features[2] = 1.0;   // CCI
        features[4] = 1.0;   // hospitalizations
        features[5] = 1.0;   // ED visits
        features[15] = 3.0;  // social risk
        features[16] = 4.0;  // length of stay >= 4

        double probability = model.predict(features);

        assertThat(probability).isCloseTo(0.58, within(0.0001));
    }

    @Test
    void shouldApplyAgeFiftyBand() {
        ReadmissionRiskModel model = new ReadmissionRiskModel();
        double[] features = new double[1];
        features[0] = 55.0;

        double probability = model.predict(features);

        assertThat(probability).isCloseTo(0.20, within(0.0001));
    }

    @Test
    void shouldReturnFeatureImportanceCopyAndMetadata() {
        ReadmissionRiskModel model = new ReadmissionRiskModel();

        Map<String, Double> importance = model.getFeatureImportance();
        importance.put("age", 999.0);

        assertThat(model.getFeatureImportance().get("age")).isEqualTo(0.12);
        assertThat(model.getConfidence()).isEqualTo(0.80);
        assertThat(model.getModelVersion()).isEqualTo("v1.0.0");
    }
}

package com.healthdata.predictive.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CostPredictionModelTest {

    @Test
    void shouldPredictCostsWithFeatureOverrides() {
        CostPredictionModel model = new CostPredictionModel();
        double[] features = new double[8];
        features[2] = 3.0; // CCI
        features[4] = 2.0; // Hospitalizations
        features[5] = 1.0; // ED visits
        features[6] = 4.0; // Outpatient visits
        features[7] = 5.0; // Active meds

        double inpatient = model.predictInpatientCost(features);
        double outpatient = model.predictOutpatientCost(features);
        double pharmacy = model.predictPharmacyCost(features);
        double emergency = model.predictEmergencyCost(features);
        double lab = model.predictLabCost(features);
        double imaging = model.predictImagingCost(features);
        double other = model.predictOtherCost(features);

        assertThat(inpatient).isEqualTo(5000.0 + 2.0 * 8000.0 + 3.0 * 500.0);
        assertThat(outpatient).isEqualTo(2000.0 + 4.0 * 200.0);
        assertThat(pharmacy).isEqualTo(1500.0 + 5.0 * 150.0);
        assertThat(emergency).isEqualTo(500.0 + 1.0 * 1200.0);
        assertThat(lab).isEqualTo(800.0);
        assertThat(imaging).isEqualTo(600.0);
        assertThat(other).isEqualTo(400.0);

        assertThat(model.predictTotalCost(features))
            .isEqualTo(inpatient + outpatient + pharmacy + emergency + lab + imaging + other);
    }

    @Test
    void shouldReturnStaticCostsWhenFeaturesMissing() {
        CostPredictionModel model = new CostPredictionModel();
        double[] features = new double[1];

        assertThat(model.predictInpatientCost(features)).isEqualTo(5000.0);
        assertThat(model.predictOutpatientCost(features)).isEqualTo(2000.0);
        assertThat(model.predictPharmacyCost(features)).isEqualTo(1500.0);
        assertThat(model.predictEmergencyCost(features)).isEqualTo(500.0);
    }

    @Test
    void shouldReturnModelMetadata() {
        CostPredictionModel model = new CostPredictionModel();

        assertThat(model.getConfidence()).isEqualTo(0.75);
        assertThat(model.getModelVersion()).isEqualTo("v1.0.0");
    }
}

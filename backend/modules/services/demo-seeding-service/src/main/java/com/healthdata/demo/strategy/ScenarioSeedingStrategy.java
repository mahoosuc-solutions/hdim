package com.healthdata.demo.strategy;

import org.hl7.fhir.r4.model.Bundle;

/**
 * Strategy interface for demo scenario seeding.
 *
 * Different demo scenarios require different patient population characteristics:
 * - HEDIS Evaluation: Large patient population (5K) with specific care gap distribution (28%)
 * - Patient Journey: Small cohort (1K) with detailed clinical histories and 4 named personas
 * - Risk Stratification: Large population (10K) with HCC score distribution across risk tiers
 * - Multi-Tenant: Medium population (3K) distributed across multiple tenants for isolation testing
 *
 * Each strategy defines the specific patient generation parameters and validation criteria
 * for its scenario.
 */
public interface ScenarioSeedingStrategy {

    /**
     * Seed a specific demo scenario.
     *
     * @param tenantId The tenant ID to seed data for
     * @return SeedingResult containing statistics and validation status
     */
    SeedingResult seedScenario(String tenantId);

    /**
     * Get the name of this scenario.
     *
     * @return Scenario name (e.g., "hedis-evaluation", "patient-journey")
     */
    String getScenarioName();

    /**
     * Get a description of this scenario.
     *
     * @return Human-readable description
     */
    String getScenarioDescription();

    /**
     * Get the expected number of patients for this scenario.
     *
     * @return Patient count
     */
    int getExpectedPatientCount();

    /**
     * Result of seeding operation.
     */
    class SeedingResult {
        private final String scenarioName;
        private final int patientsCreated;
        private final int observationsCreated;
        private final int proceduresCreated;
        private final int encountersCreated;
        private final int medicationsCreated;
        private final int conditionsCreated;
        private final int careGapsExpected;
        private final long durationMs;
        private final boolean success;
        private final String errorMessage;

        public SeedingResult(String scenarioName, int patientsCreated,
                             int observationsCreated, int proceduresCreated,
                             int encountersCreated, int medicationsCreated,
                             int conditionsCreated, int careGapsExpected,
                             long durationMs, boolean success, String errorMessage) {
            this.scenarioName = scenarioName;
            this.patientsCreated = patientsCreated;
            this.observationsCreated = observationsCreated;
            this.proceduresCreated = proceduresCreated;
            this.encountersCreated = encountersCreated;
            this.medicationsCreated = medicationsCreated;
            this.conditionsCreated = conditionsCreated;
            this.careGapsExpected = careGapsExpected;
            this.durationMs = durationMs;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getScenarioName() { return scenarioName; }
        public int getPatientsCreated() { return patientsCreated; }
        public int getObservationsCreated() { return observationsCreated; }
        public int getProceduresCreated() { return proceduresCreated; }
        public int getEncountersCreated() { return encountersCreated; }
        public int getMedicationsCreated() { return medicationsCreated; }
        public int getConditionsCreated() { return conditionsCreated; }
        public int getCareGapsExpected() { return careGapsExpected; }
        public long getDurationMs() { return durationMs; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }

        public static class Builder {
            private String scenarioName;
            private int patientsCreated;
            private int observationsCreated;
            private int proceduresCreated;
            private int encountersCreated;
            private int medicationsCreated;
            private int conditionsCreated;
            private int careGapsExpected;
            private long durationMs;
            private boolean success = true;
            private String errorMessage;

            public Builder scenarioName(String scenarioName) {
                this.scenarioName = scenarioName;
                return this;
            }

            public Builder patientsCreated(int patientsCreated) {
                this.patientsCreated = patientsCreated;
                return this;
            }

            public Builder observationsCreated(int observationsCreated) {
                this.observationsCreated = observationsCreated;
                return this;
            }

            public Builder proceduresCreated(int proceduresCreated) {
                this.proceduresCreated = proceduresCreated;
                return this;
            }

            public Builder encountersCreated(int encountersCreated) {
                this.encountersCreated = encountersCreated;
                return this;
            }

            public Builder medicationsCreated(int medicationsCreated) {
                this.medicationsCreated = medicationsCreated;
                return this;
            }

            public Builder conditionsCreated(int conditionsCreated) {
                this.conditionsCreated = conditionsCreated;
                return this;
            }

            public Builder careGapsExpected(int careGapsExpected) {
                this.careGapsExpected = careGapsExpected;
                return this;
            }

            public Builder durationMs(long durationMs) {
                this.durationMs = durationMs;
                return this;
            }

            public Builder success(boolean success) {
                this.success = success;
                return this;
            }

            public Builder errorMessage(String errorMessage) {
                this.errorMessage = errorMessage;
                return this;
            }

            public SeedingResult build() {
                return new SeedingResult(
                    scenarioName, patientsCreated, observationsCreated,
                    proceduresCreated, encountersCreated, medicationsCreated,
                    conditionsCreated, careGapsExpected, durationMs,
                    success, errorMessage
                );
            }
        }

        @Override
        public String toString() {
            return String.format(
                "SeedingResult{scenario='%s', patients=%d, observations=%d, procedures=%d, " +
                "encounters=%d, medications=%d, conditions=%d, careGaps=%d, duration=%dms, success=%s}",
                scenarioName, patientsCreated, observationsCreated, proceduresCreated,
                encountersCreated, medicationsCreated, conditionsCreated, careGapsExpected,
                durationMs, success
            );
        }
    }
}

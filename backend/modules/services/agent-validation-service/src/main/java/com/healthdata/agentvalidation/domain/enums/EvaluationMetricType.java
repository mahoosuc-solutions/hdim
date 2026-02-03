package com.healthdata.agentvalidation.domain.enums;

/**
 * Types of evaluation metrics for agent response assessment.
 * These map to DeepEval metrics and custom healthcare-specific metrics.
 */
public enum EvaluationMetricType {

    // Core DeepEval metrics
    RELEVANCY("Measures if the response addresses the query", true),
    FAITHFULNESS("Measures if the response is grounded in context", true),
    HALLUCINATION("Detects fabricated or unsupported information", true),
    ANSWER_RELEVANCY("Measures overall answer quality", true),
    CONTEXTUAL_PRECISION("Measures precision of context usage", true),
    CONTEXTUAL_RECALL("Measures recall of relevant context", true),
    COHERENCE("Measures logical flow and clarity", true),
    BIAS("Detects potential biases in response", true),
    TOXICITY("Detects harmful or inappropriate content", true),

    // Healthcare-specific custom metrics
    HIPAA_COMPLIANCE("Validates HIPAA compliance for PHI handling", false),
    CLINICAL_ACCURACY("Validates clinical information accuracy", false),
    CLINICAL_SAFETY("Checks for harmful clinical recommendations", false),
    MEDICAL_TERMINOLOGY("Validates proper use of medical terms", false),
    CARE_GAP_RELEVANCE("Measures relevance to care gap context", false),
    HEDIS_COMPLIANCE("Validates HEDIS measure accuracy", false),

    // Meta metrics (calculated from other metrics)
    OVERALL_QUALITY("Composite quality score", true),
    CONFIDENCE_CALIBRATION("Self-assessed vs actual accuracy", false);

    private final String description;
    private final boolean deepEvalNative;

    EvaluationMetricType(String description, boolean deepEvalNative) {
        this.description = description;
        this.deepEvalNative = deepEvalNative;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDeepEvalNative() {
        return deepEvalNative;
    }
}

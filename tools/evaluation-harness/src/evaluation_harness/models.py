"""Pydantic models for evaluation request/response contracts."""

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class MetricType(str, Enum):
    """Evaluation metric types matching Java EvaluationMetricType enum."""

    # DeepEval Native Metrics
    RELEVANCY = "RELEVANCY"
    FAITHFULNESS = "FAITHFULNESS"
    HALLUCINATION = "HALLUCINATION"
    ANSWER_RELEVANCY = "ANSWER_RELEVANCY"
    CONTEXTUAL_PRECISION = "CONTEXTUAL_PRECISION"
    CONTEXTUAL_RECALL = "CONTEXTUAL_RECALL"
    COHERENCE = "COHERENCE"
    BIAS = "BIAS"
    TOXICITY = "TOXICITY"

    # Healthcare-Specific Custom Metrics
    HIPAA_COMPLIANCE = "HIPAA_COMPLIANCE"
    CLINICAL_ACCURACY = "CLINICAL_ACCURACY"
    CLINICAL_SAFETY = "CLINICAL_SAFETY"
    MEDICAL_TERMINOLOGY = "MEDICAL_TERMINOLOGY"
    CARE_GAP_RELEVANCE = "CARE_GAP_RELEVANCE"
    HEDIS_COMPLIANCE = "HEDIS_COMPLIANCE"

    # Meta Metrics
    OVERALL_QUALITY = "OVERALL_QUALITY"
    CONFIDENCE_CALIBRATION = "CONFIDENCE_CALIBRATION"


class EvaluationRequest(BaseModel):
    """Request payload for evaluation endpoint."""

    metric_type: MetricType = Field(
        ..., alias="metricType", description="The type of metric to evaluate"
    )
    user_message: str = Field(
        ..., alias="userMessage", description="The original user query/prompt"
    )
    agent_response: str = Field(
        ..., alias="agentResponse", description="The agent's response to evaluate"
    )
    context_data: dict[str, Any] = Field(
        default_factory=dict,
        alias="contextData",
        description="Additional context (patient ID, tenant, conditions, etc.)",
    )

    class Config:
        populate_by_name = True


class EvaluationResponse(BaseModel):
    """Response payload from evaluation endpoint."""

    metric_type: MetricType = Field(..., alias="metricType")
    score: float = Field(..., ge=0.0, le=1.0, description="Evaluation score (0.0-1.0)")
    reason: str = Field(..., description="Explanation for the score")
    metadata: dict[str, Any] = Field(
        default_factory=dict, description="Additional metric-specific data"
    )

    class Config:
        populate_by_name = True


class HealthResponse(BaseModel):
    """Health check response."""

    status: str = "UP"
    version: str
    metrics_available: list[str]


class BatchEvaluationRequest(BaseModel):
    """Request for evaluating multiple metrics at once."""

    user_message: str = Field(..., alias="userMessage")
    agent_response: str = Field(..., alias="agentResponse")
    context_data: dict[str, Any] = Field(default_factory=dict, alias="contextData")
    metric_types: list[MetricType] = Field(..., alias="metricTypes")

    class Config:
        populate_by_name = True


class BatchEvaluationResponse(BaseModel):
    """Response containing multiple metric evaluations."""

    results: list[EvaluationResponse]
    overall_score: float = Field(..., alias="overallScore")

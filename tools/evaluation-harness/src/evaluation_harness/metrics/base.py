"""Base class for metric evaluators."""

from abc import ABC, abstractmethod
from typing import Any

from ..models import EvaluationResponse, MetricType


class MetricEvaluator(ABC):
    """Abstract base class for all metric evaluators."""

    @property
    @abstractmethod
    def metric_type(self) -> MetricType:
        """Return the metric type this evaluator handles."""
        ...

    @property
    def description(self) -> str:
        """Return a description of what this metric evaluates."""
        return f"Evaluates {self.metric_type.value}"

    @abstractmethod
    async def evaluate(
        self,
        user_message: str,
        agent_response: str,
        context_data: dict[str, Any],
    ) -> EvaluationResponse:
        """
        Evaluate the agent response against this metric.

        Args:
            user_message: The original user query/prompt
            agent_response: The agent's response to evaluate
            context_data: Additional context (patient ID, conditions, etc.)

        Returns:
            EvaluationResponse with score, reason, and metadata
        """
        ...

    def _build_response(
        self,
        score: float,
        reason: str,
        metadata: dict[str, Any] | None = None,
    ) -> EvaluationResponse:
        """Helper to build a standardized response."""
        return EvaluationResponse(
            metricType=self.metric_type,
            score=max(0.0, min(1.0, score)),  # Clamp to [0, 1]
            reason=reason,
            metadata=metadata or {},
        )

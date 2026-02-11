"""DeepEval-based metric evaluators for native metrics."""

import asyncio
from typing import Any

import structlog

from ..config import get_settings
from ..models import EvaluationResponse, MetricType
from .base import MetricEvaluator

logger = structlog.get_logger()


class DeepEvalMetricEvaluator(MetricEvaluator):
    """Evaluator using DeepEval library for native metrics."""

    NATIVE_METRICS = {
        MetricType.RELEVANCY,
        MetricType.FAITHFULNESS,
        MetricType.HALLUCINATION,
        MetricType.ANSWER_RELEVANCY,
        MetricType.CONTEXTUAL_PRECISION,
        MetricType.CONTEXTUAL_RECALL,
        MetricType.COHERENCE,
        MetricType.BIAS,
        MetricType.TOXICITY,
    }

    def __init__(self, metric_type: MetricType):
        if metric_type not in self.NATIVE_METRICS:
            raise ValueError(f"{metric_type} is not a native DeepEval metric")
        self._metric_type = metric_type
        self._settings = get_settings()

    @property
    def metric_type(self) -> MetricType:
        return self._metric_type

    async def run_evaluation(
        self,
        user_message: str,
        agent_response: str,
        context_data: dict[str, Any],
    ) -> EvaluationResponse:
        """Run evaluation using DeepEval metrics."""
        try:
            # Run DeepEval in thread pool to not block async
            result = await asyncio.get_event_loop().run_in_executor(
                None,
                self._run_deepeval,
                user_message,
                agent_response,
                context_data,
            )
            return result
        except Exception as e:
            logger.error(
                "DeepEval failed, using fallback",
                metric=self._metric_type.value,
                error=str(e),
            )
            return self._fallback_scoring(user_message, agent_response, context_data)

    # Alias for interface compliance
    async def evaluate(
        self,
        user_message: str,
        agent_response: str,
        context_data: dict[str, Any],
    ) -> EvaluationResponse:
        """Evaluate the agent response against this metric."""
        return await self.run_evaluation(user_message, agent_response, context_data)

    def _run_deepeval(
        self,
        user_message: str,
        agent_response: str,
        context_data: dict[str, Any],
    ) -> EvaluationResponse:
        """Run DeepEval metric synchronously."""
        # Import DeepEval components
        from deepeval.metrics import (
            AnswerRelevancyMetric,
            BiasMetric,
            ContextualPrecisionMetric,
            ContextualRecallMetric,
            FaithfulnessMetric,
            HallucinationMetric,
            ToxicityMetric,
        )
        from deepeval.test_case import LLMTestCase

        # Build retrieval context from context_data if available
        retrieval_context = self._extract_retrieval_context(context_data)

        # Create test case
        test_case = LLMTestCase(
            input=user_message,
            actual_output=agent_response,
            retrieval_context=retrieval_context if retrieval_context else None,
            context=retrieval_context if retrieval_context else None,
        )

        # Get the appropriate metric
        metric = self._get_deepeval_metric()

        # Run measurement
        try:
            metric.measure(test_case)
            score = metric.score if metric.score is not None else 0.0
            reason = metric.reason if hasattr(metric, "reason") and metric.reason else ""

            return self._build_response(
                score=score,
                reason=reason or f"{self._metric_type.value} completed",
                metadata={
                    "deepeval_score": score,
                    "metric_class": type(metric).__name__,
                },
            )
        except Exception as e:
            logger.warning(
                "DeepEval metric measurement failed",
                metric=self._metric_type.value,
                error=str(e),
            )
            raise

    def _get_deepeval_metric(self):
        """Get the appropriate DeepEval metric instance."""
        from deepeval.metrics import (
            AnswerRelevancyMetric,
            BiasMetric,
            ContextualPrecisionMetric,
            ContextualRecallMetric,
            FaithfulnessMetric,
            HallucinationMetric,
            ToxicityMetric,
        )

        model = self._settings.openai_model

        metric_map = {
            MetricType.RELEVANCY: lambda: AnswerRelevancyMetric(model=model),
            MetricType.ANSWER_RELEVANCY: lambda: AnswerRelevancyMetric(model=model),
            MetricType.FAITHFULNESS: lambda: FaithfulnessMetric(model=model),
            MetricType.HALLUCINATION: lambda: HallucinationMetric(model=model),
            MetricType.CONTEXTUAL_PRECISION: lambda: ContextualPrecisionMetric(model=model),
            MetricType.CONTEXTUAL_RECALL: lambda: ContextualRecallMetric(model=model),
            MetricType.BIAS: lambda: BiasMetric(model=model),
            MetricType.TOXICITY: lambda: ToxicityMetric(model=model),
        }

        if self._metric_type in metric_map:
            return metric_map[self._metric_type]()

        # Coherence doesn't have a direct DeepEval metric, use Answer Relevancy as proxy
        if self._metric_type == MetricType.COHERENCE:
            return AnswerRelevancyMetric(model=model)

        raise ValueError(f"No DeepEval metric for {self._metric_type}")

    def _extract_retrieval_context(self, context_data: dict[str, Any]) -> list[str]:
        """Extract retrieval context from context_data for RAG scoring."""
        context_items = []

        # Extract any text-based context
        for key in ["patientSummary", "clinicalNotes", "careGaps", "measures", "context"]:
            if key in context_data:
                value = context_data[key]
                if isinstance(value, str):
                    context_items.append(value)
                elif isinstance(value, list):
                    context_items.extend(str(item) for item in value)

        # If conditions are present, add them as context
        if "conditions" in context_data:
            conditions = context_data["conditions"]
            if isinstance(conditions, list):
                context_items.append(f"Patient conditions: {', '.join(conditions)}")

        return context_items

    def _fallback_scoring(
        self,
        user_message: str,
        agent_response: str,
        context_data: dict[str, Any],
    ) -> EvaluationResponse:
        """Fallback heuristic scoring when DeepEval fails."""
        score = 0.5
        reason = "Fallback heuristic scoring"
        metadata: dict[str, Any] = {"fallback": True}

        if self._metric_type in (MetricType.RELEVANCY, MetricType.ANSWER_RELEVANCY):
            # Keyword overlap scoring
            user_words = set(user_message.lower().split())
            response_words = set(agent_response.lower().split())
            overlap = len(user_words & response_words)
            score = min(0.95, 0.50 + (overlap * 0.05))
            reason = f"Keyword overlap: {overlap} matching words"
            metadata["keyword_overlap"] = overlap

        elif self._metric_type == MetricType.COHERENCE:
            # Structure indicators
            sentences = agent_response.count(".") + agent_response.count("!")
            has_structure = "\n" in agent_response or sentences > 2
            length_ok = 50 < len(agent_response) < 5000
            score = 0.70 if (has_structure and length_ok) else 0.50
            reason = f"Structure check: {sentences} sentences, structured={has_structure}"
            metadata["sentence_count"] = sentences

        elif self._metric_type == MetricType.TOXICITY:
            # Simple toxic keyword detection
            toxic_keywords = ["kill", "harm", "die", "hate", "stupid", "idiot"]
            response_lower = agent_response.lower()
            found_toxic = [kw for kw in toxic_keywords if kw in response_lower]
            score = 0.30 if found_toxic else 0.95
            reason = f"Toxic keywords found: {found_toxic}" if found_toxic else "No toxic content detected"
            metadata["toxic_keywords"] = found_toxic

        elif self._metric_type == MetricType.HALLUCINATION:
            # Check for uncertainty markers (good) vs absolute claims (potentially bad)
            uncertainty_markers = ["may", "might", "could", "possibly", "appears", "suggests"]
            certainty_markers = ["definitely", "certainly", "absolutely", "always", "never"]
            response_lower = agent_response.lower()

            uncertainty_count = sum(1 for m in uncertainty_markers if m in response_lower)
            certainty_count = sum(1 for m in certainty_markers if m in response_lower)

            # More uncertainty = less likely hallucination
            if uncertainty_count > certainty_count:
                score = 0.85
            elif certainty_count > uncertainty_count:
                score = 0.60
            else:
                score = 0.70
            reason = f"Uncertainty markers: {uncertainty_count}, Certainty markers: {certainty_count}"
            metadata["uncertainty_count"] = uncertainty_count
            metadata["certainty_count"] = certainty_count

        elif self._metric_type == MetricType.FAITHFULNESS:
            # Check if response references context
            score = 0.70
            reason = "Faithfulness requires context comparison"

        else:
            score = 0.70
            reason = f"Generic fallback for {self._metric_type.value}"

        return self._build_response(score=score, reason=reason, metadata=metadata)

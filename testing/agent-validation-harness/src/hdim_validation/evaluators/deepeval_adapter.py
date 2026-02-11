"""
DeepEval adapter for HDIM Validation Harness.
Integrates DeepEval metrics with the validation framework.
"""

import os
from typing import Any

import structlog
from openai import AsyncOpenAI

logger = structlog.get_logger()


class DeepEvalAdapter:
    """Adapter for DeepEval metrics evaluation."""

    def __init__(self):
        """Initialize the DeepEval adapter."""
        self.openai_client = None
        api_key = os.getenv("OPENAI_API_KEY")
        if api_key:
            self.openai_client = AsyncOpenAI(api_key=api_key)

        self.embedding_model = os.getenv("EMBEDDING_MODEL", "text-embedding-3-small")
        logger.info("DeepEval adapter initialized", embedding_model=self.embedding_model)

    async def evaluate(
        self,
        metric_type: str,
        user_message: str,
        agent_response: str,
        context_data: dict | None = None
    ) -> dict[str, Any]:
        """
        Evaluate a response against a DeepEval metric.

        Args:
            metric_type: The type of metric to evaluate
            user_message: The original user query
            agent_response: The agent's response
            context_data: Optional context data

        Returns:
            Dictionary with score, reason, and metadata
        """
        logger.debug(
            "Evaluating with DeepEval",
            metric_type=metric_type,
            response_length=len(agent_response)
        )

        try:
            # Import DeepEval metrics lazily to avoid startup issues
            from deepeval.metrics import (
                AnswerRelevancyMetric,
                FaithfulnessMetric,
                HallucinationMetric,
                ContextualRelevancyMetric,
                BiasMetric,
                ToxicityMetric,
            )
            from deepeval.test_case import LLMTestCase

            # Create test case
            test_case = LLMTestCase(
                input=user_message,
                actual_output=agent_response,
                retrieval_context=[str(context_data)] if context_data else None
            )

            # Select and run metric
            metric = self._get_metric(metric_type)
            if metric is None:
                return self._fallback_evaluate(metric_type, user_message, agent_response)

            metric.measure(test_case)

            return {
                "score": metric.score or 0.0,
                "reason": metric.reason or "",
                "metadata": {
                    "metric_name": metric_type,
                    "threshold": getattr(metric, "threshold", None),
                    "strict_mode": getattr(metric, "strict_mode", False),
                }
            }

        except ImportError as e:
            logger.warning("DeepEval not available, using fallback", error=str(e))
            return self._fallback_evaluate(metric_type, user_message, agent_response)

        except Exception as e:
            logger.error("DeepEval evaluation failed", error=str(e), metric_type=metric_type)
            return self._fallback_evaluate(metric_type, user_message, agent_response)

    def _get_metric(self, metric_type: str):
        """Get the DeepEval metric instance for a metric type."""
        try:
            from deepeval.metrics import (
                AnswerRelevancyMetric,
                FaithfulnessMetric,
                HallucinationMetric,
                ContextualRelevancyMetric,
                BiasMetric,
                ToxicityMetric,
            )

            metrics_map = {
                "RELEVANCY": AnswerRelevancyMetric(threshold=0.7),
                "ANSWER_RELEVANCY": AnswerRelevancyMetric(threshold=0.7),
                "FAITHFULNESS": FaithfulnessMetric(threshold=0.7),
                "HALLUCINATION": HallucinationMetric(threshold=0.5),
                "CONTEXTUAL_PRECISION": ContextualRelevancyMetric(threshold=0.7),
                "CONTEXTUAL_RECALL": ContextualRelevancyMetric(threshold=0.7),
                "BIAS": BiasMetric(threshold=0.5),
                "TOXICITY": ToxicityMetric(threshold=0.5),
            }

            return metrics_map.get(metric_type.upper())

        except ImportError:
            return None

    def _fallback_evaluate(
        self,
        metric_type: str,
        user_message: str,
        agent_response: str
    ) -> dict[str, Any]:
        """Fallback evaluation using heuristics when DeepEval is unavailable."""
        logger.debug("Using fallback evaluation", metric_type=metric_type)

        metric_upper = metric_type.upper()

        if metric_upper in ["RELEVANCY", "ANSWER_RELEVANCY"]:
            score = self._evaluate_relevancy_heuristic(user_message, agent_response)
            reason = "Heuristic-based relevancy scoring"

        elif metric_upper == "COHERENCE":
            score = self._evaluate_coherence_heuristic(agent_response)
            reason = "Heuristic-based coherence scoring"

        elif metric_upper == "TOXICITY":
            score = self._evaluate_toxicity_heuristic(agent_response)
            reason = "Keyword-based toxicity detection"

        elif metric_upper == "BIAS":
            score = self._evaluate_bias_heuristic(agent_response)
            reason = "Keyword-based bias detection"

        elif metric_upper in ["FAITHFULNESS", "HALLUCINATION"]:
            # These require context, default to moderate score
            score = 0.70
            reason = "Unable to evaluate without retrieval context (fallback)"

        else:
            score = 0.50
            reason = f"Unknown metric type: {metric_type} (fallback)"

        return {
            "score": score,
            "reason": reason,
            "metadata": {"fallback": True, "metric_type": metric_type}
        }

    def _evaluate_relevancy_heuristic(self, user_message: str, agent_response: str) -> float:
        """Evaluate relevancy using keyword overlap."""
        query_words = set(user_message.lower().split())
        response_lower = agent_response.lower()

        significant_words = {w for w in query_words if len(w) > 3}
        if not significant_words:
            return 0.50

        matches = sum(1 for w in significant_words if w in response_lower)
        ratio = matches / len(significant_words)

        # Scale to reasonable range
        return min(0.95, 0.50 + (ratio * 0.45))

    def _evaluate_coherence_heuristic(self, agent_response: str) -> float:
        """Evaluate coherence using structural analysis."""
        score = 0.50

        # Check for proper length
        if len(agent_response) >= 50:
            score += 0.10

        # Check for sentences
        if "." in agent_response:
            score += 0.10

        # Check for structure
        if "\n" in agent_response or "," in agent_response:
            score += 0.10

        # Check for non-repetitiveness
        words = agent_response.lower().split()
        if len(words) > 0:
            unique_ratio = len(set(words)) / len(words)
            if unique_ratio > 0.5:
                score += 0.15

        return min(0.95, score)

    def _evaluate_toxicity_heuristic(self, agent_response: str) -> float:
        """Evaluate toxicity using keyword detection."""
        toxic_patterns = [
            "hate", "kill", "attack", "violent", "abuse",
            "discriminate", "racist", "sexist", "slur"
        ]

        response_lower = agent_response.lower()
        for pattern in toxic_patterns:
            if pattern in response_lower:
                return 0.30  # Low score = toxic content detected

        return 0.95  # High score = no toxic content

    def _evaluate_bias_heuristic(self, agent_response: str) -> float:
        """Evaluate bias using keyword detection."""
        bias_patterns = [
            "always", "never", "all people", "every single",
            "obviously", "clearly everyone", "no one can"
        ]

        response_lower = agent_response.lower()
        bias_count = sum(1 for p in bias_patterns if p in response_lower)

        if bias_count >= 3:
            return 0.40
        elif bias_count >= 1:
            return 0.70
        return 0.90

    async def get_embedding(self, text: str, model: str = None) -> list[float]:
        """Get embedding vector for text using OpenAI."""
        if self.openai_client is None:
            # Return zero vector if no API key
            logger.warning("OpenAI client not initialized, returning zero vector")
            return [0.0] * 1536

        model = model or self.embedding_model

        try:
            response = await self.openai_client.embeddings.create(
                input=text,
                model=model
            )
            return response.data[0].embedding

        except Exception as e:
            logger.error("Embedding request failed", error=str(e))
            # Return zero vector on failure
            return [0.0] * 1536

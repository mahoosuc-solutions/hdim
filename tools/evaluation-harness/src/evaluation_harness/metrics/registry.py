"""Registry for metric evaluators."""

from functools import lru_cache
from typing import Dict, Type

import structlog

from ..models import MetricType
from .base import MetricEvaluator
from .deepeval_metrics import DeepEvalMetricEvaluator

logger = structlog.get_logger()


class MetricRegistry:
    """Registry for all available metric evaluators."""

    def __init__(self):
        self._evaluators: Dict[MetricType, MetricEvaluator] = {}
        self._register_all_metrics()

    def _register_all_metrics(self):
        """Register all available metric evaluators."""
        # Register DeepEval native metrics
        for metric_type in DeepEvalMetricEvaluator.NATIVE_METRICS:
            try:
                self._evaluators[metric_type] = DeepEvalMetricEvaluator(metric_type)
                logger.debug("Registered DeepEval metric", metric=metric_type.value)
            except Exception as e:
                logger.warning(
                    "Failed to register DeepEval metric",
                    metric=metric_type.value,
                    error=str(e),
                )

        # Register healthcare-specific metrics
        self._register_healthcare_metrics()

    def _register_healthcare_metrics(self):
        """Register healthcare-specific custom metrics."""
        from ..healthcare import (
            CareGapRelevanceEvaluator,
            ClinicalAccuracyEvaluator,
            ClinicalSafetyEvaluator,
            HEDISComplianceEvaluator,
            HIPAAComplianceEvaluator,
            MedicalTerminologyEvaluator,
        )

        healthcare_evaluators = [
            HIPAAComplianceEvaluator(),
            ClinicalAccuracyEvaluator(),
            ClinicalSafetyEvaluator(),
            MedicalTerminologyEvaluator(),
            CareGapRelevanceEvaluator(),
            HEDISComplianceEvaluator(),
        ]

        for evaluator in healthcare_evaluators:
            self._evaluators[evaluator.metric_type] = evaluator
            logger.debug("Registered healthcare metric", metric=evaluator.metric_type.value)

    def get_evaluator(self, metric_type: MetricType) -> MetricEvaluator:
        """Get the evaluator for a specific metric type."""
        if metric_type not in self._evaluators:
            raise ValueError(f"No evaluator registered for metric: {metric_type.value}")
        return self._evaluators[metric_type]

    def has_evaluator(self, metric_type: MetricType) -> bool:
        """Check if an evaluator exists for the metric type."""
        return metric_type in self._evaluators

    def list_available_metrics(self) -> list[str]:
        """List all available metric types."""
        return [mt.value for mt in self._evaluators.keys()]

    def get_all_evaluators(self) -> Dict[MetricType, MetricEvaluator]:
        """Get all registered evaluators."""
        return self._evaluators.copy()


@lru_cache
def get_metric_registry() -> MetricRegistry:
    """Get the singleton metric registry instance."""
    return MetricRegistry()

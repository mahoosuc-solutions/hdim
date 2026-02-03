"""FastAPI application for the evaluation harness."""

import asyncio
import time
from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from prometheus_client import Counter, Histogram, generate_latest
from starlette.responses import Response

from . import __version__
from .config import get_settings
from .metrics import get_metric_registry
from .models import (
    BatchEvaluationRequest,
    BatchEvaluationResponse,
    EvaluationRequest,
    EvaluationResponse,
    HealthResponse,
    MetricType,
)

logger = structlog.get_logger()

# Prometheus metrics
EVALUATION_REQUESTS = Counter(
    "evaluation_requests_total",
    "Total evaluation requests",
    ["metric_type", "status"],
)
EVALUATION_DURATION = Histogram(
    "evaluation_duration_seconds",
    "Evaluation request duration",
    ["metric_type"],
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan handler."""
    settings = get_settings()
    logger.info(
        "Starting HDIM Evaluation Harness",
        version=__version__,
        port=settings.port,
        enabled_metrics=settings.enabled_metric_list,
    )

    # Initialize registry (warms up evaluators)
    registry = get_metric_registry()
    logger.info(
        "Metric registry initialized",
        available_metrics=registry.list_available_metrics(),
    )

    yield

    logger.info("Shutting down HDIM Evaluation Harness")


app = FastAPI(
    title="HDIM Evaluation Harness",
    description="DeepEval-based evaluation service for HDIM AI agent validation",
    version=__version__,
    lifespan=lifespan,
)

# CORS middleware for development
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.middleware("http")
async def log_requests(request: Request, call_next):
    """Log all incoming requests."""
    start_time = time.time()
    response = await call_next(request)
    duration = time.time() - start_time

    logger.info(
        "Request completed",
        method=request.method,
        path=request.url.path,
        status_code=response.status_code,
        duration_ms=round(duration * 1000, 2),
    )
    return response


@app.get("/health", response_model=HealthResponse, tags=["Health"])
async def health_check():
    """Health check endpoint for Kubernetes/Docker probes."""
    registry = get_metric_registry()
    return HealthResponse(
        status="UP",
        version=__version__,
        metrics_available=registry.list_available_metrics(),
    )


@app.get("/metrics", tags=["Monitoring"])
async def prometheus_metrics():
    """Prometheus metrics endpoint."""
    return Response(content=generate_latest(), media_type="text/plain")


@app.post("/evaluate", response_model=EvaluationResponse, tags=["Evaluation"])
async def run_evaluation(request: EvaluationRequest):
    """
    Evaluate an agent response against a specific metric.

    This is the main endpoint called by the agent-validation-service.
    """
    start_time = time.time()
    metric_type = request.metric_type

    try:
        registry = get_metric_registry()

        if not registry.has_evaluator(metric_type):
            EVALUATION_REQUESTS.labels(metric_type=metric_type.value, status="error").inc()
            raise HTTPException(
                status_code=400,
                detail=f"Unsupported metric type: {metric_type.value}",
            )

        evaluator = registry.get_evaluator(metric_type)

        # Run the evaluation
        result = await evaluator.evaluate(
            user_message=request.user_message,
            agent_response=request.agent_response,
            context_data=request.context_data,
        )

        duration = time.time() - start_time
        EVALUATION_REQUESTS.labels(metric_type=metric_type.value, status="success").inc()
        EVALUATION_DURATION.labels(metric_type=metric_type.value).observe(duration)

        logger.info(
            "Evaluation completed",
            metric=metric_type.value,
            score=result.score,
            duration_ms=round(duration * 1000, 2),
        )

        return result

    except HTTPException:
        raise
    except Exception as e:
        EVALUATION_REQUESTS.labels(metric_type=metric_type.value, status="error").inc()
        logger.error(
            "Evaluation failed",
            metric=metric_type.value,
            error=str(e),
            exc_info=True,
        )
        raise HTTPException(
            status_code=500,
            detail=f"Evaluation failed: {str(e)}",
        )


@app.post("/evaluate/batch", response_model=BatchEvaluationResponse, tags=["Evaluation"])
async def run_batch_evaluation(request: BatchEvaluationRequest):
    """
    Evaluate an agent response against multiple metrics in parallel.

    Returns individual results plus an overall score.
    """
    registry = get_metric_registry()
    results: list[EvaluationResponse] = []

    # Create evaluation tasks for each metric
    tasks = []
    for metric_type in request.metric_types:
        if registry.has_evaluator(metric_type):
            evaluator = registry.get_evaluator(metric_type)
            task = evaluator.evaluate(
                user_message=request.user_message,
                agent_response=request.agent_response,
                context_data=request.context_data,
            )
            tasks.append((metric_type, task))

    # Run all evaluations concurrently
    for metric_type, task in tasks:
        try:
            result = await task
            results.append(result)
        except Exception as e:
            logger.error(
                "Batch evaluation failed for metric",
                metric=metric_type.value,
                error=str(e),
            )
            # Add failed result
            results.append(
                EvaluationResponse(
                    metricType=metric_type,
                    score=0.0,
                    reason=f"Evaluation failed: {str(e)}",
                    metadata={"error": True},
                )
            )

    # Calculate overall score (weighted average - safety metrics have higher weight)
    if results:
        safety_metrics = {MetricType.CLINICAL_SAFETY, MetricType.HIPAA_COMPLIANCE}
        total_weight = 0.0
        weighted_sum = 0.0

        for result in results:
            weight = 2.0 if result.metric_type in safety_metrics else 1.0
            weighted_sum += result.score * weight
            total_weight += weight

        overall_score = weighted_sum / total_weight if total_weight > 0 else 0.0
    else:
        overall_score = 0.0

    return BatchEvaluationResponse(
        results=results,
        overallScore=round(overall_score, 4),
    )


@app.get("/metrics/available", tags=["Metadata"])
async def list_available_metrics():
    """List all available metrics and their descriptions."""
    registry = get_metric_registry()
    evaluators = registry.get_all_evaluators()

    return {
        "metrics": [
            {
                "type": mt.value,
                "description": evaluator.description,
                "is_native_deepeval": mt in DeepEvalMetricEvaluator.NATIVE_METRICS
                if hasattr(DeepEvalMetricEvaluator, "NATIVE_METRICS")
                else False,
            }
            for mt, evaluator in evaluators.items()
        ]
    }


# Import for the check above
from .metrics.deepeval_metrics import DeepEvalMetricEvaluator

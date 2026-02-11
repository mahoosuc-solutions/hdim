"""
Main entry point for the HDIM Validation Harness REST API.
"""

import os
from contextlib import asynccontextmanager

import structlog
import uvicorn
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

from hdim_validation.evaluators.deepeval_adapter import DeepEvalAdapter
from hdim_validation.evaluators.hipaa_evaluator import HIPAAComplianceEvaluator
from hdim_validation.evaluators.clinical_accuracy_evaluator import ClinicalAccuracyEvaluator

# Load environment variables
load_dotenv()

# Configure structured logging
structlog.configure(
    processors=[
        structlog.stdlib.filter_by_level,
        structlog.stdlib.add_logger_name,
        structlog.stdlib.add_log_level,
        structlog.stdlib.PositionalArgumentsFormatter(),
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,
        structlog.processors.UnicodeDecoder(),
        structlog.processors.JSONRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    context_class=dict,
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

logger = structlog.get_logger()

# Initialize evaluators
deepeval_adapter = None
hipaa_evaluator = None
clinical_evaluator = None


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize and cleanup resources."""
    global deepeval_adapter, hipaa_evaluator, clinical_evaluator

    logger.info("Initializing HDIM Validation Harness")

    deepeval_adapter = DeepEvalAdapter()
    hipaa_evaluator = HIPAAComplianceEvaluator()
    clinical_evaluator = ClinicalAccuracyEvaluator()

    logger.info("Validation Harness initialized successfully")

    yield

    logger.info("Shutting down Validation Harness")


app = FastAPI(
    title="HDIM Validation Harness",
    description="AI Agent Testing and Evaluation Framework",
    version="1.0.0",
    lifespan=lifespan
)


# Request/Response Models
class EvaluationRequest(BaseModel):
    """Request for metric evaluation."""
    metric_type: str
    user_message: str
    agent_response: str
    context_data: dict | None = None


class EvaluationResponse(BaseModel):
    """Response from metric evaluation."""
    metric_type: str
    score: float
    reason: str
    metadata: dict | None = None


class EmbeddingRequest(BaseModel):
    """Request for text embedding."""
    text: str
    model: str = "text-embedding-3-small"


class EmbeddingResponse(BaseModel):
    """Response with embedding vector."""
    embedding: list[float]
    model: str


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    version: str
    evaluators: dict


# Endpoints
@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    return HealthResponse(
        status="healthy",
        version="1.0.0",
        evaluators={
            "deepeval": deepeval_adapter is not None,
            "hipaa": hipaa_evaluator is not None,
            "clinical": clinical_evaluator is not None,
        }
    )


@app.post("/evaluate", response_model=EvaluationResponse)
async def evaluate_metric(request: EvaluationRequest):
    """Evaluate an agent response against a metric."""
    logger.info(
        "Evaluating metric",
        metric_type=request.metric_type,
        response_length=len(request.agent_response)
    )

    try:
        metric_type = request.metric_type.upper()

        # Route to appropriate evaluator
        if metric_type in ["HIPAA_COMPLIANCE"]:
            result = await hipaa_evaluator.evaluate(
                request.user_message,
                request.agent_response,
                request.context_data
            )
        elif metric_type in ["CLINICAL_ACCURACY", "CLINICAL_SAFETY"]:
            result = await clinical_evaluator.evaluate(
                request.user_message,
                request.agent_response,
                request.context_data
            )
        else:
            # DeepEval native metrics
            result = await deepeval_adapter.evaluate(
                metric_type,
                request.user_message,
                request.agent_response,
                request.context_data
            )

        logger.info(
            "Evaluation complete",
            metric_type=request.metric_type,
            score=result["score"]
        )

        return EvaluationResponse(
            metric_type=request.metric_type,
            score=result["score"],
            reason=result.get("reason", ""),
            metadata=result.get("metadata")
        )

    except Exception as e:
        logger.error("Evaluation failed", error=str(e), metric_type=request.metric_type)
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/embed", response_model=EmbeddingResponse)
async def get_embedding(request: EmbeddingRequest):
    """Get embedding vector for text."""
    logger.info("Getting embedding", text_length=len(request.text), model=request.model)

    try:
        embedding = await deepeval_adapter.get_embedding(request.text, request.model)

        return EmbeddingResponse(
            embedding=embedding,
            model=request.model
        )

    except Exception as e:
        logger.error("Embedding failed", error=str(e))
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/evaluate/batch")
async def evaluate_batch(requests: list[EvaluationRequest]):
    """Evaluate multiple metrics in batch."""
    results = []

    for request in requests:
        try:
            response = await evaluate_metric(request)
            results.append(response.model_dump())
        except Exception as e:
            results.append({
                "metric_type": request.metric_type,
                "score": 0.0,
                "reason": f"Error: {str(e)}",
                "metadata": {"error": True}
            })

    return results


def main():
    """Run the validation harness server."""
    port = int(os.getenv("VALIDATION_HARNESS_PORT", "8500"))
    host = os.getenv("VALIDATION_HARNESS_HOST", "0.0.0.0")

    logger.info("Starting HDIM Validation Harness", host=host, port=port)

    uvicorn.run(
        "hdim_validation.main:app",
        host=host,
        port=port,
        reload=os.getenv("VALIDATION_HARNESS_RELOAD", "false").lower() == "true"
    )


if __name__ == "__main__":
    main()

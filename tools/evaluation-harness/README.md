# HDIM Evaluation Harness

DeepEval-based evaluation service for HDIM AI agent validation. This service provides metric evaluation for the agent-validation-service.

## Features

- **DeepEval Integration**: Native support for standard LLM evaluation metrics
- **Healthcare-Specific Metrics**: Custom evaluators for HIPAA, clinical safety, and HEDIS compliance
- **Fallback Heuristics**: Graceful degradation when LLM-based evaluation is unavailable
- **Batch Evaluation**: Evaluate multiple metrics in parallel
- **Prometheus Metrics**: Built-in monitoring and observability

## Quick Start

### Prerequisites

- Python 3.10+
- OpenAI API key (optional but recommended for full DeepEval support)

### Installation

```bash
# Clone and navigate to the harness
cd tools/evaluation-harness

# Create virtual environment
python -m venv .venv
source .venv/bin/activate  # or `.venv\Scripts\activate` on Windows

# Install dependencies
pip install -e .

# Copy and configure environment
cp .env.example .env
# Edit .env with your OpenAI API key
```

### Running Locally

```bash
# Start the server
python -m evaluation_harness.main

# Or with uvicorn directly
uvicorn evaluation_harness.api:app --host 0.0.0.0 --port 8500 --reload
```

### Running with Docker

```bash
# Build and run
docker compose up -d

# View logs
docker compose logs -f
```

## API Endpoints

### Health Check
```bash
curl http://localhost:8500/health
```

### Evaluate Single Metric
```bash
curl -X POST http://localhost:8500/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "metricType": "RELEVANCY",
    "userMessage": "What are the care gaps for this diabetic patient?",
    "agentResponse": "Based on the patient records, the following care gaps have been identified: 1. HbA1c test overdue (last test 9 months ago), 2. Annual eye exam needed, 3. Kidney screening recommended.",
    "contextData": {
      "patientId": "patient-001",
      "conditions": ["Type 2 Diabetes"]
    }
  }'
```

### Batch Evaluation
```bash
curl -X POST http://localhost:8500/evaluate/batch \
  -H "Content-Type: application/json" \
  -d '{
    "userMessage": "What medications should this patient take?",
    "agentResponse": "I recommend consulting with your healthcare provider about medication options...",
    "contextData": {},
    "metricTypes": ["RELEVANCY", "CLINICAL_SAFETY", "HIPAA_COMPLIANCE"]
  }'
```

### List Available Metrics
```bash
curl http://localhost:8500/metrics/available
```

## Available Metrics

### DeepEval Native Metrics
| Metric | Description |
|--------|-------------|
| RELEVANCY | Response addresses the query |
| FAITHFULNESS | Response grounded in context |
| HALLUCINATION | Detects fabricated information |
| ANSWER_RELEVANCY | Overall answer quality |
| CONTEXTUAL_PRECISION | Precision of context usage |
| CONTEXTUAL_RECALL | Recall of relevant context |
| COHERENCE | Logical flow and clarity |
| BIAS | Detects potential biases |
| TOXICITY | Detects harmful content |

### Healthcare-Specific Metrics
| Metric | Description |
|--------|-------------|
| HIPAA_COMPLIANCE | Validates PHI handling, no PII leaks |
| CLINICAL_ACCURACY | Healthcare information accuracy |
| CLINICAL_SAFETY | Harmful clinical recommendations check |
| MEDICAL_TERMINOLOGY | Proper medical term usage |
| CARE_GAP_RELEVANCE | Relevance to patient care gaps |
| HEDIS_COMPLIANCE | HEDIS measure accuracy |

## Integration with Agent Validation Service

The agent-validation-service calls this harness at `http://localhost:8500/evaluate` for each metric evaluation. Configure the service connection in `application.yml`:

```yaml
hdim:
  validation:
    evaluation:
      harness-url: http://localhost:8500
      timeout-seconds: 60
```

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| EVAL_HOST | 0.0.0.0 | Server bind address |
| EVAL_PORT | 8500 | Server port |
| EVAL_WORKERS | 4 | Number of worker processes |
| EVAL_LOG_LEVEL | INFO | Logging level |
| EVAL_OPENAI_API_KEY | | OpenAI API key for DeepEval |
| EVAL_OPENAI_MODEL | gpt-4o-mini | Model for LLM-based evaluation |
| EVAL_HIPAA_STRICT_MODE | true | Zero tolerance for PHI leaks |
| EVAL_CLINICAL_SAFETY_ZERO_TOLERANCE | true | Zero tolerance for safety violations |

## Monitoring

Prometheus metrics available at `/metrics`:
- `evaluation_requests_total` - Total requests by metric type and status
- `evaluation_duration_seconds` - Request duration histogram

## Development

```bash
# Install dev dependencies
pip install -e ".[dev]"

# Run tests
pytest

# Format code
black src/ tests/
ruff check src/ tests/

# Type checking
mypy src/
```

## Architecture

```
evaluation-harness/
├── src/evaluation_harness/
│   ├── api.py              # FastAPI application
│   ├── config.py           # Configuration management
│   ├── models.py           # Pydantic models
│   ├── main.py             # Entry point
│   ├── metrics/            # Metric evaluators
│   │   ├── base.py         # Base evaluator class
│   │   ├── deepeval_metrics.py  # DeepEval integration
│   │   └── registry.py     # Metric registry
│   └── healthcare/         # Healthcare-specific metrics
│       ├── hipaa.py        # HIPAA compliance
│       ├── clinical.py     # Clinical accuracy/safety
│       └── hedis.py        # HEDIS/care gap metrics
├── tests/                  # Test suite
├── Dockerfile
├── docker-compose.yml
└── pyproject.toml
```

# Agent Testing Harness

A comprehensive testing framework for AI agents using **Langfuse** for observability and **DeepEval** for LLM evaluation.

## Quick Start

### 1. Start Langfuse (Self-Hosted)

```bash
# Start Langfuse stack (PostgreSQL, ClickHouse, Redis, MinIO, Langfuse)
docker compose -f docker-compose.langfuse.yml up -d

# Wait for services to be ready (~2-3 minutes)
docker compose -f docker-compose.langfuse.yml logs -f langfuse-web

# Access Langfuse UI
open http://localhost:3100
```

**First-time setup:**
1. Open http://localhost:3100
2. Create an account (sign-up is enabled)
3. Create a new project
4. Go to Settings > API Keys and create keys
5. Copy the public/secret keys to your `.env` file

### 2. Configure Environment

```bash
# Copy example config
cp .env.example .env

# Edit with your keys
nano .env
```

Required variables:
```env
LANGFUSE_PUBLIC_KEY=pk-lf-xxxxx
LANGFUSE_SECRET_KEY=sk-lf-xxxxx
ANTHROPIC_API_KEY=sk-ant-xxxxx
```

### 3. Install Dependencies

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # or `venv\Scripts\activate` on Windows

# Install dependencies
pip install -r requirements.txt
```

### 4. Run Tests

```bash
# Run all tests with verbose output
pytest -v

# Run specific test categories
pytest tests/test_llm_evaluation.py -v     # DeepEval LLM metrics
pytest tests/test_agent_harness.py -v       # Agent capability tests

# Run with Langfuse tracing (ensure .env is configured)
pytest -v --tb=short

# Generate HTML report
pytest --html=report.html --self-contained-html
```

## Test Categories

### LLM Evaluation Tests (`test_llm_evaluation.py`)

Uses DeepEval metrics to evaluate Claude's responses:

| Metric | Description | Threshold |
|--------|-------------|-----------|
| Answer Relevancy | Response relevance to question | 0.7 |
| Faithfulness | Adherence to provided context | 0.7 |
| Hallucination | Detection of fabricated facts | 0.5 |
| Bias | Demographic bias detection | 0.5 |
| Toxicity | Harmful content detection | 0.5 |

### Agent Harness Tests (`test_agent_harness.py`)

Tests agent capabilities based on Anthropic best practices:

- **Tool Use**: Correct tool selection and result handling
- **Multi-Turn**: Context retention across conversation turns
- **Error Recovery**: Graceful handling of tool errors
- **Long-Running Patterns**: Progress tracking, incremental work
- **Performance Metrics**: Latency, token efficiency

## Architecture

```
testing/agent-harness/
├── docker-compose.langfuse.yml   # Langfuse self-hosted stack
├── requirements.txt              # Python dependencies
├── conftest.py                   # Pytest configuration & fixtures
├── .env.example                  # Environment template
└── tests/
    ├── test_llm_evaluation.py    # DeepEval LLM metrics
    └── test_agent_harness.py     # Agent capability tests
```

## Langfuse Dashboard

After running tests, view traces at http://localhost:3100:

- **Traces**: Full conversation history with timing
- **Generations**: Individual LLM calls with tokens/cost
- **Scores**: Custom metrics (latency, token efficiency)
- **Sessions**: Group related traces together

## DeepEval Cloud (Optional)

For shared test reports and CI/CD integration:

```bash
# Login to DeepEval cloud
deepeval login

# Run tests with cloud reporting
deepeval test run tests/
```

## Integration with HDIM Services

Test against running HDIM backend services:

```bash
# Start HDIM services
cd ../..
docker compose --profile ai up -d

# Run integration tests
pytest tests/test_agent_harness.py -v -k "integration"
```

## Best Practices Applied

Based on [Anthropic's Effective Harnesses for Long-Running Agents](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents):

1. **Progress Tracking**: Agents output progress markers
2. **Incremental Work**: Tasks broken into manageable steps
3. **Context Bridging**: State preserved across sessions
4. **Explicit Testing**: Agents prompted to verify their work
5. **Tool Validation**: Comprehensive tool use testing

## Troubleshooting

### Langfuse not starting
```bash
# Check container logs
docker compose -f docker-compose.langfuse.yml logs langfuse-web

# Ensure all dependencies are healthy
docker compose -f docker-compose.langfuse.yml ps
```

### Tests failing to connect
```bash
# Verify Langfuse is accessible
curl http://localhost:3100/api/public/health

# Check your API keys are correct
cat .env | grep LANGFUSE
```

### DeepEval metrics failing
```bash
# DeepEval uses OpenAI for evaluation by default
# Ensure OPENAI_API_KEY is set for DeepEval metrics
export OPENAI_API_KEY=sk-xxx
```

## Resources

- [Langfuse Documentation](https://langfuse.com/docs)
- [DeepEval Documentation](https://docs.confident-ai.com/)
- [Anthropic Claude SDK](https://docs.anthropic.com/claude/reference/client-libraries)
- [Agent Testing Best Practices](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents)

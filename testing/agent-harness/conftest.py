"""
Agent Testing Harness - Pytest Configuration

This module sets up:
1. Langfuse tracing for all tests
2. Anthropic client with instrumentation
3. DeepEval integration
4. Common fixtures for agent testing
"""

import os
import pytest
from dotenv import load_dotenv

# Load environment variables
load_dotenv()


def pytest_configure(config):
    """Configure pytest with Langfuse and OpenTelemetry instrumentation."""
    # Initialize Langfuse
    from langfuse import Langfuse

    langfuse_host = os.getenv("LANGFUSE_HOST", "http://localhost:3100")
    public_key = os.getenv("LANGFUSE_PUBLIC_KEY")
    secret_key = os.getenv("LANGFUSE_SECRET_KEY")

    if public_key and secret_key:
        config.langfuse = Langfuse(
            host=langfuse_host,
            public_key=public_key,
            secret_key=secret_key,
        )
        print(f"\n[Langfuse] Connected to {langfuse_host}")
    else:
        config.langfuse = None
        print("\n[Langfuse] Not configured - traces will not be recorded")

    # Initialize OpenTelemetry instrumentation for Anthropic
    try:
        from opentelemetry.instrumentation.anthropic import AnthropicInstrumentor
        AnthropicInstrumentor().instrument()
        print("[OTEL] Anthropic instrumentation enabled")
    except ImportError:
        print("[OTEL] Anthropic instrumentation not available")


def pytest_unconfigure(config):
    """Cleanup on pytest exit."""
    if hasattr(config, "langfuse") and config.langfuse:
        config.langfuse.flush()


@pytest.fixture(scope="session")
def langfuse(request):
    """Provide Langfuse client for tracing."""
    return request.config.langfuse


@pytest.fixture(scope="session")
def anthropic_client():
    """Provide configured Anthropic client."""
    import anthropic

    api_key = os.getenv("ANTHROPIC_API_KEY")
    if not api_key or api_key.startswith("sk-ant-placeholder"):
        pytest.skip("ANTHROPIC_API_KEY not configured (set a valid key in .env)")

    return anthropic.Anthropic(api_key=api_key)


@pytest.fixture(scope="session")
def default_model():
    """Default model for testing."""
    return os.getenv("ANTHROPIC_MODEL", "claude-sonnet-4-20250514")


@pytest.fixture
def trace(langfuse, request):
    """Create a Langfuse trace (span) for the current test.

    Note: Langfuse v3 uses start_span() instead of trace().
    """
    if not langfuse:
        yield None
        return

    # Create a span as the root trace for this test
    span = langfuse.start_span(
        name=request.node.name,
        metadata={
            "test_file": request.node.fspath.basename,
            "test_class": request.node.parent.name if request.node.parent else None,
        }
    )
    yield span
    span.end()
    langfuse.flush()


@pytest.fixture(scope="session")
def agent_builder_url():
    """URL for Agent Builder Service."""
    return os.getenv("AGENT_BUILDER_URL", "http://localhost:8096")


@pytest.fixture(scope="session")
def agent_runtime_url():
    """URL for Agent Runtime Service."""
    return os.getenv("AGENT_RUNTIME_URL", "http://localhost:8088")


@pytest.fixture(scope="session")
def gateway_url():
    """URL for API Gateway."""
    return os.getenv("GATEWAY_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def test_jwt():
    """JWT token for authenticated requests."""
    token = os.getenv("TEST_JWT_TOKEN")
    if not token:
        pytest.skip("TEST_JWT_TOKEN not configured")
    return token

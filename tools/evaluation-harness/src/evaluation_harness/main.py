"""Main entry point for the evaluation harness."""

import os
import sys

import structlog
import uvicorn
from dotenv import load_dotenv

from .config import get_settings


def configure_logging():
    """Configure structured logging."""
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
            structlog.processors.JSONRenderer(),
        ],
        context_class=dict,
        logger_factory=structlog.stdlib.LoggerFactory(),
        wrapper_class=structlog.stdlib.BoundLogger,
        cache_logger_on_first_use=True,
    )


def validate_settings():
    """Validate required settings are present."""
    settings = get_settings()
    logger = structlog.get_logger()

    # Check for OpenAI API key (required for DeepEval)
    if not settings.openai_api_key and not settings.use_azure:
        logger.warning(
            "No OpenAI API key configured. DeepEval metrics will use fallback heuristics.",
            hint="Set EVAL_OPENAI_API_KEY environment variable for full DeepEval support",
        )

    if settings.use_azure and not settings.azure_openai_api_key:
        logger.error(
            "Azure OpenAI enabled but no API key configured",
            hint="Set EVAL_AZURE_OPENAI_API_KEY environment variable",
        )
        sys.exit(1)

    return settings


def main():
    """Main entry point."""
    # Load environment variables from .env file
    load_dotenv()

    # Configure logging
    configure_logging()
    logger = structlog.get_logger()

    # Validate settings
    settings = validate_settings()

    logger.info(
        "Starting HDIM Evaluation Harness",
        host=settings.host,
        port=settings.port,
        workers=settings.workers,
        log_level=settings.log_level,
    )

    # Set OpenAI API key for DeepEval
    if settings.openai_api_key:
        os.environ["OPENAI_API_KEY"] = settings.openai_api_key

    # Run the server
    uvicorn.run(
        "evaluation_harness.api:app",
        host=settings.host,
        port=settings.port,
        workers=settings.workers,
        log_level=settings.log_level.lower(),
        reload=os.getenv("EVAL_DEBUG", "false").lower() == "true",
    )


if __name__ == "__main__":
    main()

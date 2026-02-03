"""Configuration management for the evaluation harness."""

import os
from functools import lru_cache

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    # Server configuration
    host: str = "0.0.0.0"
    port: int = 8500
    workers: int = 4
    log_level: str = "INFO"

    # OpenAI configuration (for DeepEval)
    openai_api_key: str = ""
    openai_model: str = "gpt-4o-mini"  # Cost-effective for evaluation

    # Azure OpenAI (alternative)
    azure_openai_api_key: str = ""
    azure_openai_endpoint: str = ""
    azure_openai_deployment: str = ""
    use_azure: bool = False

    # Evaluation settings
    default_threshold: float = 0.70
    timeout_seconds: int = 30
    max_retries: int = 3

    # Healthcare-specific settings
    hipaa_strict_mode: bool = True
    clinical_safety_zero_tolerance: bool = True

    # Metrics configuration
    enabled_metrics: str = "RELEVANCY,FAITHFULNESS,HALLUCINATION,HIPAA_COMPLIANCE,CLINICAL_ACCURACY,CLINICAL_SAFETY"

    class Config:
        env_prefix = "EVAL_"
        env_file = ".env"
        case_sensitive = False

    @property
    def enabled_metric_list(self) -> list[str]:
        """Parse comma-separated metric list."""
        return [m.strip() for m in self.enabled_metrics.split(",") if m.strip()]


@lru_cache
def get_settings() -> Settings:
    """Get cached settings instance."""
    return Settings()

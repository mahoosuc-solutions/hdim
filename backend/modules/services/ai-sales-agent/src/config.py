"""Configuration for AI Sales Agent service."""

import os
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Service configuration from environment variables."""

    # Anthropic API
    anthropic_api_key: str = os.getenv("ANTHROPIC_API_KEY", "")
    anthropic_model: str = os.getenv("ANTHROPIC_MODEL", "claude-3-5-sonnet-20241022")

    # Service
    service_host: str = os.getenv("SERVICE_HOST", "0.0.0.0")
    service_port: int = int(os.getenv("SERVICE_PORT", "8090"))
    service_debug: bool = os.getenv("SERVICE_DEBUG", "false").lower() == "true"

    # CRM Integration (future)
    crm_type: str = os.getenv("CRM_TYPE", "salesforce")  # salesforce, hubspot
    crm_api_key: str = os.getenv("CRM_API_KEY", "")
    crm_base_url: str = os.getenv("CRM_BASE_URL", "")

    # Logging
    log_level: str = os.getenv("LOG_LEVEL", "INFO")

    class Config:
        """Pydantic configuration."""

        case_sensitive = False


# Global settings instance
settings = Settings()

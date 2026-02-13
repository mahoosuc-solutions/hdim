"""Configuration management for Live Call Sales Agent service."""

from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """Service configuration from environment variables."""

    # Service config
    SERVICE_NAME: str = "live-call-sales-agent"
    SERVICE_PORT: int = 8095
    DEBUG: bool = False

    # Google Cloud config
    GOOGLE_APPLICATION_CREDENTIALS: str = "/secrets/google-meet-service-account.json"
    GOOGLE_MEET_API_ENABLED: bool = True
    GOOGLE_SPEECH_API_ENABLED: bool = True

    # AI Sales Agent integration
    AI_SALES_AGENT_URL: str = "http://ai-sales-agent:8090"

    # WebSocket config (HDIM pattern)
    WEBSOCKET_HOST: str = "localhost"
    WEBSOCKET_PORT: int = 8080
    WEBSOCKET_PATH: str = "/ws"

    # Database config (customer_deployments_db)
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str = "customer_deployments_db"
    POSTGRES_USER: str = "healthdata"
    POSTGRES_PASSWORD: str = "healthdata"

    # Redis config
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0

    # File storage config
    TRANSCRIPT_BASE_PATH: str = "/data/transcripts"

    # Pause detection config (milliseconds)
    PAUSE_THRESHOLD_MS: int = 2000
    MAX_PENDING_SUGGESTIONS: int = 10

    # Bot config
    BOT_HEADLESS: bool = True
    BOT_SANDBOX_DISABLED: bool = True
    PUPPETEER_EXECUTABLE_PATH: Optional[str] = "/usr/bin/google-chrome-stable"

    # Mock mode (for testing without real Google APIs)
    MOCK_GOOGLE_MEET: bool = False
    MOCK_GOOGLE_SPEECH: bool = False

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()

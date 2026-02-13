"""Google authentication for Meet bot service account."""

import logging
from pathlib import Path
from typing import Optional

from google.auth.transport.requests import Request
from google.oauth2 import service_account

logger = logging.getLogger(__name__)

# Google Meet and Speech-to-Text scopes
SCOPES = [
    "https://www.googleapis.com/auth/meetings",
    "https://www.googleapis.com/auth/cloud-platform",
]


class GoogleAuthManager:
    """Manages Google Cloud service account authentication."""

    def __init__(self, credentials_path: str):
        """Initialize with service account credentials file."""
        self.credentials_path = credentials_path
        self.credentials = None
        self._load_credentials()

    def _load_credentials(self) -> None:
        """Load service account credentials from JSON file."""
        try:
            cred_path = Path(self.credentials_path)
            if not cred_path.exists():
                logger.warning(
                    f"⚠️  Credentials file not found: {self.credentials_path}"
                )
                logger.info(
                    "Using mock authentication (set MOCK_GOOGLE_MEET=false to use real credentials)"
                )
                return

            self.credentials = service_account.Credentials.from_service_account_file(
                self.credentials_path, scopes=SCOPES
            )
            self._refresh_credentials()
            logger.info(
                f"✅ Service account authenticated: {self.credentials.service_account_email}"
            )

        except Exception as e:
            logger.error(f"❌ Failed to load credentials: {e}")
            logger.info("Falling back to mock authentication")

    def _refresh_credentials(self) -> None:
        """Refresh credentials if expired."""
        if self.credentials and self.credentials.expired:
            try:
                self.credentials.refresh(Request())
                logger.debug("🔄 Credentials refreshed")
            except Exception as e:
                logger.error(f"❌ Failed to refresh credentials: {e}")

    def get_credentials(self):
        """Get current credentials."""
        if self.credentials and self.credentials.expired:
            self._refresh_credentials()
        return self.credentials

    def get_access_token(self) -> Optional[str]:
        """Get access token for API calls."""
        creds = self.get_credentials()
        if creds:
            return creds.token
        return None

    def is_authenticated(self) -> bool:
        """Check if credentials are available and valid."""
        return self.credentials is not None and not self.credentials.expired

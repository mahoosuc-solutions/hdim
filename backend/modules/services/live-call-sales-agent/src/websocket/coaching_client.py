"""WebSocket client for sending coaching messages to coaching UI."""

import asyncio
import json
import logging
import time
from typing import Optional

try:
    import websockets
except ImportError:
    websockets = None

logger = logging.getLogger(__name__)


class CoachingWebSocketClient:
    """
    Native WebSocket client following HDIM Quality Measure Service pattern.

    **HDIM WebSocket Pattern:**
    - JWT authentication via URL parameter or Authorization header
    - Native WebSocket (not STOMP/SockJS)
    - Topic-based message routing
    - Automatic reconnection with exponential backoff
    - Heartbeat/ping to keep connection alive
    - HIPAA-compliant message structure

    **Topics:**
    - /topic/sales-coaching/{user_id} - Coaching suggestions
    - /topic/sales-transcript/{user_id} - Live transcript updates
    """

    def __init__(
        self,
        user_id: str,
        jwt_token: str,
        tenant_id: str,
        websocket_url: str = "ws://localhost:8080",
        mock_mode: bool = False,
    ):
        """
        Initialize WebSocket client.

        **Args:**
            user_id: User identifier
            jwt_token: JWT authentication token
            tenant_id: HIPAA multi-tenant identifier
            websocket_url: WebSocket endpoint URL
            mock_mode: Simulate messages without real connection
        """
        self.user_id = user_id
        self.jwt_token = jwt_token
        self.tenant_id = tenant_id
        self.websocket_url = websocket_url
        self.mock_mode = mock_mode
        self.ws = None
        self.connected = False
        self.reconnect_attempts = 0
        self.max_reconnect_attempts = 5
        self.heartbeat_task = None

    async def connect(self) -> bool:
        """
        Connect to WebSocket endpoint.

        **Returns:**
            True if connected, False if mock mode or connection failed
        """
        if self.mock_mode:
            logger.info("🎭 Mock mode - not connecting to real WebSocket")
            self.connected = True
            return True

        if not websockets:
            logger.warning("⚠️  websockets library not installed - using mock mode")
            self.mock_mode = True
            return True

        try:
            # Construct WebSocket URL with JWT token
            ws_url = f"{self.websocket_url}/ws?token={self.jwt_token}"

            logger.info(f"🔗 Connecting to WebSocket: {self.websocket_url}")

            self.ws = await websockets.connect(ws_url)
            self.connected = True
            self.reconnect_attempts = 0

            logger.info("✅ WebSocket connected")

            # Start heartbeat
            self.heartbeat_task = asyncio.create_task(self._heartbeat())

            return True

        except Exception as e:
            logger.error(f"❌ WebSocket connection failed: {e}")
            self.connected = False
            return False

    async def disconnect(self) -> None:
        """Disconnect from WebSocket."""
        logger.info("🔌 Disconnecting from WebSocket")

        if self.heartbeat_task:
            self.heartbeat_task.cancel()
            try:
                await self.heartbeat_task
            except asyncio.CancelledError:
                pass

        if self.ws:
            try:
                await self.ws.close()
            except Exception as e:
                logger.debug(f"Error closing WebSocket: {e}")

        self.connected = False
        self.ws = None

    async def send_coaching(self, coaching_message: dict) -> bool:
        """
        Send coaching message to UI.

        **Args:**
            coaching_message: Coaching suggestion dict

        **Returns:**
            True if sent, False if not connected
        """
        if self.mock_mode:
            logger.debug(f"🎭 Mock mode - would send: {coaching_message.get('message', '')[:50]}")
            return True

        if not self.connected or not self.ws:
            logger.warning("⚠️  WebSocket not connected, cannot send message")
            return False

        try:
            # HDIM WebSocket message format
            message = {
                "type": "coaching",
                "timestamp": int(time.time() * 1000),
                "tenantId": self.tenant_id,
                "userId": self.user_id,
                "payload": coaching_message,
            }

            await self.ws.send(json.dumps(message))
            logger.debug(f"📤 Sent coaching: {coaching_message.get('message', '')[:50]}")
            return True

        except Exception as e:
            logger.error(f"❌ Failed to send coaching message: {e}")
            self.connected = False
            return False

    async def send_transcript_update(self, speaker: str, text: str) -> bool:
        """
        Send transcript update to UI.

        **Args:**
            speaker: Speaker label (Speaker 1, Speaker 2, etc.)
            text: Latest transcript text

        **Returns:**
            True if sent, False otherwise
        """
        if self.mock_mode:
            logger.debug(f"🎭 Mock mode - would send transcript: {text[:50]}")
            return True

        if not self.connected or not self.ws:
            return False

        try:
            message = {
                "type": "transcript",
                "timestamp": int(time.time() * 1000),
                "tenantId": self.tenant_id,
                "userId": self.user_id,
                "payload": {"speaker": speaker, "text": text},
            }

            await self.ws.send(json.dumps(message))
            return True

        except Exception as e:
            logger.error(f"❌ Failed to send transcript: {e}")
            self.connected = False
            return False

    async def send_call_status(self, status: dict) -> bool:
        """
        Send call status update.

        **Args:**
            status: Status dict with call metrics

        **Returns:**
            True if sent, False otherwise
        """
        if self.mock_mode:
            logger.debug("🎭 Mock mode - would send status update")
            return True

        if not self.connected or not self.ws:
            return False

        try:
            message = {
                "type": "status",
                "timestamp": int(time.time() * 1000),
                "tenantId": self.tenant_id,
                "userId": self.user_id,
                "payload": status,
            }

            await self.ws.send(json.dumps(message))
            return True

        except Exception as e:
            logger.error(f"❌ Failed to send status: {e}")
            self.connected = False
            return False

    async def _heartbeat(self) -> None:
        """
        Send periodic PING to keep connection alive.

        **HDIM Pattern:**
        - PING every 25 seconds
        - Prevents connection timeout
        - Detects dead connections
        """
        while self.connected:
            try:
                await asyncio.sleep(25)

                if self.ws:
                    ping_msg = {
                        "type": "PING",
                        "timestamp": int(time.time() * 1000),
                        "tenantId": self.tenant_id,
                    }
                    await self.ws.send(json.dumps(ping_msg))
                    logger.debug("💓 Heartbeat sent")

            except asyncio.CancelledError:
                logger.debug("Heartbeat cancelled")
                break
            except Exception as e:
                logger.error(f"❌ Heartbeat failed: {e}")
                self.connected = False
                break

    async def reconnect(self) -> bool:
        """
        Attempt to reconnect with exponential backoff.

        **Strategy:**
        - Wait 1s, 2s, 4s, 8s, 16s between attempts
        - Max 5 attempts (31s total)
        - Return True if successfully reconnected

        **Returns:**
            True if connected after reconnect, False otherwise
        """
        while self.reconnect_attempts < self.max_reconnect_attempts:
            wait_time = 2 ** self.reconnect_attempts
            logger.info(f"⏳ Reconnecting in {wait_time}s (attempt {self.reconnect_attempts + 1}/{self.max_reconnect_attempts})")

            await asyncio.sleep(wait_time)
            self.reconnect_attempts += 1

            if await self.connect():
                return True

        logger.error("❌ Failed to reconnect after max attempts")
        return False

    def is_connected(self) -> bool:
        """Check if connected."""
        return self.connected

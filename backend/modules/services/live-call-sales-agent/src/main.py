"""FastAPI entry point for Live Call Sales Agent service."""

import asyncio
import logging
from contextlib import asynccontextmanager
from typing import Dict, Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .config import settings
from .meet_bot.auth import GoogleAuthManager
from .meet_bot.bot import MeetBot
from .transcription.google_speech import SpeechClient

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


# ============================================================================
# Request/Response Models
# ============================================================================

class JoinMeetingRequest(BaseModel):
    """Request to join a Google Meet call."""
    meeting_url: str
    user_id: str
    tenant_id: str
    customer_name: str
    persona_type: str = "cmo"  # cmo, cffo, provider, coordinator, it_leader


class JoinMeetingResponse(BaseModel):
    """Response when bot joins meeting."""
    status: str
    meeting_url: str
    user_id: str
    call_id: str
    message: str


class HealthResponse(BaseModel):
    """Health check response."""
    status: str
    service: str
    version: str


# ============================================================================
# Global State (Phase 1: Real Bot Instances)
# ============================================================================

active_bots: Dict[str, dict] = {}  # user_id -> bot instance and state
auth_manager: Optional[GoogleAuthManager] = None


# ============================================================================
# Lifecycle Management
# ============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage application lifecycle."""
    global auth_manager
    logger.info(f"✅ {settings.SERVICE_NAME} starting on port {settings.SERVICE_PORT}")

    # Startup: Initialize Google authentication
    try:
        auth_manager = GoogleAuthManager(settings.GOOGLE_APPLICATION_CREDENTIALS)
        if auth_manager.is_authenticated():
            logger.info("✅ Google authentication ready")
        else:
            logger.warning("⚠️  Google authentication not available - using mock mode")
    except Exception as e:
        logger.error(f"❌ Failed to initialize auth: {e}")
        logger.info("Continuing with mock mode")

    yield

    # Shutdown: Cleanup active bots
    logger.info(f"🛑 {settings.SERVICE_NAME} shutting down")
    for user_id, bot_info in list(active_bots.items()):
        if bot_info.get("bot"):
            try:
                await bot_info["bot"].leave_meeting()
            except Exception as e:
                logger.error(f"Error cleaning up bot for {user_id}: {e}")


# ============================================================================
# FastAPI Application
# ============================================================================

app = FastAPI(
    title=settings.SERVICE_NAME,
    description="Real-time coaching bot for Google Meet sales calls",
    version="1.0.0",
    lifespan=lifespan,
)


# ============================================================================
# Health Check Endpoint
# ============================================================================

@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    return HealthResponse(
        status="healthy",
        service=settings.SERVICE_NAME,
        version="1.0.0",
    )


# ============================================================================
# Meet Bot Endpoints
# ============================================================================

@app.post("/api/meet/join", response_model=JoinMeetingResponse)
async def join_meeting(request: JoinMeetingRequest):
    """
    Join a Google Meet call and start coaching.

    **Phase 1 (Real Bot Integration):**
    - Authenticate with Google service account (or OAuth fallback)
    - Create MeetBot instance
    - Join meeting via headless Chrome
    - Start audio capture and transcription

    **Returns:**
        JoinMeetingResponse with call_id and status
    """
    global auth_manager

    logger.info(f"📞 Joining meeting for user {request.user_id}")
    logger.info(f"   Customer: {request.customer_name}")
    logger.info(f"   Persona: {request.persona_type}")
    logger.info(f"   URL: {request.meeting_url}")

    # Generate call ID
    call_id = f"call-{request.user_id}-{hash(request.meeting_url) % 10000:04d}"

    # Check if bot already active for this user
    if request.user_id in active_bots:
        raise HTTPException(
            status_code=409,
            detail=f"User {request.user_id} already has an active bot",
        )

    try:
        # Create bot instance
        mock_mode = settings.MOCK_GOOGLE_MEET or not auth_manager or not auth_manager.is_authenticated()
        bot = MeetBot(auth_manager or GoogleAuthManager(""), mock_mode=mock_mode)

        # Join meeting
        joined = await bot.join_meeting(request.meeting_url)
        if not joined:
            raise HTTPException(
                status_code=503,
                detail="Failed to join meeting - check authentication and meeting URL",
            )

        # Store bot state
        active_bots[request.user_id] = {
            "bot": bot,
            "call_id": call_id,
            "meeting_url": request.meeting_url,
            "tenant_id": request.tenant_id,
            "customer_name": request.customer_name,
            "persona_type": request.persona_type,
            "transcript_segments": [],
            "coaching_messages": [],
        }

        return JoinMeetingResponse(
            status="joined",
            meeting_url=request.meeting_url,
            user_id=request.user_id,
            call_id=call_id,
            message=f"✅ Bot joined call for {request.customer_name} ({request.persona_type})",
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"❌ Error joining meeting: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Error joining meeting: {str(e)}",
        )


@app.post("/api/meet/leave/{user_id}")
async def leave_meeting(user_id: str):
    """
    Leave the current meeting and cleanup resources.

    **Returns:**
        Status, call statistics, and transcript/coaching summary
    """
    logger.info(f"👋 Leaving meeting for user {user_id}")

    if user_id not in active_bots:
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")

    bot_info = active_bots.pop(user_id)

    try:
        # Leave meeting
        bot = bot_info.get("bot")
        if bot:
            await bot.leave_meeting()

        return {
            "status": "left",
            "call_id": bot_info.get("call_id"),
            "transcript_segments": len(bot_info.get("transcript_segments", [])),
            "coaching_messages": len(bot_info.get("coaching_messages", [])),
        }

    except Exception as e:
        logger.error(f"❌ Error leaving meeting: {e}", exc_info=True)
        raise HTTPException(
            status_code=500,
            detail=f"Error leaving meeting: {str(e)}",
        )


@app.get("/api/meet/status/{user_id}")
async def get_call_status(user_id: str):
    """
    Get current call status for user.

    **Returns:**
        Bot status, call metadata, and transcript/coaching statistics
    """
    if user_id not in active_bots:
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")

    bot_info = active_bots[user_id]
    bot = bot_info.get("bot")

    # Get bot status
    bot_status = bot.get_status() if bot else {}

    return {
        "status": "active" if bot_status.get("is_joined") else "inactive",
        "call_id": bot_info.get("call_id"),
        "customer_name": bot_info.get("customer_name"),
        "persona_type": bot_info.get("persona_type"),
        "transcript_segments": len(bot_info.get("transcript_segments", [])),
        "coaching_messages": len(bot_info.get("coaching_messages", [])),
        "bot_status": bot_status,
    }


# ============================================================================
# Coaching Endpoints (Integration with AI Sales Agent)
# ============================================================================

@app.post("/api/sales/coach/live-call")
async def generate_live_coaching(request: dict):
    """
    Generate real-time coaching based on live transcript.

    **Phase 0 (Mock Implementation):**
    Returns mock coaching suggestion.
    Real AI Sales Agent integration in Phase 2.
    """
    logger.info("🎯 Generating coaching suggestion")
    logger.info(f"   Persona: {request.get('persona_type')}")
    logger.info(f"   Phase: {request.get('call_phase')}")

    # Mock coaching suggestion
    return {
        "type": "improvement",
        "severity": "low",
        "message": "Consider asking about their current gap closure rate",
        "confidence": 0.75,
    }


# ============================================================================
# Health & Diagnostics
# ============================================================================

@app.get("/api/diagnostics")
async def diagnostics():
    """Diagnostics endpoint for troubleshooting."""
    return {
        "service": settings.SERVICE_NAME,
        "active_calls": len(active_bots),
        "google_meet_enabled": settings.GOOGLE_MEET_API_ENABLED,
        "google_speech_enabled": settings.GOOGLE_SPEECH_API_ENABLED,
        "mock_meet": settings.MOCK_GOOGLE_MEET,
        "mock_speech": settings.MOCK_GOOGLE_SPEECH,
        "redis": f"{settings.REDIS_HOST}:{settings.REDIS_PORT}",
        "postgres": f"{settings.POSTGRES_HOST}:{settings.POSTGRES_PORT}/{settings.POSTGRES_DB}",
    }


# ============================================================================
# Error Handlers
# ============================================================================

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    """Handle HTTP exceptions."""
    return JSONResponse(
        status_code=exc.status_code,
        content={"status": "error", "message": exc.detail},
    )


@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    """Handle unexpected exceptions."""
    logger.error(f"❌ Unexpected error: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"status": "error", "message": "Internal server error"},
    )


# ============================================================================
# Startup
# ============================================================================

if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.SERVICE_PORT,
        reload=settings.DEBUG,
        log_level="info",
    )

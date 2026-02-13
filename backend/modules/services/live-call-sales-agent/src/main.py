"""FastAPI entry point for Live Call Sales Agent service."""

import asyncio
import logging
from contextlib import asynccontextmanager
from typing import Dict, Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .config import settings

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
# Global State (Mock for Phase 0)
# ============================================================================

active_bots: Dict[str, dict] = {}  # user_id -> bot state


# ============================================================================
# Lifecycle Management
# ============================================================================

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage application lifecycle."""
    logger.info(f"✅ {settings.SERVICE_NAME} starting on port {settings.SERVICE_PORT}")

    # Startup
    yield

    # Shutdown
    logger.info(f"🛑 {settings.SERVICE_NAME} shutting down")
    # Cleanup active bots
    for user_id, bot_state in active_bots.items():
        if bot_state.get("active"):
            logger.info(f"Cleaning up bot for user {user_id}")


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

    **Phase 0 (Mock Implementation):**
    Returns success immediately without connecting to Google Meet.
    Real Google Meet integration in Phase 1.
    """
    logger.info(f"📞 Joining meeting for user {request.user_id}")
    logger.info(f"   Customer: {request.customer_name}")
    logger.info(f"   Persona: {request.persona_type}")
    logger.info(f"   URL: {request.meeting_url}")

    # Generate call ID (mock)
    call_id = f"call-{request.user_id}-{hash(request.meeting_url) % 10000:04d}"

    # Store bot state (mock)
    active_bots[request.user_id] = {
        "active": True,
        "call_id": call_id,
        "meeting_url": request.meeting_url,
        "tenant_id": request.tenant_id,
        "customer_name": request.customer_name,
        "persona_type": request.persona_type,
        "joined_at": None,  # Will be set when real bot joins
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


@app.post("/api/meet/leave/{user_id}")
async def leave_meeting(user_id: str):
    """Leave the current meeting."""
    logger.info(f"👋 Leaving meeting for user {user_id}")

    if user_id not in active_bots:
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")

    bot_state = active_bots.pop(user_id)

    return {
        "status": "left",
        "call_id": bot_state.get("call_id"),
        "transcript_segments": len(bot_state.get("transcript_segments", [])),
        "coaching_messages": len(bot_state.get("coaching_messages", [])),
    }


@app.get("/api/meet/status/{user_id}")
async def get_call_status(user_id: str):
    """Get current call status for user."""
    if user_id not in active_bots:
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")

    bot_state = active_bots[user_id]

    return {
        "status": "active" if bot_state.get("active") else "inactive",
        "call_id": bot_state.get("call_id"),
        "customer_name": bot_state.get("customer_name"),
        "persona_type": bot_state.get("persona_type"),
        "transcript_segments": len(bot_state.get("transcript_segments", [])),
        "coaching_messages": len(bot_state.get("coaching_messages", [])),
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

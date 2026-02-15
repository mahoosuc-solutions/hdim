"""
HDIM AI VP Sales Agent - FastAPI Service

Entry point for autonomous discovery calls, demo generation, and pipeline management.
Available at: http://localhost:8090
"""

import csv
import io
import logging
import os
import sqlite3
import threading
import uuid
from datetime import datetime, timezone
from typing import Literal, Optional

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse, PlainTextResponse
from pydantic import BaseModel, Field

from agents.discovery_agent import DiscoveryAgent

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="HDIM AI VP Sales Agent",
    description="Autonomous sales execution for Phase 2 launch (Mar 1, 2026)",
    version="0.1.0",
)

DB_PATH = os.getenv("OPERATOR_AUDIT_DB_PATH", "/tmp/ai_sales_operator.db")
db_lock = threading.Lock()

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:8090",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize agents
discovery_agent = DiscoveryAgent()

# ============================================================================
# REQUEST/RESPONSE MODELS
# ============================================================================


class DiscoveryCallRequest(BaseModel):
    """Request to execute a discovery call."""

    customer_name: str
    customer_context: str
    persona_type: str = "cmo"
    call_duration_minutes: int = 30


class DiscoveryCallResponse(BaseModel):
    """Response from a discovery call."""

    customer_name: str
    persona_type: str
    qualification: str  # green, yellow, red
    pain_points_discovered: list[str]
    call_score: float
    next_steps: str
    call_transcript_preview: str  # First 500 chars


class PipelineMetrics(BaseModel):
    """Pipeline health snapshot."""

    total_leads: int
    stage_distribution: dict
    weighted_pipeline: str
    expected_close_rate_percent: float
    forecasted_revenue_30days: str


class OperatorStateResponse(BaseModel):
    objective: str
    phase: str
    confidence: float = Field(ge=0, le=1)
    next_action: str
    risk_level: Literal["low", "medium", "high"]
    autonomy: Literal["manual", "assisted", "auto"]


class PendingAction(BaseModel):
    id: str
    title: str
    rationale: str
    impact: Literal["low", "medium", "high"]
    created_at: int
    status: Literal["pending", "approved", "rejected", "revision_requested"]


class DecisionEvent(BaseModel):
    action_id: str
    action_title: str
    decision: Literal["approved", "rejected", "revision_requested"]
    decided_at: int
    operator: str


class SubmitDecisionRequest(BaseModel):
    decision: Literal["approved", "rejected", "revision_requested"]
    operator_id: str = "operator"


def now_ms() -> int:
    return int(datetime.now(tz=timezone.utc).timestamp() * 1000)


def get_db_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_operator_tables() -> None:
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS operator_state (
              key TEXT PRIMARY KEY,
              value TEXT NOT NULL
            )
            """
        )
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS pending_actions (
              id TEXT PRIMARY KEY,
              title TEXT NOT NULL,
              rationale TEXT NOT NULL,
              impact TEXT NOT NULL,
              created_at INTEGER NOT NULL,
              status TEXT NOT NULL
            )
            """
        )
        cur.execute(
            """
            CREATE TABLE IF NOT EXISTS decision_events (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              action_id TEXT NOT NULL,
              action_title TEXT NOT NULL,
              decision TEXT NOT NULL,
              decided_at INTEGER NOT NULL,
              operator TEXT NOT NULL
            )
            """
        )
        conn.commit()
        conn.close()


def upsert_operator_state(state: "OperatorStateResponse") -> None:
    values = {
        "objective": state.objective,
        "phase": state.phase,
        "confidence": str(state.confidence),
        "next_action": state.next_action,
        "risk_level": state.risk_level,
        "autonomy": state.autonomy,
    }
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        for key, value in values.items():
            cur.execute(
                """
                INSERT INTO operator_state (key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value=excluded.value
                """,
                (key, value),
            )
        conn.commit()
        conn.close()


def load_operator_state() -> Optional["OperatorStateResponse"]:
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("SELECT key, value FROM operator_state")
        rows = cur.fetchall()
        conn.close()
    if not rows:
        return None
    data = {row["key"]: row["value"] for row in rows}
    try:
        return OperatorStateResponse(
            objective=data["objective"],
            phase=data["phase"],
            confidence=float(data["confidence"]),
            next_action=data["next_action"],
            risk_level=data["risk_level"],
            autonomy=data["autonomy"],
        )
    except KeyError:
        return None


def seed_pending_actions_if_empty(actions: list["PendingAction"]) -> None:
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) AS count FROM pending_actions")
        count = cur.fetchone()["count"]
        if count == 0:
            for action in actions:
                cur.execute(
                    """
                    INSERT INTO pending_actions (id, title, rationale, impact, created_at, status)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    (
                        action.id,
                        action.title,
                        action.rationale,
                        action.impact,
                        action.created_at,
                        action.status,
                    ),
                )
        conn.commit()
        conn.close()


def fetch_pending_actions(status: str = "pending") -> list["PendingAction"]:
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            """
            SELECT id, title, rationale, impact, created_at, status
            FROM pending_actions
            WHERE status = ?
            ORDER BY created_at DESC
            """,
            (status,),
        )
        rows = cur.fetchall()
        conn.close()
    return [PendingAction(**dict(row)) for row in rows]


def update_action_status(action_id: str, status: str) -> Optional["PendingAction"]:
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            """
            UPDATE pending_actions
            SET status = ?
            WHERE id = ?
            """,
            (status, action_id),
        )
        if cur.rowcount == 0:
            conn.close()
            return None
        cur.execute(
            """
            SELECT id, title, rationale, impact, created_at, status
            FROM pending_actions
            WHERE id = ?
            """,
            (action_id,),
        )
        row = cur.fetchone()
        conn.commit()
        conn.close()
    return PendingAction(**dict(row)) if row else None


def insert_decision_event(event: "DecisionEvent") -> None:
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            """
            INSERT INTO decision_events (action_id, action_title, decision, decided_at, operator)
            VALUES (?, ?, ?, ?, ?)
            """,
            (
                event.action_id,
                event.action_title,
                event.decision,
                event.decided_at,
                event.operator,
            ),
        )
        conn.commit()
        conn.close()


def fetch_decision_history(limit: int = 50) -> list["DecisionEvent"]:
    bounded_limit = min(max(limit, 1), 500)
    with db_lock:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute(
            """
            SELECT action_id, action_title, decision, decided_at, operator
            FROM decision_events
            ORDER BY decided_at DESC, id DESC
            LIMIT ?
            """,
            (bounded_limit,),
        )
        rows = cur.fetchall()
        conn.close()
    return [DecisionEvent(**dict(row)) for row in rows]


# In-memory operator state mirrors persisted DB state.
operator_state = OperatorStateResponse(
    objective="Guide discovery call to qualified next step",
    phase="Discovery",
    confidence=0.62,
    next_action="Ask pain-clarification follow-up",
    risk_level="medium",
    autonomy="assisted",
)

pending_actions: list[PendingAction] = [
    PendingAction(
        id=f"act-{uuid.uuid4().hex[:8]}",
        title="Offer ROI mini-case tailored to prospect",
        rationale="Prospect requested evidence before discussing pricing.",
        impact="high",
        created_at=now_ms(),
        status="pending",
    ),
    PendingAction(
        id=f"act-{uuid.uuid4().hex[:8]}",
        title="Schedule technical validation follow-up",
        rationale="Buyer signaled integration concerns with current CRM.",
        impact="medium",
        created_at=now_ms(),
        status="pending",
    ),
]


# ============================================================================
# API ENDPOINTS
# ============================================================================


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "ai-sales-agent", "version": "0.1.0"}


@app.post("/api/sales/discovery-call", response_model=DiscoveryCallResponse)
async def execute_discovery_call(request: DiscoveryCallRequest):
    """Execute a 30-minute autonomous discovery call.

    This endpoint simulates a discovery call with a customer based on:
    - Customer name and context
    - Persona type (CMO, Coordinator, CFO, Provider, IT)
    - Call duration (15, 30, or 45 minutes)

    Returns:
    - Call transcript
    - Pain points discovered
    - Qualification (green/yellow/red)
    - Call quality score (0-100)
    - Recommended next steps

    Example:
    ```
    POST /api/sales/discovery-call
    {
      "customer_name": "HealthFirst Insurance",
      "customer_context": "500K members, CMO on call, pain: manual gap closure",
      "persona_type": "cmo",
      "call_duration_minutes": 30
    }
    ```
    """
    try:
        logger.info(
            f"Executing discovery call: {request.customer_name} "
            f"(persona: {request.persona_type})"
        )

        # Execute discovery call
        result = discovery_agent.execute_discovery_call(
            customer_name=request.customer_name,
            customer_context=request.customer_context,
            persona_type=request.persona_type,
            call_duration_minutes=request.call_duration_minutes,
        )

        # Return response
        return DiscoveryCallResponse(
            customer_name=result.customer_name,
            persona_type=result.persona_type,
            qualification=result.qualification,
            pain_points_discovered=result.pain_points_discovered,
            call_score=result.call_score,
            next_steps=result.next_steps,
            call_transcript_preview=result.call_transcript[:500],
        )

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        logger.error(f"Discovery call failed: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail="Discovery call execution failed")


@app.get("/api/sales/pipeline")
async def get_pipeline_health() -> PipelineMetrics:
    """Get current pipeline health snapshot.

    Returns metrics on:
    - Total leads in pipeline
    - Distribution by stage (discovery, demo, proposal, negotiation, closed)
    - Weighted pipeline value
    - Expected close rate percentage
    - 30-day revenue forecast

    Example:
    ```
    GET /api/sales/pipeline

    {
      "total_leads": 47,
      "stage_distribution": {
        "discovery": 15,
        "demo_scheduled": 8,
        "proposal": 3,
        "negotiation": 1,
        "closed": 2
      },
      "weighted_pipeline": "$2.4M",
      "expected_close_rate_percent": 28.5,
      "forecasted_revenue_30days": "$150K-$250K"
    }
    ```
    """
    # In production, fetch from CRM (Salesforce, HubSpot)
    # For MVP, return simulated metrics
    return PipelineMetrics(
        total_leads=47,
        stage_distribution={
            "discovery": 15,
            "demo_scheduled": 8,
            "proposal": 3,
            "negotiation": 1,
            "closed": 2,
        },
        weighted_pipeline="$2.4M",
        expected_close_rate_percent=28.5,
        forecasted_revenue_30days="$150K-$250K (Mar 2026)",
    )


@app.get("/api/sales/agents")
async def list_agents():
    """List available agents and their capabilities.

    Returns information on all sales agents:
    - Discovery Agent: 30-minute discovery calls
    - Demo Agent: Persona-specific demos (15/30/45 min)
    - Objection Agent: 6 common objection handlers
    - Pipeline Agent: Deal scoring and forecasting
    """
    return {
        "agents": [
            {
                "name": "Discovery Agent",
                "status": "active",
                "capability": "Execute 30-minute autonomous discovery calls",
                "endpoint": "POST /api/sales/discovery-call",
                "personas": ["cmo", "coordinator", "cfo", "provider", "it"],
            },
            {
                "name": "Demo Agent",
                "status": "planned",
                "capability": "Generate persona-specific product demos (15/30/45 min)",
                "timeline": "Week 2 (Feb 22-28)",
            },
            {
                "name": "Objection Agent",
                "status": "planned",
                "capability": "Handle 6 common objections with real-time reframes",
                "timeline": "Month 2 (March)",
            },
            {
                "name": "Pipeline Agent",
                "status": "planned",
                "capability": "Score deals, identify stalled opportunities, forecast revenue",
                "timeline": "Month 2 (March)",
            },
        ],
        "phase": "Phase 1: Discovery Agent (Feb 15-21)",
        "launch_target": "March 1, 2026",
        "goals": {
            "discovery_calls": "50-100",
            "lois_signed": "1-2",
            "committed_revenue": "$50-100K",
            "avg_call_quality_score": "80+/100",
        },
    }


@app.get("/api/sales/personas")
async def list_personas():
    """Get information on 5 customer personas.

    Returns details on:
    - CMO / VP Quality
    - Quality Coordinator
    - Healthcare Provider / Physician
    - CFO / VP Finance
    - IT / Analytics Leader
    """
    return {
        "personas": [
            {
                "name": "CMO / VP Quality",
                "priority": "Primary buyer (5/5)",
                "pain_points": [
                    "HEDIS deadline pressure",
                    "Reactive gap discovery",
                    "Manual workflows",
                ],
                "green_flags": [
                    "Mentions specific HEDIS targets",
                    "Interested in pilot with metrics",
                ],
            },
            {
                "name": "Quality Coordinator",
                "priority": "Secondary (4/5)",
                "pain_points": [
                    "Manual gap prioritization",
                    "Provider outreach burden",
                    "Time-consuming processes",
                ],
                "green_flags": [
                    "Frustrated with manual work",
                    "Interested in automation",
                ],
            },
            {
                "name": "CFO / VP Finance",
                "priority": "Budget decision (5/5)",
                "pain_points": [
                    "Unclear ROI on quality initiatives",
                    "High manual labor costs",
                    "Missing quality bonuses",
                ],
                "green_flags": [
                    "Asks about ROI with specific numbers",
                    "Interested in pilot ROI",
                ],
            },
            {
                "name": "Healthcare Provider",
                "priority": "Tertiary (3/5)",
                "pain_points": [
                    "Alert fatigue",
                    "Lack of clinical context",
                    "Integration challenges",
                ],
                "green_flags": [
                    "Interested in clinical narratives",
                    "Wants better engagement",
                ],
            },
            {
                "name": "IT / Analytics Leader",
                "priority": "Technical gate (4/5)",
                "pain_points": [
                    "Complex integrations",
                    "Security/compliance requirements",
                    "Data quality issues",
                ],
                "green_flags": [
                    "Questions about HIPAA compliance",
                    "Interested in API specs",
                ],
            },
        ]
    }


@app.get("/api/sales/playbooks")
async def get_discovery_playbook():
    """Get the 30-minute discovery call playbook structure.

    Returns:
    - Call structure (opening, pain discovery, positioning, next steps)
    - Questions by phase
    - Qualification framework
    - Scoring rubric
    - Persona-specific talking points
    """
    return {
        "playbook": "30-Minute Discovery Call Script",
        "phases": [
            {
                "phase": "Opening & Credibility",
                "duration_minutes": 5,
                "goal": "Build credibility, hook attention",
            },
            {
                "phase": "Pain Discovery",
                "duration_minutes": 15,
                "goal": "Uncover 3+ pain points",
            },
            {
                "phase": "Solution Positioning",
                "duration_minutes": 10,
                "goal": "Connect HDIM capabilities to pain",
            },
            {
                "phase": "Qualification & Next Steps",
                "duration_minutes": 5,
                "goal": "Qualify and schedule demo/call",
            },
        ],
        "qualification_framework": {
            "green": "25K+ members, Q1/Q2 budget, 30-60d decision, high pain",
            "yellow": "20-25K members, Q3 budget, moderate pain",
            "red": "<20K members, 2027 budget, low pain",
        },
        "scoring_rubric": {
            "pain_discovery": 20,
            "qualification": 20,
            "objection_handling": 15,
            "next_steps": 20,
            "credibility": 25,
            "total": 100,
        },
    }


@app.get("/api/sales/operator/state", response_model=OperatorStateResponse)
async def get_operator_state():
    """Get live operator state for AI sales console."""
    return operator_state


@app.get("/api/sales/operator/actions/pending")
async def get_pending_actions():
    """List pending actions requiring operator review."""
    actions = fetch_pending_actions(status="pending")
    return {"actions": actions}


@app.get("/api/sales/operator/decisions")
async def get_decision_history(limit: int = 50):
    """List most recent operator decisions."""
    return {"decisions": fetch_decision_history(limit=limit)}


@app.get("/api/sales/operator/activity/export")
async def export_operator_activity(format: Literal["json", "csv"] = "json", limit: int = 200):
    """Export operator activity feed in JSON or CSV format."""
    events = fetch_decision_history(limit=limit)
    if format == "json":
        return {"decisions": events, "count": len(events), "exported_at": now_ms()}

    csv_buffer = io.StringIO()
    writer = csv.writer(csv_buffer)
    writer.writerow(["action_id", "action_title", "decision", "decided_at", "operator"])
    for event in events:
        writer.writerow(
            [
                event.action_id,
                event.action_title,
                event.decision,
                event.decided_at,
                event.operator,
            ]
        )
    return PlainTextResponse(
        content=csv_buffer.getvalue(),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=operator-activity.csv"},
    )


@app.post("/api/sales/operator/actions/{action_id}/decision")
async def submit_operator_decision(action_id: str, request: SubmitDecisionRequest, http_request: Request):
    """Approve, reject, or request revision for a pending action."""
    role = (http_request.headers.get("x-operator-role") or "viewer").lower()
    if role not in {"operator", "admin"}:
        raise HTTPException(status_code=403, detail="Operator role required for decision actions")

    action = update_action_status(action_id=action_id, status=request.decision)
    if action is None:
        raise HTTPException(status_code=404, detail=f"Action not found: {action_id}")

    event = DecisionEvent(
        action_id=action.id,
        action_title=action.title,
        decision=request.decision,
        decided_at=now_ms(),
        operator=request.operator_id or "operator",
    )
    insert_decision_event(event)

    operator_state.next_action = (
        "Execute approved action"
        if request.decision == "approved"
        else "Re-plan next action with updated constraints"
    )
    if request.decision == "rejected":
        operator_state.risk_level = "medium"
        operator_state.confidence = 0.48
    elif request.decision == "revision_requested":
        operator_state.risk_level = "low"
        operator_state.confidence = 0.55
    else:
        operator_state.risk_level = "low"
        operator_state.confidence = 0.71
    upsert_operator_state(operator_state)

    return {"status": "ok", "event": event}


# ============================================================================
# ERROR HANDLERS
# ============================================================================


@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    """Handle HTTP exceptions."""
    return JSONResponse(
        status_code=exc.status_code,
        content={"detail": exc.detail, "service": "ai-sales-agent"},
    )


@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    """Handle general exceptions."""
    logger.error(f"Unhandled exception: {str(exc)}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error", "service": "ai-sales-agent"},
    )


# ============================================================================
# STARTUP/SHUTDOWN
# ============================================================================


@app.on_event("startup")
async def startup_event():
    """Initialize on startup."""
    global operator_state
    init_operator_tables()
    state_from_db = load_operator_state()
    if state_from_db is None:
        upsert_operator_state(operator_state)
    else:
        operator_state = state_from_db
    seed_pending_actions_if_empty(pending_actions)
    logger.info("AI VP Sales Agent starting up")
    logger.info("Discovery Agent: ACTIVE")
    logger.info("Demo Agent: PLANNED (Week 2)")
    logger.info("Objection Agent: PLANNED (Month 2)")
    logger.info("Pipeline Agent: PLANNED (Month 2)")
    logger.info(f"Operator audit DB initialized at {DB_PATH}")


@app.on_event("shutdown")
async def shutdown_event():
    """Cleanup on shutdown."""
    logger.info("AI VP Sales Agent shutting down")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8090,
        log_level="info",
    )

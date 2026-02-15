"""
Coordinator Agent (Sales Director) - Routes work to specialized agents.

Responsibilities:
- Route incoming customer interactions to appropriate worker agents
- Track overall pipeline health (value, stage distribution, velocity)
- Make strategic prioritization decisions (which deals to push)
- Escalate complex situations to human sales leaders
"""

import logging
from typing import Optional
from enum import Enum

logger = logging.getLogger(__name__)


class TaskType(str, Enum):
    """Types of sales tasks."""

    NEW_LEAD_DISCOVERY = "new_lead_discovery"
    SCHEDULE_DEMO = "schedule_demo"
    HANDLE_OBJECTION = "handle_objection"
    ASSESS_PIPELINE = "assess_pipeline"
    ESCALATE_HUMAN = "escalate_human"


class AgentType(str, Enum):
    """Available specialized agents."""

    DISCOVERY = "discovery"
    DEMO = "demo"
    OBJECTION = "objection"
    PIPELINE = "pipeline"


class CoordinatorAgent:
    """Sales Director - coordinates team of specialized agents."""

    def __init__(self):
        """Initialize coordinator agent."""
        self.logger = logging.getLogger(self.__class__.__name__)

    def route_task(
        self, task_type: TaskType, customer_context: str, urgency: int = 5
    ) -> dict:
        """Route incoming task to appropriate agent.

        Args:
            task_type: Type of task (new discovery, demo, objection, etc.)
            customer_context: Customer/deal context
            urgency: 1-10 scale (10 = urgent, 1 = low priority)

        Returns:
            Routing decision with recommended agent and action
        """
        routing = {}

        if task_type == TaskType.NEW_LEAD_DISCOVERY:
            routing["agent"] = AgentType.DISCOVERY
            routing["action"] = "Execute 30-minute discovery call"
            routing["priority"] = urgency

        elif task_type == TaskType.SCHEDULE_DEMO:
            routing["agent"] = AgentType.DEMO
            routing["action"] = "Generate persona-specific demo outline"
            routing["priority"] = urgency

        elif task_type == TaskType.HANDLE_OBJECTION:
            routing["agent"] = AgentType.OBJECTION
            routing["action"] = "Handle objection with reframe"
            routing["priority"] = urgency

        elif task_type == TaskType.ASSESS_PIPELINE:
            routing["agent"] = AgentType.PIPELINE
            routing["action"] = "Analyze pipeline health and velocity"
            routing["priority"] = 1  # Low urgency for analytics

        elif task_type == TaskType.ESCALATE_HUMAN:
            routing["agent"] = None
            routing["action"] = "Escalate to VP Sales (human)"
            routing["priority"] = 10  # Highest urgency

        self.logger.info(
            f"Routed {task_type} to {routing.get('agent')} "
            f"(priority: {routing.get('priority')}/10)"
        )

        return routing

    def get_pipeline_health(self) -> dict:
        """Get current pipeline health snapshot.

        Returns metrics on:
        - Total leads by stage
        - Weighted pipeline value
        - Close rate forecast
        - Revenue velocity
        """
        # In production, fetch from CRM (Salesforce, HubSpot)
        # For MVP, return simulated metrics
        return {
            "total_leads": 47,
            "by_stage": {
                "discovery": 15,
                "demo_scheduled": 8,
                "proposal": 3,
                "negotiation": 1,
                "closed": 2,
            },
            "weighted_pipeline": 2_400_000,  # $2.4M
            "expected_close_rate": 0.285,  # 28.5%
            "forecasted_revenue_30days": {
                "low": 150_000,  # $150K
                "high": 250_000,  # $250K
            },
            "pipeline_health": "strong",  # or "weak", "moderate"
        }

    def prioritize_deals(self) -> list[dict]:
        """Get prioritized list of deals to work.

        Returns top N deals ranked by:
        1. Deal size (ACV)
        2. Decision timeline (urgent = high priority)
        3. Qualification level (green = high priority)
        4. Buying signals detected
        5. Stalled time (stuck deals = get attention)
        """
        # In production, rank from CRM pipeline
        return [
            {
                "rank": 1,
                "customer": "HealthFirst Insurance",
                "acv": 200_000,
                "stage": "demo_scheduled",
                "decision_timeline": "30 days",
                "qualification": "green",
                "next_action": "Follow up on demo scheduling",
                "priority_score": 95,
            },
            {
                "rank": 2,
                "customer": "MediCare Plus",
                "acv": 150_000,
                "stage": "discovery",
                "decision_timeline": "60 days",
                "qualification": "green",
                "next_action": "Schedule demo",
                "priority_score": 88,
            },
            {
                "rank": 3,
                "customer": "Blue Shield",
                "acv": 300_000,
                "stage": "proposal",
                "decision_timeline": "45 days",
                "qualification": "yellow",
                "next_action": "Follow up on proposal questions",
                "priority_score": 82,
            },
        ]

    def identify_stalled_deals(self, days_threshold: int = 7) -> list[dict]:
        """Identify deals stalled for >N days without activity.

        Args:
            days_threshold: Days without activity threshold (default: 7)

        Returns:
            List of stalled deals with recommended actions
        """
        # In production, query CRM for deals with no activity in >N days
        return [
            {
                "customer": "Anthem Health",
                "stage": "demo_scheduled",
                "days_stalled": 9,
                "last_activity": "Feb 10 (email sent)",
                "recommended_action": "Send follow-up email + call",
                "priority": "high",
            },
            {
                "customer": "United Health",
                "stage": "discovery",
                "days_stalled": 5,
                "last_activity": "Feb 9 (call completed)",
                "recommended_action": "Schedule demo",
                "priority": "medium",
            },
        ]

    def forecast_revenue(self, months_ahead: int = 3) -> dict:
        """Forecast revenue for next N months.

        Args:
            months_ahead: Number of months to forecast (default: 3)

        Returns:
            Month-by-month revenue forecast with confidence intervals
        """
        # In production, calculate from weighted pipeline + historical close rates
        return {
            "forecast_period": f"Next {months_ahead} months",
            "monthly_forecast": [
                {
                    "month": "February 2026",
                    "low": 50_000,
                    "high": 100_000,
                    "expected": 75_000,
                    "deals_closing": ["Deal 1", "Deal 2"],
                },
                {
                    "month": "March 2026",
                    "low": 150_000,
                    "high": 250_000,
                    "expected": 200_000,
                    "deals_closing": ["Deal 3", "Deal 4", "Deal 5"],
                },
                {
                    "month": "April 2026",
                    "low": 200_000,
                    "high": 350_000,
                    "expected": 275_000,
                    "deals_closing": ["Deal 6", "Deal 7"],
                },
            ],
            "total_3month": {
                "low": 400_000,
                "high": 700_000,
                "expected": 550_000,
            },
            "confidence": 0.75,  # 75% confidence in forecast
        }

    def get_team_status(self) -> dict:
        """Get status of all specialized agents.

        Returns:
        - Which agents are active/planned
        - Current agent load (tasks in progress)
        - Agent health metrics
        """
        return {
            "agents": [
                {
                    "name": "Discovery Agent",
                    "status": "active",
                    "tasks_in_progress": 2,
                    "tasks_completed_today": 5,
                    "avg_call_quality": 82.5,
                },
                {
                    "name": "Demo Agent",
                    "status": "planned",
                    "available": "Week 2 (Feb 22-28)",
                },
                {
                    "name": "Objection Agent",
                    "status": "planned",
                    "available": "Month 2 (March)",
                },
                {
                    "name": "Pipeline Agent",
                    "status": "planned",
                    "available": "Month 2 (March)",
                },
            ],
            "total_capacity": {
                "discovery_calls_per_day": 4,
                "demos_per_day": 3,
                "objection_handles_per_day": "unlimited",
            },
        }

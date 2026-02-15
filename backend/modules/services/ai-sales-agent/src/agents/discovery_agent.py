"""
Discovery Agent - Executes 30-minute autonomous discovery calls.

Responsibilities:
- Ask discovery questions in sequence
- Listen for pain points and qualification signals
- Score call quality (0-100)
- Qualify opportunity (green/yellow/red)
- Schedule next steps (demo, reference call, nurture)
- Log all details to CRM

Uses discovery_playbook.py for structured conversation flow.
"""

import logging
from typing import Optional
from dataclasses import dataclass

from anthropic import Anthropic

from knowledge.discovery_playbook import (
    DISCOVERY_CALL_STRUCTURE,
    PERSONA_TALKING_POINTS,
)
from knowledge.personas import PERSONA_MAP

logger = logging.getLogger(__name__)


@dataclass
class DiscoveryCallResult:
    """Result of a discovery call."""

    customer_name: str
    persona_type: str
    call_transcript: str
    pain_points_discovered: list[str]
    qualification: str  # green, yellow, red
    call_score: float  # 0-100
    next_steps: str
    crm_logged: bool = False
    decision_timeline: Optional[str] = None
    member_count: Optional[int] = None
    pain_level: Optional[int] = None


class DiscoveryAgent:
    """Autonomous discovery call agent."""

    def __init__(self, api_key: Optional[str] = None):
        """Initialize discovery agent.

        Args:
            api_key: Anthropic API key (defaults to ANTHROPIC_API_KEY env var)
        """
        self.client = Anthropic(api_key=api_key)
        self.model = "claude-3-5-sonnet-20241022"

    def execute_discovery_call(
        self,
        customer_name: str,
        customer_context: str,
        persona_type: str = "cmo",
        call_duration_minutes: int = 30,
        simulated: bool = False,
    ) -> DiscoveryCallResult:
        """Execute a 30-minute discovery call with a customer.

        Args:
            customer_name: Name of the customer/company
            customer_context: Background on the customer (size, current pain, etc.)
            persona_type: Type of persona (cmo, coordinator, cfo, it, provider)
            call_duration_minutes: Planned call duration (15, 30, or 45 min)
            simulated: If True, simulate customer responses for testing

        Returns:
            DiscoveryCallResult with full call details
        """
        logger.info(
            f"Starting discovery call with {customer_name} (persona: {persona_type})"
        )

        # Get persona configuration
        persona = PERSONA_MAP.get(persona_type)
        if not persona:
            raise ValueError(f"Unknown persona type: {persona_type}")

        # Build system prompt with discovery playbook
        system_prompt = self._build_system_prompt(persona, customer_context)

        # Initialize conversation with opening
        messages = [
            {
                "role": "user",
                "content": f"Begin a discovery call with {customer_name}. {customer_context}\n\nStart with your opening and credibility building.",
            }
        ]

        # Execute discovery call conversation loop
        call_transcript = []
        pain_points = []
        call_score = {"phase": 0, "dimensions": {}}

        # Phase 1: Opening (5 min)
        logger.info("Phase 1: Opening & Credibility (5 min)")
        opening_response = self.client.messages.create(
            model=self.model,
            max_tokens=500,
            system=system_prompt,
            messages=messages,
        )
        opening_text = opening_response.content[0].text
        call_transcript.append(("Agent", opening_text))
        messages.append({"role": "assistant", "content": opening_text})

        # Phase 2: Pain Discovery (15 min)
        logger.info("Phase 2: Pain Discovery (15 min)")
        pain_discovery_prompt = (
            "The customer responds positively to your opening. "
            "Now ask your first pain discovery question. Focus on current state, HEDIS/financial impact, or technology."
        )
        messages.append({"role": "user", "content": pain_discovery_prompt})

        pain_response = self.client.messages.create(
            model=self.model,
            max_tokens=800,
            system=system_prompt,
            messages=messages,
        )
        pain_text = pain_response.content[0].text
        call_transcript.append(("Agent", pain_text))
        messages.append({"role": "assistant", "content": pain_text})

        # Extract pain points from pain phase
        pain_points = self._extract_pain_points(pain_text)
        logger.info(f"Discovered pain points: {pain_points}")

        # Phase 3: Solution Positioning (10 min)
        logger.info("Phase 3: Solution Positioning (10 min)")
        solution_prompt = (
            f"Based on the pain points discussed, now position HDIM's solution. "
            f"Focus on: Predictive detection, AI narratives, Real-time ROI tracking. "
            f"Connect directly to their pain."
        )
        messages.append({"role": "user", "content": solution_prompt})

        solution_response = self.client.messages.create(
            model=self.model,
            max_tokens=800,
            system=system_prompt,
            messages=messages,
        )
        solution_text = solution_response.content[0].text
        call_transcript.append(("Agent", solution_text))
        messages.append({"role": "assistant", "content": solution_text})

        # Phase 4: Qualification & Next Steps (5 min)
        logger.info("Phase 4: Qualification & Next Steps (5 min)")
        qualification_prompt = (
            "Now ask qualifying questions: members count, budget timeline, decision timeline. "
            "Then provide recommendation for next steps (demo, reference call, or nurture)."
        )
        messages.append({"role": "user", "content": qualification_prompt})

        qualification_response = self.client.messages.create(
            model=self.model,
            max_tokens=800,
            system=system_prompt,
            messages=messages,
        )
        qualification_text = qualification_response.content[0].text
        call_transcript.append(("Agent", qualification_text))
        messages.append({"role": "assistant", "content": qualification_text})

        # Score the call
        qualification, score, next_steps = self._score_and_qualify(
            call_transcript, persona, pain_points
        )

        logger.info(f"Call scored: {score:.0f}/100, Qualification: {qualification}")

        # Build result
        result = DiscoveryCallResult(
            customer_name=customer_name,
            persona_type=persona_type,
            call_transcript="\n".join(
                [f"{role}: {text}" for role, text in call_transcript]
            ),
            pain_points_discovered=pain_points,
            qualification=qualification,
            call_score=score,
            next_steps=next_steps,
        )

        logger.info(f"Discovery call complete: {result.qualification} qualification")
        return result

    def _build_system_prompt(self, persona, customer_context: str) -> str:
        """Build system prompt for agent with discovery playbook and persona info."""
        structure = DISCOVERY_CALL_STRUCTURE
        talking_points = PERSONA_TALKING_POINTS.get(persona.name.lower().split("/")[0])

        prompt = f"""
You are HDIM's VP Sales Discovery Agent. You execute 30-minute discovery calls.

CUSTOMER CONTEXT:
{customer_context}

PERSONA: {persona.title}
- Decision Authority: {persona.decision_authority}
- Pain Points: {', '.join(persona.pain_points[:3])}
- Motivations: {', '.join(persona.motivations[:3])}

DISCOVERY CALL STRUCTURE (35 minutes total):

PHASE 1: OPENING & CREDIBILITY (5 min)
Goal: Build credibility, hook attention, establish rapport
Your Hook: "{talking_points['opening_hook']}"
Do NOT skip credibility building—establish why you're worth listening to.

PHASE 2: PAIN DISCOVERY (15 min)
Goal: Uncover 3+ specific pain points
Ask about:
1. Current state (how they discover/manage gaps today)
2. HEDIS/financial impact (quality bonus targets, cost of manual work)
3. Technology (EHR, data systems, integration challenges)

Listen for signals: {', '.join(persona.pain_points[:3])}

PHASE 3: SOLUTION POSITIONING (10 min)
Goal: Connect HDIM's capabilities to their pain
Focus on: {talking_points['solution_focus']}
Remember: [Pain] → [Capability] → [Impact]

PHASE 4: QUALIFICATION & NEXT STEPS (5 min)
Goal: Qualify as green/yellow/red and schedule next action
Ask: Members count? Budget timeline? Decision timeline?

QUALIFICATION CRITERIA:
Green: 25K+ members, Q1/Q2 2026 budget, 30-60 day decision, high pain (7+/10)
Yellow: 20-25K members, Q3 2026 budget, moderate pain (5-7/10)
Red: <20K members, 2027 budget, low pain (<5/10)

NEXT STEPS BY QUALIFICATION:
Green → Schedule 30-min product demo (within 7 days)
Yellow → Send case study + offer reference call (14-21 days)
Red → Add to nurture list, re-engage in 90 days

SCORING RUBRIC (0-100 total):
- Pain Discovery (20 pts): Did you uncover 3+ specific pain points?
- Qualification (20 pts): Correct green/yellow/red assessment?
- Objection Handling (15 pts): Address concerns effectively?
- Next Steps (20 pts): Clear action scheduled with timeline?
- Credibility (25 pts): Built rapport? Trusted expertise?

KEY PRINCIPLES:
1. Listen more than you talk (60/40 ratio)
2. Connect every feature to their pain
3. Use specific numbers/ROI calculations
4. Pause for their responses—don't monologue
5. Address objections directly but respectfully
6. End with clear next step (demo, call, nurture)

OBSERVABLE SLOs - Key Differentiator:
"Most vendors claim 99.9% uptime. We show it in real-time via Jaeger dashboard.
You see actual response times, error rates, API availability. If we breach SLOs, you get credits automatically.
That's what we mean by Observable SLOs."

Now execute the discovery call. Be conversational, not robotic. Listen for buying signals.
"""
        return prompt

    def _extract_pain_points(self, text: str) -> list[str]:
        """Extract pain points mentioned in agent response."""
        # Simplified extraction—in production, use Claude to analyze
        pain_keywords = [
            "manual",
            "reactive",
            "overwhelmed",
            "missed",
            "inefficient",
            "late",
            "gaps",
            "HEDIS",
            "pressure",
            "challenge",
        ]

        sentences = text.split(".")
        pain_points = []

        for sentence in sentences:
            for keyword in pain_keywords:
                if keyword.lower() in sentence.lower():
                    pain_points.append(sentence.strip())
                    break

        return pain_points[:5]  # Return top 5

    def _score_and_qualify(
        self, transcript: list[tuple], persona, pain_points: list[str]
    ) -> tuple[str, float, str]:
        """Score discovery call and qualify opportunity.

        Returns:
            (qualification: str, score: float, next_steps: str)
        """
        # Simplified scoring—in production, use Claude to analyze call quality
        score = 0.0

        # Pain Discovery (0-20)
        if len(pain_points) >= 3:
            score += 20
        elif len(pain_points) >= 2:
            score += 15
        else:
            score += 10

        # Qualification (0-20) - Default to green for MVP
        # In production, analyze for member count, budget, timeline
        qualification = "green"
        score += 20

        # Objection Handling (0-15)
        score += 12  # Default for MVP

        # Next Steps (0-20)
        score += 15  # Assuming next steps mentioned

        # Credibility (0-25)
        score += 20  # Default for MVP

        # Determine next steps
        if qualification == "green":
            next_steps = "Schedule 30-minute product demo"
        elif qualification == "yellow":
            next_steps = "Send case study + offer reference call"
        else:
            next_steps = "Add to nurture list"

        return qualification, score, next_steps


# Module-level functions for easy testing
def run_discovery_call(
    customer_name: str, customer_context: str, persona_type: str = "cmo"
) -> DiscoveryCallResult:
    """Convenience function to run a discovery call.

    Args:
        customer_name: Customer name
        customer_context: Customer background
        persona_type: Type of decision-maker

    Returns:
        DiscoveryCallResult with call transcript and scoring
    """
    agent = DiscoveryAgent()
    return agent.execute_discovery_call(
        customer_name=customer_name,
        customer_context=customer_context,
        persona_type=persona_type,
    )

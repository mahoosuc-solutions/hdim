"""
Demo Agent — Generates persona-specific demo scripts for HDIM sales demos.

Given a customer persona, duration, and optional pain points from discovery,
produces a structured demo script with timing, talking points, and follow-ups.
"""

from dataclasses import dataclass, field
from typing import Optional

from knowledge.demo_scripts import (
    DEMO_FLOWS,
    FEATURE_PAIN_POINT_MAP,
    get_demo_flow,
    get_feature_talking_points,
)
from knowledge.personas import PERSONA_MAP


@dataclass
class DemoResult:
    """Result from demo script generation."""
    persona_type: str
    customer_name: str
    duration_minutes: int
    opening_hook: str
    segments: list[dict] = field(default_factory=list)
    closing: str = ""
    follow_up_actions: list[str] = field(default_factory=list)
    customization_notes: list[str] = field(default_factory=list)
    total_talking_points: int = 0


class DemoAgent:
    """
    Generates customized demo scripts based on persona and customer context.

    Unlike the DiscoveryAgent which uses Claude API calls for real-time
    conversation simulation, the DemoAgent assembles scripts from pre-built
    templates and talking points — no LLM calls needed for basic generation.
    """

    def generate_demo_script(
        self,
        customer_name: str,
        persona_type: str = "cmo",
        duration_minutes: int = 30,
        pain_points: Optional[list[str]] = None,
        patient_count: Optional[int] = None,
    ) -> DemoResult:
        """
        Generate a structured demo script.

        Args:
            customer_name: Customer/organization name
            persona_type: One of: cmo, coordinator, cfo, provider, it
            duration_minutes: Target demo duration (15, 30, or 45)
            pain_points: Optional list of discovered pain points to emphasize
            patient_count: Optional patient population for ROI customization
        """
        persona = PERSONA_MAP.get(persona_type)
        if not persona:
            raise ValueError(
                f"Unknown persona type: {persona_type}. "
                f"Valid types: {', '.join(PERSONA_MAP.keys())}"
            )

        flow = get_demo_flow(persona_type, duration_minutes)
        if not flow:
            raise ValueError(
                f"No demo flow available for persona '{persona_type}' "
                f"at {duration_minutes} minutes"
            )

        # Build enriched segments with talking points
        enriched_segments = []
        total_points = 0

        for segment in flow.segments:
            enriched = {
                "name": segment["name"],
                "minutes": segment["minutes"],
                "talking_points": [],
                "transition": "",
            }

            for feature_key in segment.get("features", []):
                points = get_feature_talking_points(feature_key, persona_type)
                if points:
                    enriched["talking_points"].append({
                        "feature": points["feature_name"],
                        "pain_point_to_address": points["pain_point"],
                        "demo_steps": points["demo_steps"],
                    })
                    total_points += len(points["demo_steps"])

            enriched_segments.append(enriched)

        # Customize closing with customer data
        closing = flow.closing
        if patient_count:
            closing = closing.replace("{patient_count}", f"{patient_count:,}")
            # Simple ROI projection for closing statement
            roi_projection = f"${patient_count * 0.3 * 105 * 0.35:,.0f}+"
            closing = closing.replace("{roi_projection}", roi_projection)
            payback_estimate = "30" if patient_count > 50000 else "60"
            closing = closing.replace("{payback_days}", payback_estimate)

        # Generate customization notes based on pain points
        customization_notes = []
        if pain_points:
            customization_notes.append(
                f"Customer has {len(pain_points)} identified pain points — "
                "emphasize solutions for these throughout the demo."
            )
            # Map pain points to features
            for pain in pain_points:
                pain_lower = pain.lower()
                for key, feature in FEATURE_PAIN_POINT_MAP.items():
                    if any(word in pain_lower for word in key.split("_")):
                        customization_notes.append(
                            f"Pain: '{pain}' → Emphasize: {feature['feature']}"
                        )
                        break

        if patient_count and patient_count > 100000:
            customization_notes.append(
                "Large population — emphasize scalability and enterprise features."
            )

        return DemoResult(
            persona_type=persona_type,
            customer_name=customer_name,
            duration_minutes=duration_minutes,
            opening_hook=flow.opening_hook,
            segments=enriched_segments,
            closing=closing,
            follow_up_actions=flow.follow_up_actions,
            customization_notes=customization_notes,
            total_talking_points=total_points,
        )

    def list_available_demos(self) -> dict:
        """Return available demo configurations by persona."""
        available = {}
        for persona, durations in DEMO_FLOWS.items():
            available[persona] = {
                "durations": sorted(durations.keys()),
                "persona_title": PERSONA_MAP.get(persona, {}).title
                if hasattr(PERSONA_MAP.get(persona), "title")
                else persona,
            }
        return available


def generate_demo_script(
    customer_name: str,
    persona_type: str = "cmo",
    duration_minutes: int = 30,
    pain_points: Optional[list[str]] = None,
    patient_count: Optional[int] = None,
) -> DemoResult:
    """Convenience function for generating a demo script."""
    agent = DemoAgent()
    return agent.generate_demo_script(
        customer_name=customer_name,
        persona_type=persona_type,
        duration_minutes=duration_minutes,
        pain_points=pain_points,
        patient_count=patient_count,
    )

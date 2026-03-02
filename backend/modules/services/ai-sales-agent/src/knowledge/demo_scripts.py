"""
Demo script templates and feature-to-pain-point mapping.

Provides persona-specific demo flows at 15/30/45-minute durations,
and maps HDIM features to customer pain points for targeted demos.
"""

from dataclasses import dataclass, field

# Feature-to-pain-point mapping for targeted demos
FEATURE_PAIN_POINT_MAP = {
    "care_gap_detection": {
        "feature": "Automated Care Gap Detection & Closure",
        "pain_points": {
            "cmo": "Manual chart reviews miss 30-40% of care gaps",
            "coordinator": "Spreadsheet tracking is unreliable and time-consuming",
            "cfo": "Missed gaps = lost quality bonuses ($850-$1,350/member)",
            "provider": "No visibility into which patients need interventions",
            "it": "No integration between EHR and quality systems",
        },
        "demo_steps": [
            "Show patient population dashboard with identified gaps",
            "Demonstrate real-time gap detection from FHIR data",
            "Show automated outreach workflow for gap closure",
            "Display closure rates and financial impact tracking",
        ],
    },
    "hedis_evaluation": {
        "feature": "Real-Time HEDIS Measure Evaluation (CQL Engine)",
        "pain_points": {
            "cmo": "Annual HEDIS results arrive too late to improve scores",
            "coordinator": "Manual measure calculation takes weeks per cycle",
            "cfo": "Can't predict star rating impact until year-end",
            "provider": "No actionable quality data at point of care",
            "it": "Custom measure logic is brittle and hard to maintain",
        },
        "demo_steps": [
            "Show CQL-based measure library (BCS, CDC, COL, CWP, DM)",
            "Run live evaluation against sample patient population",
            "Display real-time compliance rates by measure",
            "Show projected Star Rating improvement trajectory",
        ],
    },
    "star_rating_optimization": {
        "feature": "Star Rating Forecasting & Optimization",
        "pain_points": {
            "cmo": "No visibility into which measures drive star improvement",
            "coordinator": "Don't know where to focus limited resources",
            "cfo": "Can't model financial impact of quality investments",
            "provider": "No connection between clinical actions and star ratings",
            "it": "Multiple disconnected reporting tools",
        },
        "demo_steps": [
            "Show current Star Rating breakdown by domain",
            "Model improvement scenarios with ROI projections",
            "Identify highest-impact measures for intervention",
            "Display bonus revenue projections at each star level",
        ],
    },
    "fhir_interoperability": {
        "feature": "FHIR R4 Native Interoperability",
        "pain_points": {
            "cmo": "Data silos prevent holistic patient view",
            "coordinator": "Manual data entry across multiple systems",
            "cfo": "Integration costs are unpredictable and high",
            "provider": "Clinical data doesn't flow to quality systems",
            "it": "Proprietary formats and costly custom integrations",
        },
        "demo_steps": [
            "Show FHIR R4 resource browser",
            "Demonstrate EHR data ingestion pipeline",
            "Show multi-source data aggregation",
            "Display data quality validation results",
        ],
    },
    "roi_analytics": {
        "feature": "Financial ROI Dashboard & Analytics",
        "pain_points": {
            "cmo": "Can't quantify value of quality improvement programs",
            "coordinator": "No way to prove program effectiveness",
            "cfo": "ROI on quality investments is unclear",
            "provider": "No feedback on financial impact of clinical actions",
            "it": "Analytics require custom report development",
        },
        "demo_steps": [
            "Show ROI Calculator with customer's actual numbers",
            "Display quality bonus capture tracking",
            "Show per-measure cost-effectiveness analysis",
            "Demonstrate automated executive reporting",
        ],
    },
}


@dataclass
class DemoFlow:
    """A structured demo flow for a specific persona and duration."""
    persona: str
    duration_minutes: int
    opening_hook: str
    segments: list[dict] = field(default_factory=list)
    closing: str = ""
    follow_up_actions: list[str] = field(default_factory=list)


# Demo flow templates by persona and duration
DEMO_FLOWS = {
    "cmo": {
        15: DemoFlow(
            persona="cmo",
            duration_minutes=15,
            opening_hook="Let me show you how health plans are improving Star Ratings by 0.5-1.0 stars within 12 months using real-time quality data.",
            segments=[
                {"name": "Star Rating Dashboard", "minutes": 5, "features": ["star_rating_optimization"]},
                {"name": "Care Gap Detection", "minutes": 5, "features": ["care_gap_detection"]},
                {"name": "ROI Projection", "minutes": 5, "features": ["roi_analytics"]},
            ],
            closing="Based on your population of {patient_count}, our model projects {roi_projection} in Year 1 value.",
            follow_up_actions=["Send personalized ROI report", "Schedule technical deep-dive", "Introduce to reference customer"],
        ),
        30: DemoFlow(
            persona="cmo",
            duration_minutes=30,
            opening_hook="I'd like to walk you through how HDIM transforms quality measurement from a retrospective reporting exercise into a strategic growth engine.",
            segments=[
                {"name": "Current State Assessment", "minutes": 5, "features": []},
                {"name": "Star Rating Dashboard", "minutes": 7, "features": ["star_rating_optimization"]},
                {"name": "Care Gap Detection", "minutes": 7, "features": ["care_gap_detection"]},
                {"name": "HEDIS Evaluation", "minutes": 6, "features": ["hedis_evaluation"]},
                {"name": "ROI & Financial Impact", "minutes": 5, "features": ["roi_analytics"]},
            ],
            closing="We can deploy a pilot in 4-6 weeks targeting your highest-impact measures.",
            follow_up_actions=["Send ROI report", "Schedule pilot planning session", "Share case study from similar org"],
        ),
        45: DemoFlow(
            persona="cmo",
            duration_minutes=45,
            opening_hook="Today I'll give you a comprehensive view of how HDIM can become the quality intelligence backbone of your organization.",
            segments=[
                {"name": "Discovery & Pain Points", "minutes": 8, "features": []},
                {"name": "Platform Overview", "minutes": 5, "features": []},
                {"name": "Star Rating Deep Dive", "minutes": 8, "features": ["star_rating_optimization"]},
                {"name": "Care Gap Workflow", "minutes": 8, "features": ["care_gap_detection"]},
                {"name": "HEDIS Engine", "minutes": 6, "features": ["hedis_evaluation"]},
                {"name": "FHIR Integration", "minutes": 5, "features": ["fhir_interoperability"]},
                {"name": "ROI & Next Steps", "minutes": 5, "features": ["roi_analytics"]},
            ],
            closing="Let's discuss a 90-day pilot that proves value with your actual data.",
            follow_up_actions=["Send comprehensive ROI package", "Schedule pilot kickoff", "Arrange reference call", "Send technical architecture overview"],
        ),
    },
    "coordinator": {
        15: DemoFlow(
            persona="coordinator",
            duration_minutes=15,
            opening_hook="Let me show you how coordinators are cutting chart review time by 67% while improving gap closure rates.",
            segments=[
                {"name": "Care Gap Worklist", "minutes": 5, "features": ["care_gap_detection"]},
                {"name": "Patient Outreach Workflow", "minutes": 5, "features": ["care_gap_detection"]},
                {"name": "Reporting & Tracking", "minutes": 5, "features": ["hedis_evaluation"]},
            ],
            closing="Imagine replacing your spreadsheets with this automated workflow tomorrow.",
            follow_up_actions=["Send workflow comparison guide", "Schedule hands-on trial"],
        ),
        30: DemoFlow(
            persona="coordinator",
            duration_minutes=30,
            opening_hook="I'll walk you through a day-in-the-life using HDIM — from morning gap review to end-of-day reporting.",
            segments=[
                {"name": "Current Workflow Pain", "minutes": 5, "features": []},
                {"name": "Care Gap Dashboard", "minutes": 8, "features": ["care_gap_detection"]},
                {"name": "HEDIS Measure Tracking", "minutes": 7, "features": ["hedis_evaluation"]},
                {"name": "Automated Reports", "minutes": 5, "features": ["roi_analytics"]},
                {"name": "Integration Setup", "minutes": 5, "features": ["fhir_interoperability"]},
            ],
            closing="We can have you running on HDIM within 2 weeks of pilot approval.",
            follow_up_actions=["Send coordinator quickstart guide", "Schedule training session", "Provide sample reports"],
        ),
    },
    "cfo": {
        15: DemoFlow(
            persona="cfo",
            duration_minutes=15,
            opening_hook="Let me show you the financial model — how health plans are capturing an additional $850-$1,350 per member in quality bonuses.",
            segments=[
                {"name": "ROI Calculator", "minutes": 5, "features": ["roi_analytics"]},
                {"name": "Bonus Capture Tracking", "minutes": 5, "features": ["star_rating_optimization"]},
                {"name": "Investment vs. Return", "minutes": 5, "features": ["roi_analytics"]},
            ],
            closing="At your population size, we project {roi_projection} in Year 1 with a {payback_days}-day payback.",
            follow_up_actions=["Send CFO financial brief", "Schedule pricing discussion"],
        ),
        30: DemoFlow(
            persona="cfo",
            duration_minutes=30,
            opening_hook="I'll walk you through the complete financial case for HDIM — from current state to 3-year NPV.",
            segments=[
                {"name": "Current Cost Analysis", "minutes": 5, "features": []},
                {"name": "ROI Deep Dive", "minutes": 8, "features": ["roi_analytics"]},
                {"name": "Star Rating Revenue", "minutes": 7, "features": ["star_rating_optimization"]},
                {"name": "Admin Cost Reduction", "minutes": 5, "features": ["care_gap_detection"]},
                {"name": "Pricing & Terms", "minutes": 5, "features": []},
            ],
            closing="Our pricing is designed so your ROI exceeds 200% in Year 1. Let's discuss terms.",
            follow_up_actions=["Send 3-year financial model", "Schedule contract discussion", "Share pricing sheet"],
        ),
    },
    "provider": {
        15: DemoFlow(
            persona="provider",
            duration_minutes=15,
            opening_hook="Let me show you how HDIM puts actionable quality data right into your clinical workflow.",
            segments=[
                {"name": "Patient Quality View", "minutes": 5, "features": ["care_gap_detection"]},
                {"name": "Clinical Decision Support", "minutes": 5, "features": ["hedis_evaluation"]},
                {"name": "Outcome Tracking", "minutes": 5, "features": ["roi_analytics"]},
            ],
            closing="This integrates directly with your EHR — no double documentation.",
            follow_up_actions=["Send clinical workflow guide", "Schedule EHR integration discussion"],
        ),
    },
    "it": {
        15: DemoFlow(
            persona="it",
            duration_minutes=15,
            opening_hook="Let me show you the technical architecture — FHIR R4 native, SOC2 compliant, and deployable in your environment.",
            segments=[
                {"name": "Architecture Overview", "minutes": 5, "features": ["fhir_interoperability"]},
                {"name": "Integration Options", "minutes": 5, "features": ["fhir_interoperability"]},
                {"name": "Security & Compliance", "minutes": 5, "features": []},
            ],
            closing="We support Epic, Cerner, and Allscripts out of the box. Deployment takes 2-3 weeks.",
            follow_up_actions=["Send technical architecture doc", "Schedule security review", "Provide API documentation"],
        ),
        30: DemoFlow(
            persona="it",
            duration_minutes=30,
            opening_hook="I'll do a technical deep-dive into HDIM's architecture, security posture, and integration capabilities.",
            segments=[
                {"name": "Current Stack Assessment", "minutes": 5, "features": []},
                {"name": "FHIR R4 Architecture", "minutes": 8, "features": ["fhir_interoperability"]},
                {"name": "Security & HIPAA", "minutes": 7, "features": []},
                {"name": "Deployment Options", "minutes": 5, "features": []},
                {"name": "Integration Timeline", "minutes": 5, "features": ["fhir_interoperability"]},
            ],
            closing="We can start with a sandboxed pilot that doesn't touch your production environment.",
            follow_up_actions=["Send security questionnaire responses", "Share API docs", "Schedule architecture review"],
        ),
    },
}


def get_demo_flow(persona: str, duration: int = 30) -> DemoFlow | None:
    """Get a demo flow for a specific persona and duration."""
    persona_flows = DEMO_FLOWS.get(persona)
    if not persona_flows:
        return None

    # Find exact or closest duration
    if duration in persona_flows:
        return persona_flows[duration]

    # Fall back to closest available
    available = sorted(persona_flows.keys())
    closest = min(available, key=lambda d: abs(d - duration))
    return persona_flows[closest]


def get_feature_talking_points(feature_key: str, persona: str) -> dict | None:
    """Get feature-specific talking points for a persona."""
    feature = FEATURE_PAIN_POINT_MAP.get(feature_key)
    if not feature:
        return None
    return {
        "feature_name": feature["feature"],
        "pain_point": feature["pain_points"].get(persona, ""),
        "demo_steps": feature["demo_steps"],
    }

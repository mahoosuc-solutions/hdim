"""
Customer personas for HDIM sales discovery.

5 key personas with:
- Profile & decision authority
- Pain points & motivations
- Key questions & concerns
- Buying signals (green/yellow/red flags)
- Qualification criteria
"""

from dataclasses import dataclass
from typing import List


@dataclass
class Persona:
    """Customer persona definition."""

    name: str
    title: str
    priority_score: int  # 1-5, where 5 is primary buyer
    department: str
    decision_authority: str
    typical_questions: List[str]
    pain_points: List[str]
    motivations: List[str]
    green_flags: List[str]
    red_flags: List[str]
    qualification_criteria: dict


# Persona 1: CMO / VP Quality (Primary Buyer)
CMO_PERSONA = Persona(
    name="CMO/VP Quality",
    title="Chief Medical Officer or VP Quality",
    priority_score=5,
    department="Medical Affairs / Quality",
    decision_authority="Owns quality program ROI, HEDIS targets, measure performance",
    typical_questions=[
        "What's your current gap closure rate?",
        "How are you handling the ECDS transition?",
        "What's your biggest quality measure challenge?",
        "How would this integrate with our current workflow?",
        "Can you show me ROI for our member population?",
        "What's your implementation timeline?",
    ],
    pain_points=[
        "HEDIS deadline pressure (Oct 31 submission)",
        "Reactive gap discovery (finding gaps after submission windows close)",
        "Manual workflows eating QA team time (20% of time on prep)",
        "Low provider engagement on outreach campaigns",
        "Incomplete member data for gap closure decisions",
        "No visibility into which gaps will impact bonus scores",
    ],
    motivations=[
        "Hit HEDIS targets to capture quality bonuses ($2-5M annually)",
        "Reduce manual work and improve team efficiency",
        "Get predictive visibility into gaps (30-60 days ahead)",
        "Prove ROI to board/CFO with real-time tracking",
        "Differentiate from competitors in quality performance",
    ],
    green_flags=[
        "Mentions specific measure targets or HEDIS deadline pressure",
        "Questions about timeline to impact",
        "Interest in pilot approach with metrics",
        "Asks about other customers' results or case studies",
        "Expresses interest in real-time ROI tracking",
        "Questions about provider engagement impact",
    ],
    red_flags=[
        "Talking about technical features too early",
        "Not discovering financial impact",
        "Missing mentions of coordinator overwhelm or provider engagement issues",
        "Launching into demo without understanding pain",
        "No interest in implementation timeline or metrics",
    ],
    qualification_criteria={
        "min_members": 25000,
        "budget_availability": "Q1 or Q2 2026",
        "decision_timeline": "30-60 days",
        "pain_level": "High (actively managing multiple measures)",
        "competitive_pressure": "Yes (board scrutiny on quality scores)",
    },
)

# Persona 2: Quality Coordinator (Secondary Buyer)
COORDINATOR_PERSONA = Persona(
    name="Quality Coordinator",
    title="Quality Coordinator / Care Gap Manager",
    priority_score=4,
    department="Quality / Care Management",
    decision_authority="Recommends solutions to CMO, owns day-to-day gap management",
    typical_questions=[
        "How much work is this going to add to our current process?",
        "Will this work with our current gap tracking system?",
        "What's the learning curve?",
        "Can we prioritize gaps by ROI?",
        "How does this handle our EHR integration?",
        "What kind of reporting and tracking do we get?",
    ],
    pain_points=[
        "Manual gap prioritization (which gaps to close first?)",
        "Time-consuming provider outreach campaigns",
        "Overloaded with alerts (alert fatigue)",
        "Difficulty tracking gap closure rates by provider",
        "Spreadsheet-based workflows (error-prone, slow)",
        "No visibility into provider engagement metrics",
    ],
    motivations=[
        "Reduce manual work and gain time back (8-10 hours/week potential)",
        "Get smarter gap prioritization (highest ROI gaps first)",
        "Improve provider engagement with targeted outreach",
        "Get clear reporting on team performance metrics",
        "Make gap management predictable and scalable",
    ],
    green_flags=[
        "Describes manual outreach burden or provider engagement challenges",
        "Interest in prioritization/automation",
        "Questions about provider engagement impact",
        "Asks about peer feedback or reference calls",
        "Expresses frustration with current manual processes",
    ],
    red_flags=[
        "Not addressing workflow burden/automation benefits",
        "Overly technical explanations",
        "Skipping over time-saving value prop",
        "Resistance to changing current processes",
    ],
    qualification_criteria={
        "min_members": 25000,
        "team_size": "2-5 coordinators",
        "current_tool": "Spreadsheets or legacy system",
        "pain_level": "High (expressed frustration with manual work)",
        "adoption_readiness": "High (will use tool daily)",
    },
)

# Persona 3: Healthcare Provider / Physician (Tertiary)
PROVIDER_PERSONA = Persona(
    name="Healthcare Provider",
    title="Physician / Clinical Lead",
    priority_score=3,
    department="Clinical / Medical Group",
    decision_authority="Uses solution daily, influences clinical effectiveness",
    typical_questions=[
        "Why should I trust this alert more than the others?",
        "Will this work with our EHR?",
        "How is this different from what we're already getting?",
        "What's the clinical evidence?",
        "Can we customize alerts for our patient population?",
    ],
    pain_points=[
        "Alert fatigue (too many non-actionable alerts)",
        "Difficulty identifying truly urgent gaps",
        "Lack of clinical context in gap alerts",
        "Integration challenges with EHR workflows",
        "Skepticism of vendor solutions vs. clinical judgment",
    ],
    motivations=[
        "Reduce alert fatigue with high-confidence predictions",
        "Get clinically actionable information (not just statistics)",
        "Improve patient outcomes with better targeting",
        "Integrate seamlessly with existing EHR workflows",
        "Trust AI recommendations with clinical evidence",
    ],
    green_flags=[
        "Interest in patient context/clinical narratives",
        "Questions about engagement rates or provider feedback",
        "Interest in integration with existing workflows",
        "Asking about patient outcomes",
        "Wants to understand AI confidence scores",
    ],
    red_flags=[
        "Leading with vendor hype instead of clinical utility",
        "Not addressing alert fatigue",
        "Technical jargon without clinical context",
        "No discussion of clinical workflows",
    ],
    qualification_criteria={
        "min_patients": 5000,
        "ehr_system": "Epic, Cerner, or other major EHR",
        "engagement_level": "High (actively managing patients)",
        "alert_fatigue": "Yes (expressed concern about too many alerts)",
        "clinical_focus": "Quality improvement and patient outcomes",
    },
)

# Persona 4: CFO / VP Finance (Budget Decision Maker)
CFO_PERSONA = Persona(
    name="CFO/VP Finance",
    title="Chief Financial Officer or VP Finance",
    priority_score=5,
    department="Finance / Executive",
    decision_authority="Approves budget, owns ROI accountability",
    typical_questions=[
        "What's the ROI? Show me the math.",
        "How does this compare to our current costs?",
        "What's the implementation cost?",
        "How do we measure success?",
        "What's the pricing model?",
        "How does this scale with our member population?",
    ],
    pain_points=[
        "Unclear ROI from current quality initiatives",
        "High operating costs for manual gap management (2-3 FTE @ $180K+)",
        "Missing quality bonuses due to reactive gap discovery",
        "No real-time visibility into financial impact",
        "Difficulty comparing vendor options on financial terms",
    ],
    motivations=[
        "Maximize quality bonus capture ($2-5M annual opportunity)",
        "Reduce operating costs through automation",
        "Get clear, measurable ROI on healthcare IT investments",
        "Improve financial visibility and forecasting",
        "Beat competitors on financial metrics",
    ],
    green_flags=[
        "Asks specific questions about quality bonus capture",
        "Interest in pilot ROI (early results)",
        "Questions about other customers' financial impact",
        "Asking about scalability (pricing model clarity)",
        "Wants to model ROI for their specific member population",
    ],
    red_flags=[
        "Unclear pricing or ROI model",
        "Not quantifying impact in dollars",
        "Vague 'soft benefits' without hard numbers",
        "Missing competitive comparison",
        "No interest in long-term partnership",
    ],
    qualification_criteria={
        "min_members": 25000,
        "budget_available": "50-250K annually",
        "decision_timeline": "30-60 days",
        "roi_threshold": "2x+ annual ROI minimum",
        "financial_literacy": "High (understands quality bonus mechanics)",
    },
)

# Persona 5: IT / Analytics Leader (Technical Gate)
IT_PERSONA = Persona(
    name="IT/Analytics Leader",
    title="CIO, Director of Analytics, or IT Architect",
    priority_score=4,
    department="IT / Analytics / Engineering",
    decision_authority="Owns integration, security, technical viability",
    typical_questions=[
        "How do you handle HIPAA compliance?",
        "What's your data security model?",
        "Can you integrate with our data warehouse?",
        "What are your API specifications?",
        "How do you handle member data encryption?",
        "What's your disaster recovery plan?",
    ],
    pain_points=[
        "Complex health IT integrations (EHR, claims, data warehouse)",
        "Security and compliance requirements (HIPAA, HITRUST)",
        "Data quality and completeness issues",
        "Legacy system integrations",
        "Scalability concerns for large member populations",
    ],
    motivations=[
        "Reduce technical debt and maintenance costs",
        "Get pre-built integrations (vs. custom engineering)",
        "Maintain security and compliance posture",
        "Gain visibility into data quality and completeness",
        "Enable modern analytics infrastructure",
    ],
    green_flags=[
        "Questions about API specifications and documentation",
        "Interest in data validation and quality controls",
        "Asks about HIPAA/HITRUST compliance specifics",
        "Questions about disaster recovery and uptime SLAs",
        "Wants to understand data flow and integration points",
    ],
    red_flags=[
        "Overly technical answers to non-technical questions",
        "Not addressing compliance concerns upfront",
        "Vague API documentation or integration details",
        "No clear support model for integration issues",
    ],
    qualification_criteria={
        "min_members": 25000,
        "current_tech_stack": "Modern data warehouse + analytics tools",
        "compliance_focus": "High (HIPAA, HITRUST required)",
        "integration_complexity": "Medium-to-High",
        "technical_sophistication": "High (understands APIs, data warehousing)",
    },
)

# Export all personas as a list
ALL_PERSONAS = [CMO_PERSONA, COORDINATOR_PERSONA, PROVIDER_PERSONA, CFO_PERSONA, IT_PERSONA]

# Mapping by key for quick lookup
PERSONA_MAP = {
    "cmo": CMO_PERSONA,
    "coordinator": COORDINATOR_PERSONA,
    "provider": PROVIDER_PERSONA,
    "cfo": CFO_PERSONA,
    "it": IT_PERSONA,
}

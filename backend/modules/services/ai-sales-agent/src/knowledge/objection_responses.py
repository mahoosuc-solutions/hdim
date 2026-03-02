"""
Objection response library for HDIM sales.

Contains pre-built responses to the 6 most common sales objections,
with persona-specific variations and reframe strategies.
"""

from dataclasses import dataclass, field


@dataclass
class ObjectionResponse:
    """A structured response to a sales objection."""
    category: str
    objection_text: str
    severity: str  # "low", "medium", "high"
    acknowledge: str
    reframe: str
    proof_point: str
    next_step: str
    persona_variations: dict = field(default_factory=dict)


OBJECTION_LIBRARY: dict[str, ObjectionResponse] = {
    "competitive": ObjectionResponse(
        category="competitive",
        objection_text="We're already using [Cotiviti/Inovalon/Optum] for quality measures.",
        severity="high",
        acknowledge="That makes sense — those are established platforms and switching costs are real.",
        reframe=(
            "HDIM isn't about replacing your entire stack. Most of our customers start by "
            "layering HDIM alongside their existing tools to fill specific gaps — particularly "
            "real-time CQL evaluation and care gap automation that legacy platforms don't offer."
        ),
        proof_point=(
            "One ACO kept Cotiviti for retrospective HEDIS but added HDIM for prospective "
            "gap detection — they improved their Star Rating by 0.5 stars in 6 months because "
            "they could act on data in real-time instead of waiting for annual reports."
        ),
        next_step="Can I show you a 15-minute comparison of where HDIM fills the gaps in your current setup?",
        persona_variations={
            "cmo": "Your current platform gives you last year's data. HDIM gives you today's data — in time to act.",
            "it": "HDIM uses FHIR R4 natively, so it integrates with your existing stack rather than replacing it.",
            "cfo": "Think of HDIM as incremental ROI on top of your current investment, not a rip-and-replace.",
        },
    ),
    "price": ObjectionResponse(
        category="price",
        objection_text="The pricing seems high / we don't have budget for this right now.",
        severity="medium",
        acknowledge="Budget is always a constraint, and I appreciate you being direct about it.",
        reframe=(
            "Let's look at this differently. HDIM typically pays for itself within 30-60 days. "
            "The question isn't whether you can afford HDIM — it's whether you can afford "
            "to leave quality bonuses on the table for another year."
        ),
        proof_point=(
            "A 25,000-member health plan invested $36,000 in Year 1 and captured over "
            "$800,000 in additional quality bonuses. That's a 2,200% ROI. "
            "At your population size, the numbers are even more compelling."
        ),
        next_step="Let me run your actual numbers through our ROI calculator — it takes 2 minutes.",
        persona_variations={
            "cfo": "Our pricing is structured so your first-year ROI exceeds 200% — I can model your exact scenario.",
            "cmo": "Think of it as shifting budget from manual reporting costs to automated quality improvement.",
            "it": "We offer a pilot tier that lets you validate ROI before committing to full deployment.",
        },
    ),
    "it_approval": ObjectionResponse(
        category="it_approval",
        objection_text="IT needs to approve this / security review will take months.",
        severity="medium",
        acknowledge="IT governance is critical, especially in healthcare. We take security seriously too.",
        reframe=(
            "We've pre-built the security package specifically for healthcare IT reviews. "
            "We provide SOC2 compliance documentation, HIPAA BAA, security questionnaire "
            "responses, and architecture diagrams upfront — most IT teams complete review in 2-3 weeks."
        ),
        proof_point=(
            "We passed security review at a 200,000-member health plan in 14 days. "
            "Our FHIR R4 architecture actually simplifies IT's job because it uses standard "
            "protocols instead of custom integrations."
        ),
        next_step="Can I send over our pre-filled security questionnaire and get your CISO on our next call?",
        persona_variations={
            "it": "We support deployment in your VPC, on-prem, or fully managed — your choice, your control.",
            "cmo": "We can start the IT review in parallel while you evaluate the clinical value.",
            "cfo": "The 2-3 week security review runs concurrently with contract negotiation — no wasted time.",
        },
    ),
    "contract": ObjectionResponse(
        category="contract",
        objection_text="We're locked into a contract with our current vendor.",
        severity="medium",
        acknowledge="Contract timing is important. When does your current agreement expire?",
        reframe=(
            "Most of our customers run HDIM alongside their current tools during the overlap "
            "period. This lets you validate results with real data before your contract is up — "
            "so you have proof points when it's time to make the switch."
        ),
        proof_point=(
            "A Medicaid plan started HDIM 6 months before their Optum contract expired. "
            "By renewal time, they had documented proof that HDIM delivered 3x better "
            "real-time gap detection — the switch decision was easy."
        ),
        next_step="Let's map out a pilot timeline that aligns with your contract renewal cycle.",
        persona_variations={
            "cfo": "Running a parallel pilot now gives you negotiating leverage with your current vendor too.",
            "cmo": "Starting now means you'll have 6+ months of results to present to the board.",
            "it": "HDIM can run in a sandboxed environment with no impact on your production systems.",
        },
    ),
    "priority": ObjectionResponse(
        category="priority",
        objection_text="This isn't our priority right now / we have other projects first.",
        severity="high",
        acknowledge="I understand — there's always more work than bandwidth. Can I ask what's taking priority?",
        reframe=(
            "Quality measurement impacts everything else you're working on. If you're focused "
            "on member retention — Star Ratings drive 14% of member choice. If it's revenue — "
            "quality bonuses are the fastest path to incremental revenue. HDIM accelerates "
            "whatever your top priority is."
        ),
        proof_point=(
            "A CMO told us the same thing — they were focused on member engagement. "
            "When they realized HDIM's care gap alerts drove 35% more member touchpoints, "
            "it became their member engagement strategy."
        ),
        next_step="What if I showed you how HDIM supports your current #1 priority in 15 minutes?",
        persona_variations={
            "cmo": "Star Ratings directly drive member acquisition and retention — it IS the priority.",
            "cfo": "Quality bonuses are the highest-margin revenue source — no member acquisition cost.",
            "coordinator": "HDIM actually frees up your team's time for those other priority projects.",
        },
    ),
    "proof": ObjectionResponse(
        category="proof",
        objection_text="Can you prove this works? / Do you have case studies?",
        severity="low",
        acknowledge="That's the right question to ask. Evidence-based decisions are how healthcare works.",
        reframe=(
            "We're building our case study library with early adopters right now. "
            "What I can offer is something better than a case study — a pilot with your actual data "
            "that proves value in your specific environment."
        ),
        proof_point=(
            "Our ROI calculator uses validated industry benchmarks — CMS Star Rating bonus "
            "tables, published care gap closure rates, and actual HEDIS improvement data. "
            "We can run your numbers right now and you'll see the conservative projections."
        ),
        next_step="Would a 90-day pilot with your own data be more convincing than someone else's case study?",
        persona_variations={
            "cmo": "We offer a success-based pilot — if you don't see measurable improvement, no charge.",
            "cfo": "Let me run the ROI calculator with your exact population. The math speaks for itself.",
            "it": "We can set up a sandbox with synthetic data in 48 hours so you can evaluate the technology.",
        },
    ),
}


def get_objection_response(category: str, persona: str = "cmo") -> dict | None:
    """
    Get an objection response with persona-specific variation applied.

    Returns a dict with the full response, including persona-specific talk track.
    """
    response = OBJECTION_LIBRARY.get(category)
    if not response:
        return None

    persona_variation = response.persona_variations.get(persona, "")

    return {
        "category": response.category,
        "objection": response.objection_text,
        "severity": response.severity,
        "response": {
            "acknowledge": response.acknowledge,
            "reframe": response.reframe,
            "proof_point": response.proof_point,
            "persona_specific": persona_variation,
            "next_step": response.next_step,
        },
    }


def get_all_categories() -> list[str]:
    """Return all available objection categories."""
    return list(OBJECTION_LIBRARY.keys())

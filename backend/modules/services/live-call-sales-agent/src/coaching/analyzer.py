"""Real-time transcript analysis and coaching suggestion generation."""

import logging
import json
from typing import Optional, List
from datetime import datetime

from .pause_detector import PauseDetector, TranscriptBuffer

logger = logging.getLogger(__name__)


class CoachingAnalyzer:
    """
    Analyzes transcripts and generates coaching suggestions.

    **Strategy:**
    1. Buffer transcript segments
    2. Analyze for objections and phase transitions
    3. Queue suggestions during active speaking
    4. Send suggestions when pause detected
    5. Track call metrics for post-call analytics

    **Objection Types Detected:**
    - Price/cost concerns
    - Timing/implementation barriers
    - Risk/compliance concerns
    - Competitive alternatives
    - Fit/relevance questions
    - Resource constraints

    **Call Phases:**
    - opening: Initial rapport and agenda setting
    - pain_discovery: Understanding current gaps and challenges
    - solution: Presenting HDIM capabilities
    - qualification: Assessing readiness and fit
    """

    # Objection keywords
    OBJECTION_KEYWORDS = {
        "price": ["expensive", "cost", "budget", "price", "expensive", "investment"],
        "timing": ["not now", "later", "timing", "next year", "not ready", "soon"],
        "risk": ["risk", "compliance", "security", "liability", "unproven", "new"],
        "competitive": ["someone else", "competitor", "other vendor", "alternative"],
        "fit": ["fit", "relevant", "applicable", "our situation", "different"],
        "resources": ["resource", "capacity", "staff", "bandwidth", "team"],
    }

    # Phase transition indicators
    PHASE_TRANSITIONS = {
        "pain_discovery": ["measure", "gap", "closure", "performance", "challenge", "problem"],
        "solution": ["capability", "feature", "observable", "slo", "automated", "help"],
        "qualification": ["timeline", "budget", "decision", "authority", "implement", "pilot"],
    }

    def __init__(self, pause_threshold_ms: int = 2000, max_pending: int = 10):
        """Initialize coaching analyzer."""
        self.pause_detector = PauseDetector(pause_threshold_ms, max_pending)
        self.transcript_buffer = TranscriptBuffer(max_segments=20)

        # Call metrics
        self.call_start_time: Optional[datetime] = None
        self.call_metrics = {
            "total_segments": 0,
            "objections_detected": 0,
            "coaching_sent": 0,
            "phase_transitions": 0,
            "speakers": set(),
        }

    async def analyze_segment(
        self, segment: dict, call_phase: str = "opening", persona_type: str = "cmo"
    ) -> Optional[dict]:
        """
        Analyze transcript segment and return coaching if ready.

        **Args:**
            segment: Transcript with speaker, text, confidence, timestamp
            call_phase: Current phase (opening, pain_discovery, solution, qualification)
            persona_type: Customer type (cmo, cfo, provider, coordinator, it_leader)

        **Returns:**
            Coaching suggestion if pause detected, None otherwise
        """
        import time

        current_time_ms = int(time.time() * 1000)

        # Initialize call time on first segment
        if self.call_start_time is None:
            self.call_start_time = datetime.utcnow()

        # Update metrics
        self.call_metrics["total_segments"] += 1
        self.call_metrics["speakers"].add(segment.get("speaker", "Unknown"))

        # Add to transcript buffer
        self.transcript_buffer.add_segment(segment)

        # Analyze for issues
        text = segment.get("text", "").lower()

        # Detect objections
        objection_type = self._detect_objection(text)
        if objection_type:
            self.call_metrics["objections_detected"] += 1
            suggestion = self._generate_objection_reframe(
                objection_type, persona_type, segment
            )
            self.pause_detector.queue_suggestion(suggestion)

        # Detect phase transitions
        new_phase = self._detect_phase_transition(text, call_phase)
        if new_phase and new_phase != call_phase:
            self.call_metrics["phase_transitions"] += 1
            suggestion = self._generate_phase_transition_suggestion(
                call_phase, new_phase, persona_type, segment
            )
            self.pause_detector.queue_suggestion(suggestion)

        # Check if should send coaching
        self.pause_detector.process_transcript_segment(segment, current_time_ms)

        if self.pause_detector.should_send_coaching(current_time_ms):
            coaching = self.pause_detector.get_next_suggestion()
            if coaching:
                self.call_metrics["coaching_sent"] += 1
                return coaching

        return None

    def _detect_objection(self, text: str) -> Optional[str]:
        """
        Detect objection type in text.

        **Returns:**
            Objection type (price, timing, risk, etc.) or None
        """
        for objection_type, keywords in self.OBJECTION_KEYWORDS.items():
            if any(keyword in text for keyword in keywords):
                logger.info(f"🚨 Objection detected: {objection_type}")
                return objection_type
        return None

    def _generate_objection_reframe(
        self, objection_type: str, persona_type: str, segment: dict
    ) -> dict:
        """
        Generate reframe suggestion for detected objection.

        **Args:**
            objection_type: Type of objection (price, timing, risk, etc.)
            persona_type: Customer persona
            segment: Original transcript segment

        **Returns:**
            Coaching suggestion dict with reframe talking points
        """
        reframes = {
            "price": {
                "cmo": "Let me show you the ROI calculation. Most plans see 3-5% improvement in HEDIS scores, worth $5-10M for a 500K member plan.",
                "cfo": "The cost per member per month is $0.50-1.00, typically offset within 6 months by improved bonus payouts.",
                "provider": "Your quality scores improve, which improves your capitation payments.",
            },
            "timing": {
                "cmo": "We can start with a pilot on your top 3 measures - 30 days to see improvement.",
                "cfo": "Most implementations are 4-6 weeks. You could have results by Q2.",
                "provider": "Quick setup - we can have you running gap detection within 2 weeks.",
            },
            "risk": {
                "cmo": "We're HIPAA compliant with SOC2 certification. Data never leaves your environment.",
                "cfo": "We have liability insurance and a 99.9% uptime SLA.",
                "provider": "All data is encrypted at rest and in transit. No external API calls.",
            },
        }

        reframe = reframes.get(objection_type, {}).get(
            persona_type,
            f"Let me address that {objection_type} concern with some context.",
        )

        return {
            "type": "objection",
            "severity": "high",
            "message": f"Objection: {objection_type}",
            "reframe": reframe,
            "confidence": 0.85,
            "timestamp": datetime.utcnow().isoformat(),
        }

    def _detect_phase_transition(self, text: str, current_phase: str) -> Optional[str]:
        """
        Detect if customer is ready for phase transition.

        **Returns:**
            New phase name or None if still in current phase
        """
        # Simple keyword matching (Phase 2: real NLP/ML model)
        for phase, keywords in self.PHASE_TRANSITIONS.items():
            if phase != current_phase and any(keyword in text for keyword in keywords):
                logger.info(f"📊 Phase transition detected: {current_phase} → {phase}")
                return phase
        return None

    def _generate_phase_transition_suggestion(
        self, current_phase: str, new_phase: str, persona_type: str, segment: dict
    ) -> dict:
        """Generate suggestion for phase transition."""
        transition_messages = {
            ("opening", "pain_discovery"): {
                "cmo": "Tell me about your current gaps in your top 5 HEDIS measures.",
                "cfo": "What's the financial impact of your current gap closure rate?",
            },
            ("pain_discovery", "solution"): {
                "cmo": "That's where HDIM helps - we automate gap detection and prioritization.",
                "cfo": "HDIM typically improves gap closure by 15-25%, worth millions.",
            },
            ("solution", "qualification"): {
                "cmo": "When could you start a pilot? We recommend 30-60 days.",
                "cfo": "What's your timeline for implementation?",
            },
        }

        key = (current_phase, new_phase)
        message = (
            transition_messages.get(key, {})
            .get(persona_type, f"Ready to move to {new_phase}?")
        )

        return {
            "type": "phase_transition",
            "severity": "medium",
            "message": f"Ready for {new_phase.replace('_', ' ')} phase",
            "suggested_question": message,
            "confidence": 0.75,
            "timestamp": datetime.utcnow().isoformat(),
        }

    def get_call_summary(self) -> dict:
        """Get call summary for post-call analytics."""
        duration = (
            (datetime.utcnow() - self.call_start_time).total_seconds()
            if self.call_start_time
            else 0
        )

        return {
            "duration_seconds": duration,
            "total_segments": self.call_metrics["total_segments"],
            "speakers": list(self.call_metrics["speakers"]),
            "objections_detected": self.call_metrics["objections_detected"],
            "coaching_sent": self.call_metrics["coaching_sent"],
            "phase_transitions": self.call_metrics["phase_transitions"],
            "transcript": self.transcript_buffer.get_full_transcript(),
            "call_summary": self._generate_call_summary(),
        }

    def _generate_call_summary(self) -> str:
        """Generate human-readable call summary."""
        metrics = self.call_metrics
        return (
            f"Call Duration: {self._format_duration()}. "
            f"Speakers: {len(metrics['speakers'])}. "
            f"Objections: {metrics['objections_detected']}. "
            f"Coaching Provided: {metrics['coaching_sent']}. "
            f"Phase Transitions: {metrics['phase_transitions']}."
        )

    def _format_duration(self) -> str:
        """Format call duration as MM:SS."""
        if not self.call_start_time:
            return "0:00"
        duration = (datetime.utcnow() - self.call_start_time).total_seconds()
        minutes = int(duration // 60)
        seconds = int(duration % 60)
        return f"{minutes}:{seconds:02d}"

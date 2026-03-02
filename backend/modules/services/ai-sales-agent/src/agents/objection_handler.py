"""
Objection Handler Agent — Provides real-time objection responses for HDIM sales.

Retrieves pre-built, persona-specific responses to common objections
with acknowledge → reframe → proof → next step structure.
"""

from dataclasses import dataclass, field
from typing import Optional

from knowledge.objection_responses import (
    OBJECTION_LIBRARY,
    get_objection_response,
    get_all_categories,
)
from knowledge.personas import PERSONA_MAP


@dataclass
class ObjectionResult:
    """Result from objection handling."""
    category: str
    objection_text: str
    severity: str
    acknowledge: str
    reframe: str
    proof_point: str
    persona_specific: str
    next_step: str
    confidence: float  # 0-100 match confidence


class ObjectionHandler:
    """
    Handles sales objections with pre-built, persona-specific responses.

    Uses a keyword-matching approach for objection classification
    and retrieves structured responses from the objection library.
    """

    # Keywords for automatic objection classification
    CATEGORY_KEYWORDS = {
        "competitive": ["competitor", "cotiviti", "inovalon", "optum", "already using",
                        "current vendor", "existing solution", "switch", "alternative"],
        "price": ["price", "cost", "budget", "expensive", "afford", "pricing",
                  "too much", "cheaper", "investment"],
        "it_approval": ["it approval", "security", "ciso", "compliance", "review",
                        "it team", "governance", "infosec", "hipaa"],
        "contract": ["contract", "locked in", "agreement", "renewal", "committed",
                     "term", "binding"],
        "priority": ["priority", "not a priority", "other projects", "bandwidth",
                     "next quarter", "later", "not now", "timing"],
        "proof": ["prove", "case study", "evidence", "reference", "results",
                  "testimonial", "track record", "who else"],
    }

    def handle_objection(
        self,
        objection_text: str,
        persona_type: str = "cmo",
        category: Optional[str] = None,
    ) -> ObjectionResult:
        """
        Handle a sales objection.

        Args:
            objection_text: The raw objection from the customer
            persona_type: Customer persona for tailored response
            category: Optional explicit category; auto-detected if not provided
        """
        if persona_type not in PERSONA_MAP:
            raise ValueError(
                f"Unknown persona type: {persona_type}. "
                f"Valid types: {', '.join(PERSONA_MAP.keys())}"
            )

        # Auto-classify if category not provided
        if not category:
            category, confidence = self._classify_objection(objection_text)
        else:
            if category not in OBJECTION_LIBRARY:
                raise ValueError(
                    f"Unknown objection category: {category}. "
                    f"Valid categories: {', '.join(get_all_categories())}"
                )
            confidence = 100.0

        response = get_objection_response(category, persona_type)
        if not response:
            raise ValueError(f"No response found for category: {category}")

        return ObjectionResult(
            category=category,
            objection_text=objection_text,
            severity=response["severity"],
            acknowledge=response["response"]["acknowledge"],
            reframe=response["response"]["reframe"],
            proof_point=response["response"]["proof_point"],
            persona_specific=response["response"]["persona_specific"],
            next_step=response["response"]["next_step"],
            confidence=confidence,
        )

    def list_objection_categories(self) -> list[dict]:
        """Return all available objection categories with descriptions."""
        categories = []
        for key, obj in OBJECTION_LIBRARY.items():
            categories.append({
                "category": key,
                "example_objection": obj.objection_text,
                "severity": obj.severity,
            })
        return categories

    def _classify_objection(self, text: str) -> tuple[str, float]:
        """
        Classify an objection by keyword matching.

        Returns (category, confidence) tuple.
        """
        text_lower = text.lower()
        scores: dict[str, int] = {}

        for category, keywords in self.CATEGORY_KEYWORDS.items():
            score = sum(1 for kw in keywords if kw in text_lower)
            if score > 0:
                scores[category] = score

        if not scores:
            # Default to "proof" as the safest response for unclassified objections
            return "proof", 30.0

        best_category = max(scores, key=scores.get)  # type: ignore
        # Confidence based on keyword match density
        max_keywords = len(self.CATEGORY_KEYWORDS[best_category])
        confidence = min((scores[best_category] / max_keywords) * 100, 95.0)

        return best_category, round(confidence, 1)


def handle_objection(
    objection_text: str,
    persona_type: str = "cmo",
    category: Optional[str] = None,
) -> ObjectionResult:
    """Convenience function for handling an objection."""
    handler = ObjectionHandler()
    return handler.handle_objection(
        objection_text=objection_text,
        persona_type=persona_type,
        category=category,
    )

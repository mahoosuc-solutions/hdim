"""HEDIS and care gap specific metric evaluators."""

from typing import Any

import structlog

from ..metrics.base import MetricEvaluator
from ..models import EvaluationResponse, MetricType

logger = structlog.get_logger()


class CareGapRelevanceEvaluator(MetricEvaluator):
    """Evaluates relevance of response to patient care gaps."""

    # Common HEDIS care gap measures
    CARE_GAP_MEASURES = {
        "hba1c": ["diabetes", "a1c", "glycemic", "blood sugar", "glucose"],
        "bcs": ["breast cancer", "mammogram", "mammography"],
        "ccs": ["cervical cancer", "pap smear", "pap test", "hpv"],
        "col": ["colorectal", "colonoscopy", "fit test", "colon cancer"],
        "ldl": ["cholesterol", "ldl", "lipid", "statin"],
        "bp_control": ["blood pressure", "hypertension", "bp"],
        "eye_exam": ["diabetic eye", "retinopathy", "ophthalmology", "eye exam"],
        "kidney": ["kidney", "nephropathy", "egfr", "creatinine", "uacr"],
        "flu_vaccine": ["flu shot", "influenza", "vaccination"],
        "pneumonia_vaccine": ["pneumonia", "pneumococcal"],
    }

    @property
    def metric_type(self) -> MetricType:
        return MetricType.CARE_GAP_RELEVANCE

    @property
    def description(self) -> str:
        return "Evaluates how well response addresses identified care gaps"

    async def evaluate(
        self,
        user_message: str,
        agent_response: str,
        context_data: dict[str, Any],
    ) -> EvaluationResponse:
        """Assess care gap relevance."""
        response_lower = agent_response.lower()
        metadata: dict[str, Any] = {}

        # Get expected care gaps from context
        expected_gaps = context_data.get("careGaps", [])
        if not expected_gaps:
            expected_gaps = context_data.get("openGaps", [])
        if not expected_gaps:
            # Try to infer from shouldIdentify in expectedBehavior
            expected_gaps = context_data.get("shouldIdentify", [])

        # Normalize gap names
        expected_gaps = [g.lower().replace("_", " ") for g in expected_gaps]

        # Check which gaps are addressed
        gaps_addressed = []
        gaps_missed = []

        for gap in expected_gaps:
            # Check if the gap or its related terms are mentioned
            gap_key = gap.replace(" ", "_").lower()
            related_terms = self.CARE_GAP_MEASURES.get(gap_key, [gap])

            is_addressed = any(term in response_lower for term in related_terms)
            if is_addressed:
                gaps_addressed.append(gap)
            else:
                gaps_missed.append(gap)

        # Check for prioritization if multiple gaps
        mentions_priority = any(
            word in response_lower
            for word in ["priority", "prioritize", "urgent", "first", "most important"]
        )

        # Check for actionable recommendations
        has_recommendations = any(
            phrase in response_lower
            for phrase in [
                "recommend", "schedule", "order", "refer", "contact",
                "follow up", "next step", "action", "suggest",
            ]
        )

        # Calculate score
        if not expected_gaps:
            # No specific gaps to check - assess general care gap language
            has_care_gap_content = any(
                term in response_lower
                for terms in self.CARE_GAP_MEASURES.values()
                for term in terms
            )
            score = 0.80 if has_care_gap_content else 0.60
            reason = "No specific care gaps in context; general assessment"
        elif gaps_addressed:
            coverage = len(gaps_addressed) / len(expected_gaps)
            base_score = 0.50 + (coverage * 0.40)

            # Bonus for prioritization
            if mentions_priority and len(expected_gaps) > 1:
                base_score += 0.05

            # Bonus for recommendations
            if has_recommendations:
                base_score += 0.05

            score = min(1.0, base_score)

            if gaps_missed:
                reason = f"Addressed {len(gaps_addressed)}/{len(expected_gaps)} gaps; missed: {', '.join(gaps_missed[:2])}"
            else:
                reason = f"All {len(expected_gaps)} care gaps addressed"
        else:
            score = 0.30
            reason = f"Response did not address expected care gaps: {', '.join(expected_gaps[:3])}"

        metadata["expected_gaps"] = expected_gaps
        metadata["gaps_addressed"] = gaps_addressed
        metadata["gaps_missed"] = gaps_missed
        metadata["mentions_priority"] = mentions_priority
        metadata["has_recommendations"] = has_recommendations

        return self._build_response(score=score, reason=reason, metadata=metadata)


class HEDISComplianceEvaluator(MetricEvaluator):
    """Evaluates accuracy of HEDIS measure information."""

    # HEDIS measure specifications (simplified)
    HEDIS_MEASURES = {
        "hbd": {
            "name": "Hemoglobin A1c Control for Patients with Diabetes",
            "threshold": 8.0,
            "population": "diabetes",
            "keywords": ["hba1c", "a1c", "diabetes", "glycemic"],
        },
        "bcs": {
            "name": "Breast Cancer Screening",
            "age_range": (50, 74),
            "frequency": "27 months",
            "keywords": ["mammogram", "breast cancer", "mammography"],
        },
        "ccs": {
            "name": "Cervical Cancer Screening",
            "age_range": (21, 64),
            "keywords": ["pap smear", "pap test", "cervical", "hpv"],
        },
        "col": {
            "name": "Colorectal Cancer Screening",
            "age_range": (50, 75),
            "keywords": ["colonoscopy", "colorectal", "fit test"],
        },
        "cdc": {
            "name": "Comprehensive Diabetes Care",
            "keywords": ["diabetes", "a1c", "eye exam", "kidney", "blood pressure"],
        },
    }

    @property
    def metric_type(self) -> MetricType:
        return MetricType.HEDIS_COMPLIANCE

    @property
    def description(self) -> str:
        return "Evaluates accuracy of HEDIS measure calculations and explanations"

    async def evaluate(
        self,
        user_message: str,
        agent_response: str,
        context_data: dict[str, Any],
    ) -> EvaluationResponse:
        """Assess HEDIS compliance accuracy."""
        response_lower = agent_response.lower()
        metadata: dict[str, Any] = {}
        issues = []

        # Get measure from context
        measure_id = context_data.get("measureId", "").lower()
        expected_compliance = context_data.get("expectedCompliance")

        # Try to identify which measure is being discussed
        measure_spec = self.HEDIS_MEASURES.get(measure_id)

        if measure_spec:
            # Check if response uses correct terminology
            keywords_mentioned = [
                kw for kw in measure_spec["keywords"]
                if kw in response_lower
            ]
            metadata["keywords_mentioned"] = keywords_mentioned

            # Check age range accuracy if mentioned
            if "age_range" in measure_spec:
                min_age, max_age = measure_spec["age_range"]
                # Check for incorrect age statements
                if f"{min_age - 5}" in agent_response or f"{max_age + 5}" in agent_response:
                    issues.append("Potential age range inaccuracy")

            # Check threshold accuracy for HbA1c
            if measure_id == "hbd" and "threshold" in measure_spec:
                threshold = measure_spec["threshold"]
                if str(threshold) in agent_response or f"{threshold}%" in agent_response:
                    metadata["threshold_mentioned_correctly"] = True
                elif "9.0" in agent_response or "7.0" in agent_response:
                    # Common wrong thresholds
                    issues.append("May have incorrect HbA1c threshold")

            # Check for compliance statement accuracy
            if expected_compliance is not None:
                compliance_words = ["compliant", "met", "satisfied", "achieved"]
                non_compliance_words = ["not compliant", "gap", "overdue", "missed", "not met"]

                states_compliant = any(w in response_lower for w in compliance_words)
                states_non_compliant = any(w in response_lower for w in non_compliance_words)

                if expected_compliance and states_non_compliant and not states_compliant:
                    issues.append("Response suggests non-compliance when patient is compliant")
                elif not expected_compliance and states_compliant and not states_non_compliant:
                    issues.append("Response suggests compliance when patient has a gap")

        # Check for explanation of criteria
        explains_criteria = any(
            phrase in response_lower
            for phrase in ["criteria", "requirement", "measure", "qualify", "eligible"]
        )
        metadata["explains_criteria"] = explains_criteria

        # Calculate score
        if issues:
            score = max(0.4, 0.9 - (len(issues) * 0.2))
            reason = f"HEDIS accuracy concerns: {'; '.join(issues)}"
        elif measure_spec and keywords_mentioned:
            score = 0.90 if explains_criteria else 0.80
            reason = f"HEDIS measure {measure_id.upper()} correctly referenced"
        else:
            score = 0.70
            reason = "General HEDIS content without specific measure verification"

        metadata["measure_id"] = measure_id
        metadata["issues"] = issues

        return self._build_response(score=score, reason=reason, metadata=metadata)

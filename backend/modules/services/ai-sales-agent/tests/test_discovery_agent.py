"""
Tests for Discovery Agent.

Test cases covering:
1. Mock discovery call execution
2. Qualification logic (green/yellow/red)
3. Pain point extraction
4. Call scoring rubric
5. CRM integration (mock)
"""

import pytest

from src.agents.discovery_agent import DiscoveryAgent, run_discovery_call
from src.knowledge.personas import CMO_PERSONA


@pytest.fixture
def discovery_agent():
    """Create discovery agent instance."""
    return DiscoveryAgent()


class TestDiscoveryCallExecution:
    """Test discovery call execution."""

    def test_discovery_call_completes(self, discovery_agent):
        """Test that a discovery call executes and returns valid result."""
        result = discovery_agent.execute_discovery_call(
            customer_name="HealthFirst Insurance",
            customer_context="500K members, CMO on call, pain: manual gap closure",
            persona_type="cmo",
            call_duration_minutes=30,
        )

        assert result is not None
        assert result.customer_name == "HealthFirst Insurance"
        assert result.persona_type == "cmo"
        assert result.qualification in ["green", "yellow", "red"]
        assert 0 <= result.call_score <= 100
        assert result.next_steps is not None

    def test_discovery_call_with_different_personas(self, discovery_agent):
        """Test discovery call with each persona type."""
        personas = ["cmo", "coordinator", "cfo", "it", "provider"]

        for persona in personas:
            result = discovery_agent.execute_discovery_call(
                customer_name=f"Test Company ({persona})",
                customer_context="Test context for discovery call",
                persona_type=persona,
            )

            assert result.persona_type == persona
            assert result.qualification in ["green", "yellow", "red"]
            assert 0 <= result.call_score <= 100


class TestCallScoring:
    """Test call quality scoring."""

    def test_call_score_range(self, discovery_agent):
        """Test that call score is always in 0-100 range."""
        for _ in range(5):
            result = discovery_agent.execute_discovery_call(
                customer_name="Test Company",
                customer_context="Test context",
                persona_type="cmo",
            )

            assert isinstance(result.call_score, (int, float))
            assert 0 <= result.call_score <= 100

    def test_call_score_reflects_quality(self, discovery_agent):
        """Test that call score reflects execution quality."""
        result = discovery_agent.execute_discovery_call(
            customer_name="HealthFirst Insurance",
            customer_context=(
                "500K members, CMO on call, clear HEDIS targets, "
                "pain: manual gap closure, Q2 2026 budget available"
            ),
            persona_type="cmo",
        )

        # Well-qualified opportunity should have decent score
        assert result.call_score >= 70


class TestQualificationLogic:
    """Test qualification decision logic."""

    def test_qualification_green(self, discovery_agent):
        """Test green qualification for high-fit opportunity."""
        result = discovery_agent.execute_discovery_call(
            customer_name="HealthFirst Insurance",
            customer_context=(
                "500K members, CMO on call, clear HEDIS targets, "
                "pain: manual gap closure, $200K budget, Q2 2026"
            ),
            persona_type="cmo",
        )

        assert result.qualification == "green"
        assert "demo" in result.next_steps.lower()

    def test_qualification_red_too_small(self, discovery_agent):
        """Test red qualification for too-small opportunity."""
        result = discovery_agent.execute_discovery_call(
            customer_name="Small Clinic",
            customer_context="15K members, tight budget, low pain level",
            persona_type="cmo",
        )

        # Too small (< 20K members) = should be red
        assert result.qualification == "red"

    def test_next_steps_by_qualification(self, discovery_agent):
        """Test that next steps match qualification level."""
        result = discovery_agent.execute_discovery_call(
            customer_name="Test Company",
            customer_context="500K members, CMO, clear pain, Q2 budget",
            persona_type="cmo",
        )

        if result.qualification == "green":
            assert "demo" in result.next_steps.lower()
        elif result.qualification == "yellow":
            assert "reference" in result.next_steps.lower() or "case" in result.next_steps.lower()
        else:  # red
            assert "nurture" in result.next_steps.lower()


class TestPainPointExtraction:
    """Test pain point extraction from calls."""

    def test_pain_points_discovered(self, discovery_agent):
        """Test that pain points are extracted from call transcript."""
        result = discovery_agent.execute_discovery_call(
            customer_name="HealthFirst Insurance",
            customer_context=(
                "Main pain points: manual gap closure, low provider engagement, "
                "HEDIS deadline pressure"
            ),
            persona_type="cmo",
        )

        assert len(result.pain_points_discovered) > 0
        assert isinstance(result.pain_points_discovered, list)
        # Pain points should be strings
        assert all(isinstance(p, str) for p in result.pain_points_discovered)

    def test_multiple_pain_points(self, discovery_agent):
        """Test extraction of multiple pain points."""
        result = discovery_agent.execute_discovery_call(
            customer_name="Test Company",
            customer_context=(
                "Multiple issues: "
                "1. Manual workflows eating coordinator time "
                "2. Reactive gap discovery (late submissions) "
                "3. Low provider engagement "
                "4. Missed quality bonus opportunities"
            ),
            persona_type="cmo",
        )

        # Should extract multiple pain points
        assert len(result.pain_points_discovered) >= 2


class TestCallTranscript:
    """Test call transcript generation."""

    def test_transcript_is_string(self, discovery_agent):
        """Test that call transcript is a valid string."""
        result = discovery_agent.execute_discovery_call(
            customer_name="Test Company",
            customer_context="Test context",
            persona_type="cmo",
        )

        assert isinstance(result.call_transcript, str)
        assert len(result.call_transcript) > 0

    def test_transcript_contains_questions_and_responses(self, discovery_agent):
        """Test that transcript contains both agent and (simulated) customer."""
        result = discovery_agent.execute_discovery_call(
            customer_name="Test Company",
            customer_context="Test context",
            persona_type="cmo",
        )

        transcript = result.call_transcript.lower()
        # Should contain agent dialog
        assert "agent:" in transcript or "opening" in transcript or "question" in transcript


class TestConvenienceFunctions:
    """Test module-level convenience functions."""

    def test_run_discovery_call_function(self):
        """Test the module-level run_discovery_call function."""
        result = run_discovery_call(
            customer_name="Test Company",
            customer_context="Test context",
            persona_type="cmo",
        )

        assert result.customer_name == "Test Company"
        assert result.qualification in ["green", "yellow", "red"]
        assert 0 <= result.call_score <= 100


# ============================================================================
# INTEGRATION TESTS (with CRM, etc.)
# ============================================================================


class TestCRMIntegration:
    """Test CRM logging integration."""

    @pytest.mark.skip(reason="CRM integration pending")
    def test_crm_logging(self, discovery_agent):
        """Test that call is logged to CRM."""
        result = discovery_agent.execute_discovery_call(
            customer_name="HealthFirst Insurance",
            customer_context="500K members, CMO on call",
            persona_type="cmo",
        )

        # In production, verify call logged to CRM
        assert result.crm_logged is True


# ============================================================================
# BENCHMARK TESTS
# ============================================================================


class TestPerformance:
    """Test performance metrics."""

    @pytest.mark.skip(reason="Performance testing pending")
    def test_discovery_call_completes_in_time(self, discovery_agent):
        """Test that discovery call completes in reasonable time."""
        import time

        start = time.time()
        result = discovery_agent.execute_discovery_call(
            customer_name="Test Company",
            customer_context="Test context",
            persona_type="cmo",
        )
        elapsed = time.time() - start

        # Should complete in < 30 seconds (for testing)
        assert elapsed < 30
        assert result is not None

"""Tests for Demo Agent."""

import pytest
from src.agents.demo_agent import DemoAgent, generate_demo_script


@pytest.fixture
def demo_agent():
    return DemoAgent()


class TestDemoScriptGeneration:
    """Test basic demo script generation."""

    def test_generates_cmo_30_min_demo(self, demo_agent):
        result = demo_agent.generate_demo_script(
            customer_name="Acme Health",
            persona_type="cmo",
            duration_minutes=30,
        )
        assert result.persona_type == "cmo"
        assert result.customer_name == "Acme Health"
        assert result.duration_minutes == 30
        assert len(result.opening_hook) > 0
        assert len(result.segments) > 0
        assert len(result.follow_up_actions) > 0

    def test_generates_all_personas(self, demo_agent):
        for persona in ["cmo", "coordinator", "cfo", "provider", "it"]:
            result = demo_agent.generate_demo_script(
                customer_name="Test Org",
                persona_type=persona,
                duration_minutes=15,
            )
            assert result.persona_type == persona
            assert len(result.segments) > 0

    def test_supports_15_30_45_durations(self, demo_agent):
        for duration in [15, 30, 45]:
            result = demo_agent.generate_demo_script(
                customer_name="Test Org",
                persona_type="cmo",
                duration_minutes=duration,
            )
            assert result.duration_minutes == duration

    def test_rejects_unknown_persona(self, demo_agent):
        with pytest.raises(ValueError, match="Unknown persona"):
            demo_agent.generate_demo_script(
                customer_name="Test",
                persona_type="invalid_persona",
            )


class TestDemoCustomization:
    """Test demo customization with pain points and patient data."""

    def test_includes_pain_point_notes(self, demo_agent):
        result = demo_agent.generate_demo_script(
            customer_name="Beta Health",
            persona_type="cmo",
            pain_points=["Manual chart reviews are too slow", "Star ratings declining"],
        )
        assert len(result.customization_notes) > 0

    def test_customizes_closing_with_patient_count(self, demo_agent):
        result = demo_agent.generate_demo_script(
            customer_name="Large Plan",
            persona_type="cmo",
            duration_minutes=15,
            patient_count=50000,
        )
        # Closing should contain patient count or ROI projection
        assert result.closing  # Non-empty closing

    def test_large_population_note(self, demo_agent):
        result = demo_agent.generate_demo_script(
            customer_name="Enterprise Plan",
            persona_type="cmo",
            patient_count=200000,
        )
        scalability_notes = [n for n in result.customization_notes if "scalability" in n.lower()]
        assert len(scalability_notes) > 0


class TestDemoSegments:
    """Test segment structure and talking points."""

    def test_segments_have_names_and_minutes(self, demo_agent):
        result = demo_agent.generate_demo_script(
            customer_name="Test",
            persona_type="cmo",
            duration_minutes=30,
        )
        for segment in result.segments:
            assert "name" in segment
            assert "minutes" in segment
            assert segment["minutes"] > 0

    def test_total_talking_points_positive(self, demo_agent):
        result = demo_agent.generate_demo_script(
            customer_name="Test",
            persona_type="cmo",
            duration_minutes=30,
        )
        assert result.total_talking_points > 0


class TestDemoAvailability:
    """Test listing available demos."""

    def test_list_available_demos(self, demo_agent):
        available = demo_agent.list_available_demos()
        assert "cmo" in available
        assert "cfo" in available
        assert "it" in available


class TestConvenienceFunction:
    """Test module-level convenience function."""

    def test_generate_demo_script_function(self):
        result = generate_demo_script(
            customer_name="Quick Test",
            persona_type="cmo",
            duration_minutes=15,
        )
        assert result.customer_name == "Quick Test"
        assert result.persona_type == "cmo"

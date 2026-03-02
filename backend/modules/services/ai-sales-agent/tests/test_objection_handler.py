"""Tests for Objection Handler Agent."""

import pytest
from src.agents.objection_handler import ObjectionHandler, handle_objection


@pytest.fixture
def handler():
    return ObjectionHandler()


class TestObjectionClassification:
    """Test automatic objection classification."""

    def test_classifies_competitive_objection(self, handler):
        result = handler.handle_objection(
            objection_text="We're already using Cotiviti for quality measures",
            persona_type="cmo",
        )
        assert result.category == "competitive"
        assert result.confidence > 0

    def test_classifies_price_objection(self, handler):
        result = handler.handle_objection(
            objection_text="The pricing seems too expensive for our budget",
            persona_type="cfo",
        )
        assert result.category == "price"

    def test_classifies_it_approval_objection(self, handler):
        result = handler.handle_objection(
            objection_text="Our IT team needs to do a security review first",
            persona_type="it",
        )
        assert result.category == "it_approval"

    def test_classifies_contract_objection(self, handler):
        result = handler.handle_objection(
            objection_text="We're locked into a contract with our current vendor",
        )
        assert result.category == "contract"

    def test_classifies_priority_objection(self, handler):
        result = handler.handle_objection(
            objection_text="This isn't our priority right now, we have other projects",
        )
        assert result.category == "priority"

    def test_classifies_proof_objection(self, handler):
        result = handler.handle_objection(
            objection_text="Can you prove this works? Do you have a case study?",
        )
        assert result.category == "proof"

    def test_defaults_to_proof_for_unknown(self, handler):
        result = handler.handle_objection(
            objection_text="I'm not sure about this whole thing",
        )
        # Should default to "proof" with low confidence
        assert result.category == "proof"
        assert result.confidence <= 30


class TestObjectionResponses:
    """Test response structure and content."""

    def test_response_has_all_fields(self, handler):
        result = handler.handle_objection(
            objection_text="Too expensive",
            persona_type="cfo",
            category="price",
        )
        assert len(result.acknowledge) > 0
        assert len(result.reframe) > 0
        assert len(result.proof_point) > 0
        assert len(result.next_step) > 0
        assert result.severity in ["low", "medium", "high"]

    def test_explicit_category_gives_full_confidence(self, handler):
        result = handler.handle_objection(
            objection_text="anything",
            category="competitive",
        )
        assert result.confidence == 100.0

    def test_persona_specific_response(self, handler):
        cfo_result = handler.handle_objection(
            objection_text="Too expensive",
            persona_type="cfo",
            category="price",
        )
        cmo_result = handler.handle_objection(
            objection_text="Too expensive",
            persona_type="cmo",
            category="price",
        )
        # Different personas should get different persona_specific text
        assert cfo_result.persona_specific != cmo_result.persona_specific


class TestObjectionCategories:
    """Test category listing."""

    def test_lists_all_6_categories(self, handler):
        categories = handler.list_objection_categories()
        assert len(categories) == 6
        category_names = [c["category"] for c in categories]
        assert "competitive" in category_names
        assert "price" in category_names
        assert "it_approval" in category_names
        assert "contract" in category_names
        assert "priority" in category_names
        assert "proof" in category_names


class TestValidation:
    """Test input validation."""

    def test_rejects_unknown_persona(self, handler):
        with pytest.raises(ValueError, match="Unknown persona"):
            handler.handle_objection(
                objection_text="Too expensive",
                persona_type="invalid",
            )

    def test_rejects_unknown_category(self, handler):
        with pytest.raises(ValueError, match="Unknown objection category"):
            handler.handle_objection(
                objection_text="Something",
                category="nonexistent",
            )


class TestConvenienceFunction:
    """Test module-level convenience function."""

    def test_handle_objection_function(self):
        result = handle_objection(
            objection_text="We use Cotiviti already",
            persona_type="cmo",
        )
        assert result.category == "competitive"
        assert len(result.reframe) > 0


class TestAllPersonasAllCategories:
    """Test that all persona+category combinations produce valid responses."""

    PERSONAS = ["cmo", "coordinator", "cfo", "provider", "it"]
    CATEGORIES = ["competitive", "price", "it_approval", "contract", "priority", "proof"]

    def test_all_combinations(self, handler):
        for persona in self.PERSONAS:
            for category in self.CATEGORIES:
                result = handler.handle_objection(
                    objection_text="test objection",
                    persona_type=persona,
                    category=category,
                )
                assert result.acknowledge
                assert result.reframe
                assert result.proof_point
                assert result.next_step

"""
Agent Harness Tests with Langfuse Tracing

Tests agent capabilities using Anthropic's best practices:
- Tool use validation
- Multi-turn conversations
- Context window management
- Error recovery
- Long-running task patterns

Run: pytest tests/test_agent_harness.py -v
"""

import json
import pytest
from typing import Optional
from dataclasses import dataclass


@dataclass
class AgentResponse:
    """Structured agent response."""
    content: str
    tool_calls: list
    stop_reason: str
    tokens_used: int


class TestToolUse:
    """Test agent tool use capabilities."""

    @pytest.fixture
    def calculator_tools(self):
        """Simple calculator tools for testing."""
        return [
            {
                "name": "calculate",
                "description": "Perform a mathematical calculation",
                "input_schema": {
                    "type": "object",
                    "properties": {
                        "expression": {
                            "type": "string",
                            "description": "Mathematical expression to evaluate"
                        }
                    },
                    "required": ["expression"]
                }
            },
            {
                "name": "get_patient_data",
                "description": "Retrieve patient data by MRN",
                "input_schema": {
                    "type": "object",
                    "properties": {
                        "mrn": {
                            "type": "string",
                            "description": "Medical Record Number"
                        }
                    },
                    "required": ["mrn"]
                }
            }
        ]

    def test_tool_selection(self, anthropic_client, default_model, calculator_tools, trace):
        """Test that agent selects appropriate tool."""
        response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=1024,
            tools=calculator_tools,
            messages=[{
                "role": "user",
                "content": "What is 15% of 200?"
            }]
        )

        # Check tool was used
        assert response.stop_reason == "tool_use", "Expected tool use for calculation"

        tool_use = next(
            (block for block in response.content if block.type == "tool_use"),
            None
        )
        assert tool_use is not None, "Expected tool_use block"
        assert tool_use.name == "calculate", "Expected calculate tool"

    def test_tool_with_result(self, anthropic_client, default_model, calculator_tools, trace):
        """Test full tool use cycle with result."""
        # First turn - agent requests tool
        response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=1024,
            tools=calculator_tools,
            messages=[{
                "role": "user",
                "content": "Calculate 25 * 4 for me."
            }]
        )

        tool_use = next(
            (block for block in response.content if block.type == "tool_use"),
            None
        )

        # Second turn - provide tool result
        final_response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=1024,
            tools=calculator_tools,
            messages=[
                {"role": "user", "content": "Calculate 25 * 4 for me."},
                {"role": "assistant", "content": response.content},
                {
                    "role": "user",
                    "content": [{
                        "type": "tool_result",
                        "tool_use_id": tool_use.id,
                        "content": "100"
                    }]
                }
            ]
        )

        # Check final response includes the result
        text_content = next(
            (block.text for block in final_response.content if hasattr(block, "text")),
            ""
        )
        assert "100" in text_content, "Expected result in final response"


class TestMultiTurn:
    """Test multi-turn conversation handling."""

    def test_context_retention(self, anthropic_client, default_model, trace):
        """Test that agent retains context across turns."""
        messages = [
            {"role": "user", "content": "My name is Dr. Smith and I work at General Hospital."},
        ]

        # First turn
        response1 = anthropic_client.messages.create(
            model=default_model,
            max_tokens=256,
            messages=messages
        )
        messages.append({"role": "assistant", "content": response1.content[0].text})

        # Second turn - reference previous context
        messages.append({"role": "user", "content": "What's my name and where do I work?"})

        response2 = anthropic_client.messages.create(
            model=default_model,
            max_tokens=256,
            messages=messages
        )

        text = response2.content[0].text.lower()
        assert "smith" in text, "Agent should remember the name"
        assert "general hospital" in text, "Agent should remember the workplace"

    def test_instruction_following_across_turns(self, anthropic_client, default_model, trace):
        """Test that agent follows instructions across turns."""
        messages = [
            {
                "role": "user",
                "content": "From now on, always end your responses with 'HDIM-OK'."
            }
        ]

        response1 = anthropic_client.messages.create(
            model=default_model,
            max_tokens=256,
            messages=messages
        )
        messages.append({"role": "assistant", "content": response1.content[0].text})

        # Follow-up question
        messages.append({"role": "user", "content": "What is FHIR?"})

        response2 = anthropic_client.messages.create(
            model=default_model,
            max_tokens=512,
            messages=messages
        )

        assert "HDIM-OK" in response2.content[0].text, \
            "Agent should follow instruction to end with HDIM-OK"


class TestErrorRecovery:
    """Test agent error handling and recovery."""

    @pytest.fixture
    def fallible_tool(self):
        """Tool that can fail."""
        return [{
            "name": "unreliable_api",
            "description": "An API that sometimes fails",
            "input_schema": {
                "type": "object",
                "properties": {
                    "query": {"type": "string"}
                },
                "required": ["query"]
            }
        }]

    def test_handles_tool_error(self, anthropic_client, default_model, fallible_tool, trace):
        """Test agent gracefully handles tool errors."""
        # First turn - agent requests tool
        response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=1024,
            tools=fallible_tool,
            messages=[{
                "role": "user",
                "content": "Query the unreliable API for patient data."
            }]
        )

        tool_use = next(
            (block for block in response.content if block.type == "tool_use"),
            None
        )

        if tool_use:
            # Return error result
            error_response = anthropic_client.messages.create(
                model=default_model,
                max_tokens=1024,
                tools=fallible_tool,
                messages=[
                    {"role": "user", "content": "Query the unreliable API for patient data."},
                    {"role": "assistant", "content": response.content},
                    {
                        "role": "user",
                        "content": [{
                            "type": "tool_result",
                            "tool_use_id": tool_use.id,
                            "content": "Error: Service temporarily unavailable (503)",
                            "is_error": True
                        }]
                    }
                ]
            )

            # Agent should acknowledge the error gracefully
            text = error_response.content[0].text.lower()
            assert any(word in text for word in ["error", "unavailable", "failed", "sorry", "unable"]), \
                "Agent should acknowledge the error"


class TestLongRunningPatterns:
    """Test patterns for long-running agents (Anthropic best practices)."""

    def test_progress_tracking_pattern(self, anthropic_client, default_model, trace):
        """Test progress file pattern for long-running tasks."""
        system_prompt = """You are a coding agent working on a multi-step task.
        After each step, you must output a progress update in this format:
        PROGRESS: [current_step]/[total_steps] - [description]

        When complete, output: COMPLETE: [summary]
        """

        response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=1024,
            system=system_prompt,
            messages=[{
                "role": "user",
                "content": "Plan a 3-step process to add a new API endpoint."
            }]
        )

        text = response.content[0].text
        assert "PROGRESS:" in text or "COMPLETE:" in text, \
            "Agent should use progress tracking format"

    def test_incremental_work_pattern(self, anthropic_client, default_model, trace):
        """Test that agent works incrementally vs trying to do everything."""
        response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=2048,
            system="You are a careful agent. For complex tasks, always break them into steps and complete one step at a time. After each step, pause and report what you completed.",
            messages=[{
                "role": "user",
                "content": "Implement a FHIR Patient resource validator. This is step 1 of a multi-session task."
            }]
        )

        text = response.content[0].text.lower()
        # Agent should focus on first step, not try to do everything
        assert any(word in text for word in ["step 1", "first", "begin", "start"]), \
            "Agent should focus on first step"


class TestAgentMetrics:
    """Test and collect agent performance metrics."""

    def test_response_latency(self, anthropic_client, default_model, langfuse, trace):
        """Measure and log response latency."""
        import time

        start = time.time()
        response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=256,
            messages=[{"role": "user", "content": "Hello, how are you?"}]
        )
        latency = time.time() - start

        # Log to Langfuse if available
        if langfuse and trace:
            trace.score(
                name="latency_seconds",
                value=latency,
                comment=f"Response latency: {latency:.2f}s"
            )

        assert latency < 30, f"Response took too long: {latency:.2f}s"

    def test_token_efficiency(self, anthropic_client, default_model, langfuse, trace):
        """Measure token usage efficiency."""
        response = anthropic_client.messages.create(
            model=default_model,
            max_tokens=512,
            messages=[{
                "role": "user",
                "content": "Explain HEDIS in exactly 3 sentences."
            }]
        )

        input_tokens = response.usage.input_tokens
        output_tokens = response.usage.output_tokens

        # Log to Langfuse
        if langfuse and trace:
            trace.score(
                name="input_tokens",
                value=input_tokens
            )
            trace.score(
                name="output_tokens",
                value=output_tokens
            )
            trace.score(
                name="efficiency_ratio",
                value=output_tokens / max(input_tokens, 1),
                comment="Output/Input token ratio"
            )

        # Basic efficiency check
        assert output_tokens > 0, "Should produce output"
        assert output_tokens < 500, "Should respect token constraint"

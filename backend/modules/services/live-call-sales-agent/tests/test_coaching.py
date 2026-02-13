"""Tests for coaching engine components."""

import pytest
import asyncio
from src.coaching.pause_detector import PauseDetector, TranscriptBuffer
from src.coaching.analyzer import CoachingAnalyzer
from src.websocket.coaching_client import CoachingWebSocketClient


class TestPauseDetector:
    """Tests for pause detection."""

    def test_pause_detector_init(self):
        """Test pause detector initialization."""
        detector = PauseDetector(pause_threshold_ms=2000, max_pending=10)
        assert detector.pause_threshold_ms == 2000
        assert detector.max_pending == 10
        assert detector.last_speech_time_ms == 0
        assert len(detector.pending_suggestions) == 0

    def test_should_send_coaching_without_suggestions(self):
        """Test that no coaching sent without suggestions queued."""
        detector = PauseDetector(pause_threshold_ms=2000)

        # Even with pause, no suggestions = no send
        assert detector.should_send_coaching(current_time_ms=5000) is False

    def test_should_send_coaching_with_suggestion_and_pause(self):
        """Test coaching sent when suggestion queued and pause detected."""
        detector = PauseDetector(pause_threshold_ms=2000)

        # Queue a suggestion
        detector.queue_suggestion({"message": "Test suggestion"})

        # Update last speech time to 3000ms ago
        detector.last_speech_time_ms = 0
        current_time = 3000

        # Should send (pause > threshold)
        assert detector.should_send_coaching(current_time) is True

    def test_get_next_suggestion_fifo(self):
        """Test suggestions returned in FIFO order."""
        detector = PauseDetector()

        detector.queue_suggestion({"message": "First"})
        detector.queue_suggestion({"message": "Second"})
        detector.queue_suggestion({"message": "Third"})

        assert detector.get_next_suggestion()["message"] == "First"
        assert detector.get_next_suggestion()["message"] == "Second"
        assert detector.get_next_suggestion()["message"] == "Third"
        assert detector.get_next_suggestion() is None

    def test_queue_at_capacity(self):
        """Test queue respects max capacity."""
        detector = PauseDetector(max_pending=3)

        assert detector.queue_suggestion({"message": "1"}) is True
        assert detector.queue_suggestion({"message": "2"}) is True
        assert detector.queue_suggestion({"message": "3"}) is True
        assert detector.queue_suggestion({"message": "4"}) is False

    def test_clear_queue(self):
        """Test clearing queue."""
        detector = PauseDetector()

        detector.queue_suggestion({"message": "1"})
        detector.queue_suggestion({"message": "2"})

        cleared = detector.clear_queue()
        assert cleared == 2
        assert len(detector.pending_suggestions) == 0


class TestTranscriptBuffer:
    """Tests for transcript buffering."""

    def test_buffer_add_segment(self):
        """Test adding segments to buffer."""
        buffer = TranscriptBuffer(max_segments=10)

        buffer.add_segment({"speaker": "Speaker 1", "text": "Hello"})
        buffer.add_segment({"speaker": "Speaker 2", "text": "Hi there"})

        assert len(buffer.segments) == 2

    def test_buffer_get_recent(self):
        """Test retrieving recent segments."""
        buffer = TranscriptBuffer()

        for i in range(10):
            buffer.add_segment({"speaker": f"Speaker {i}", "text": f"Message {i}"})

        recent = buffer.get_recent(3)
        assert len(recent) == 3
        assert recent[-1]["text"] == "Message 9"

    def test_buffer_get_by_speaker(self):
        """Test filtering segments by speaker."""
        buffer = TranscriptBuffer()

        buffer.add_segment({"speaker": "Speaker 1", "text": "First"})
        buffer.add_segment({"speaker": "Speaker 2", "text": "Second"})
        buffer.add_segment({"speaker": "Speaker 1", "text": "Third"})

        speaker1 = buffer.get_by_speaker("Speaker 1")
        assert len(speaker1) == 2
        assert speaker1[0]["text"] == "First"
        assert speaker1[1]["text"] == "Third"

    def test_buffer_full_transcript(self):
        """Test getting full transcript text."""
        buffer = TranscriptBuffer()

        buffer.add_segment({"speaker": "Speaker 1", "text": "Hello"})
        buffer.add_segment({"speaker": "Speaker 2", "text": "World"})

        transcript = buffer.get_full_transcript()
        assert "Hello" in transcript
        assert "World" in transcript


class TestCoachingAnalyzer:
    """Tests for coaching analyzer."""

    def test_analyzer_init(self):
        """Test analyzer initialization."""
        analyzer = CoachingAnalyzer()
        assert analyzer.call_start_time is None
        assert analyzer.call_metrics["total_segments"] == 0

    @pytest.mark.asyncio
    async def test_analyze_segment_normal(self):
        """Test analyzing normal segment without issues."""
        analyzer = CoachingAnalyzer()

        segment = {
            "speaker": "Speaker 1",
            "text": "Tell me about your quality measures",
            "confidence": 0.95,
            "timestamp": 0,
        }

        result = await analyzer.analyze_segment(segment)

        # No objection/transition, and no pause = no coaching
        assert result is None
        assert analyzer.call_metrics["total_segments"] == 1

    @pytest.mark.asyncio
    async def test_detect_price_objection(self):
        """Test detecting price objection."""
        analyzer = CoachingAnalyzer()

        segment = {
            "speaker": "Speaker 1",
            "text": "This sounds expensive. What's the cost?",
            "confidence": 0.95,
            "timestamp": 0,
        }

        result = await analyzer.analyze_segment(segment, persona_type="cmo")

        # Should queue objection suggestion
        assert analyzer.call_metrics["objections_detected"] == 1
        assert len(analyzer.pause_detector.pending_suggestions) == 1

    @pytest.mark.asyncio
    async def test_detect_timing_objection(self):
        """Test detecting timing objection."""
        analyzer = CoachingAnalyzer()

        segment = {
            "speaker": "Speaker 1",
            "text": "We're not ready for something new right now",
            "confidence": 0.95,
            "timestamp": 0,
        }

        result = await analyzer.analyze_segment(segment, persona_type="cfo")

        assert analyzer.call_metrics["objections_detected"] == 1

    @pytest.mark.asyncio
    async def test_detect_risk_objection(self):
        """Test detecting risk/compliance objection."""
        analyzer = CoachingAnalyzer()

        segment = {
            "speaker": "Speaker 1",
            "text": "What about security and compliance?",
            "confidence": 0.95,
            "timestamp": 0,
        }

        result = await analyzer.analyze_segment(segment)

        assert analyzer.call_metrics["objections_detected"] == 1

    @pytest.mark.asyncio
    async def test_generate_objection_reframe(self):
        """Test objection reframe generation."""
        analyzer = CoachingAnalyzer()

        segment = {
            "speaker": "Speaker 1",
            "text": "This is too expensive for our budget",
            "confidence": 0.95,
            "timestamp": 0,
        }

        result = await analyzer.analyze_segment(segment, persona_type="cmo")

        # Check that suggestion was queued
        suggestion = analyzer.pause_detector.get_next_suggestion()
        assert suggestion is not None
        assert "ROI" in suggestion.get("reframe", "")

    @pytest.mark.asyncio
    async def test_get_call_summary(self):
        """Test call summary generation."""
        analyzer = CoachingAnalyzer()

        segment = {
            "speaker": "Speaker 1",
            "text": "Test message",
            "confidence": 0.95,
            "timestamp": 0,
        }

        await analyzer.analyze_segment(segment)

        summary = analyzer.get_call_summary()
        assert "duration_seconds" in summary
        assert "total_segments" in summary
        assert "speakers" in summary
        assert summary["total_segments"] == 1


class TestCoachingWebSocketClient:
    """Tests for WebSocket client."""

    def test_client_init_mock_mode(self):
        """Test WebSocket client in mock mode."""
        client = CoachingWebSocketClient(
            user_id="user-123",
            jwt_token="token-abc",
            tenant_id="tenant-1",
            mock_mode=True,
        )

        assert client.user_id == "user-123"
        assert client.mock_mode is True
        assert client.connected is False

    @pytest.mark.asyncio
    async def test_client_connect_mock_mode(self):
        """Test connecting in mock mode."""
        client = CoachingWebSocketClient(
            user_id="user-123",
            jwt_token="token-abc",
            tenant_id="tenant-1",
            mock_mode=True,
        )

        result = await client.connect()
        assert result is True
        assert client.connected is True

    @pytest.mark.asyncio
    async def test_client_send_coaching_mock_mode(self):
        """Test sending coaching in mock mode."""
        client = CoachingWebSocketClient(
            user_id="user-123",
            jwt_token="token-abc",
            tenant_id="tenant-1",
            mock_mode=True,
        )

        await client.connect()

        result = await client.send_coaching({"message": "Test coaching"})
        assert result is True

    @pytest.mark.asyncio
    async def test_client_send_transcript_mock_mode(self):
        """Test sending transcript in mock mode."""
        client = CoachingWebSocketClient(
            user_id="user-123",
            jwt_token="token-abc",
            tenant_id="tenant-1",
            mock_mode=True,
        )

        await client.connect()

        result = await client.send_transcript_update("Speaker 1", "Hello world")
        assert result is True

    @pytest.mark.asyncio
    async def test_client_disconnect(self):
        """Test disconnecting."""
        client = CoachingWebSocketClient(
            user_id="user-123",
            jwt_token="token-abc",
            tenant_id="tenant-1",
            mock_mode=True,
        )

        await client.connect()
        assert client.connected is True

        await client.disconnect()
        assert client.connected is False

    @pytest.mark.asyncio
    async def test_client_is_connected(self):
        """Test is_connected status check."""
        client = CoachingWebSocketClient(
            user_id="user-123",
            jwt_token="token-abc",
            tenant_id="tenant-1",
            mock_mode=True,
        )

        assert client.is_connected() is False

        await client.connect()
        assert client.is_connected() is True

        await client.disconnect()
        assert client.is_connected() is False


# Integration tests
class TestCoachingIntegration:
    """Integration tests for coaching components."""

    @pytest.mark.asyncio
    async def test_full_coaching_workflow(self):
        """Test complete coaching workflow: analyze → detect → send."""
        analyzer = CoachingAnalyzer()
        ws_client = CoachingWebSocketClient(
            user_id="user-123",
            jwt_token="token-abc",
            tenant_id="tenant-1",
            mock_mode=True,
        )

        await ws_client.connect()

        # Analyze objection
        segment = {
            "speaker": "Speaker 1",
            "text": "This seems like a lot of cost to implement",
            "confidence": 0.95,
            "timestamp": 0,
        }

        coaching = await analyzer.analyze_segment(segment, persona_type="cfo")

        # Should have queued suggestion (or returned if pause detected)
        suggestion = analyzer.pause_detector.get_next_suggestion()
        assert suggestion is not None

        # Send via WebSocket
        result = await ws_client.send_coaching(suggestion)
        assert result is True

        await ws_client.disconnect()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

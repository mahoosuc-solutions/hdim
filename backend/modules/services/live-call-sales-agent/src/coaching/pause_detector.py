"""Pause detection for optimal coaching message delivery."""

import logging
from collections import deque
from typing import Optional, List

logger = logging.getLogger(__name__)


class PauseDetector:
    """
    Detects speaking pauses to avoid interrupting active conversation.

    **Strategy:**
    - Buffer coaching suggestions while speaker is active
    - Detect pause when no speech for 2-5 seconds
    - Release oldest suggestion when pause detected
    - Prevent message spam with queue management

    **Parameters:**
    - pause_threshold_ms: Minimum silence duration to trigger release (default: 2000ms)
    - max_pending: Maximum queued suggestions before oldest discarded (default: 10)
    """

    def __init__(self, pause_threshold_ms: int = 2000, max_pending: int = 10):
        """Initialize pause detector."""
        self.pause_threshold_ms = pause_threshold_ms
        self.max_pending = max_pending
        self.last_speech_time_ms = 0
        self.pending_suggestions: deque = deque(maxlen=max_pending)
        self.suggestion_sent_count = 0

    def process_transcript_segment(
        self, segment: dict, current_time_ms: int
    ) -> None:
        """
        Process incoming transcript segment and update state.

        **Args:**
            segment: Transcript segment with text, speaker, confidence
            current_time_ms: Current timestamp in milliseconds
        """
        # Update last speech time
        self.last_speech_time_ms = current_time_ms

        # Queue coaching suggestion if present
        if segment.get("coaching_suggestion"):
            if len(self.pending_suggestions) >= self.max_pending:
                discarded = self.pending_suggestions.popleft()
                logger.debug(
                    f"⚠️  Discarded oldest suggestion (queue full): {discarded.get('message', 'N/A')[:50]}"
                )
            self.pending_suggestions.append(segment["coaching_suggestion"])

    def should_send_coaching(self, current_time_ms: int) -> bool:
        """
        Check if pause threshold exceeded and suggestions pending.

        **Strategy:**
        - Calculate time since last speech
        - Return True only if pause detected AND suggestions queued
        - This prevents false positives during silence

        **Args:**
            current_time_ms: Current timestamp in milliseconds

        **Returns:**
            True if pause detected and suggestions pending, False otherwise
        """
        time_since_speech = current_time_ms - self.last_speech_time_ms

        pause_detected = time_since_speech >= self.pause_threshold_ms
        has_suggestions = len(self.pending_suggestions) > 0

        return pause_detected and has_suggestions

    def get_next_suggestion(self) -> Optional[dict]:
        """
        Get next suggestion to send (FIFO queue).

        **Returns:**
            Next suggestion dict or None if queue empty
        """
        if not self.pending_suggestions:
            return None

        suggestion = self.pending_suggestions.popleft()
        self.suggestion_sent_count += 1
        logger.info(
            f"✅ Sending suggestion #{self.suggestion_sent_count}: {suggestion.get('message', 'N/A')[:60]}"
        )
        return suggestion

    def queue_suggestion(self, suggestion: dict) -> bool:
        """
        Manually queue a coaching suggestion.

        **Args:**
            suggestion: Coaching suggestion dict

        **Returns:**
            True if queued, False if queue at capacity
        """
        if len(self.pending_suggestions) >= self.max_pending:
            logger.warning(f"⚠️  Cannot queue suggestion - queue at capacity ({self.max_pending})")
            return False

        self.pending_suggestions.append(suggestion)
        logger.debug(f"📋 Queued suggestion: {suggestion.get('message', 'N/A')[:60]}")
        return True

    def clear_queue(self) -> int:
        """
        Clear all pending suggestions.

        **Returns:**
            Number of suggestions discarded
        """
        count = len(self.pending_suggestions)
        self.pending_suggestions.clear()
        logger.info(f"🗑️  Cleared {count} pending suggestions")
        return count

    def get_status(self) -> dict:
        """Get pause detector status for monitoring."""
        return {
            "pending_suggestions": len(self.pending_suggestions),
            "suggestions_sent": self.suggestion_sent_count,
            "last_speech_time_ms": self.last_speech_time_ms,
            "pause_threshold_ms": self.pause_threshold_ms,
            "max_pending": self.max_pending,
        }


class TranscriptBuffer:
    """
    Buffers recent transcript segments for context.

    Used by coaching analyzer to provide context when generating suggestions.

    **Parameters:**
    - max_segments: Maximum segments to retain (default: 20, ~60 seconds)
    """

    def __init__(self, max_segments: int = 20):
        """Initialize transcript buffer."""
        self.max_segments = max_segments
        self.segments: deque = deque(maxlen=max_segments)

    def add_segment(self, segment: dict) -> None:
        """Add transcript segment to buffer."""
        self.segments.append(segment)

    def get_recent(self, count: int = 5) -> List[dict]:
        """Get most recent N segments."""
        return list(self.segments)[-count:] if self.segments else []

    def get_by_speaker(self, speaker: str) -> List[dict]:
        """Get all segments from specific speaker."""
        return [s for s in self.segments if s.get("speaker") == speaker]

    def get_full_transcript(self) -> str:
        """Get full transcript text."""
        return " ".join(s.get("text", "") for s in self.segments)

    def clear(self) -> int:
        """Clear buffer and return count."""
        count = len(self.segments)
        self.segments.clear()
        return count

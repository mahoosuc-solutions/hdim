"""Tests for Meet bot service."""

import pytest
import asyncio
from src.meet_bot.auth import GoogleAuthManager
from src.meet_bot.bot import MeetBot
from src.transcription.google_speech import SpeechClient


class TestGoogleAuthManager:
    """Tests for Google authentication manager."""

    def test_auth_manager_init_no_credentials(self):
        """Test auth manager initialization with missing credentials file."""
        # Should handle gracefully when credentials file doesn't exist
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        assert auth.credentials is None
        assert not auth.is_authenticated()

    def test_auth_manager_access_token_when_not_authenticated(self):
        """Test getting access token when not authenticated."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        token = auth.get_access_token()
        assert token is None


class TestMeetBot:
    """Tests for Meet bot."""

    @pytest.mark.asyncio
    async def test_bot_init_mock_mode(self):
        """Test bot initialization in mock mode."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        bot = MeetBot(auth, mock_mode=True)

        assert bot.mock_mode is True
        assert bot.is_joined is False
        assert bot.joined_at is None

    @pytest.mark.asyncio
    async def test_bot_join_meeting_mock_mode(self):
        """Test bot joining meeting in mock mode."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        bot = MeetBot(auth, mock_mode=True)

        result = await bot.join_meeting("https://meet.google.com/abc-defg-hij")

        assert result is True
        assert bot.is_joined is True
        assert bot.joined_at is not None
        assert bot.meeting_url == "https://meet.google.com/abc-defg-hij"

    @pytest.mark.asyncio
    async def test_bot_leave_meeting(self):
        """Test bot leaving meeting."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        bot = MeetBot(auth, mock_mode=True)

        # Join first
        await bot.join_meeting("https://meet.google.com/abc-defg-hij")
        assert bot.is_joined is True

        # Leave
        result = await bot.leave_meeting()
        assert result is True
        assert bot.is_joined is False

    @pytest.mark.asyncio
    async def test_bot_leave_when_not_joined(self):
        """Test leaving when bot is not joined."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        bot = MeetBot(auth, mock_mode=True)

        result = await bot.leave_meeting()
        assert result is False  # Should fail gracefully

    @pytest.mark.asyncio
    async def test_bot_get_status(self):
        """Test getting bot status."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        bot = MeetBot(auth, mock_mode=True)

        # Before joining
        status = bot.get_status()
        assert status["is_joined"] is False
        assert status["mock_mode"] is True

        # Join and check status
        await bot.join_meeting("https://meet.google.com/test")
        status = bot.get_status()
        assert status["is_joined"] is True
        assert status["meeting_url"] == "https://meet.google.com/test"
        assert status["duration_seconds"] > 0


class TestSpeechClient:
    """Tests for Speech-to-Text client."""

    def test_speech_client_init_mock_mode(self):
        """Test speech client in mock mode."""
        client = SpeechClient(mock_mode=True)
        assert client.mock_mode is True
        assert client.client is None

    @pytest.mark.asyncio
    async def test_speech_client_mock_transcription(self):
        """Test mock transcription."""
        client = SpeechClient(mock_mode=True)

        segments = []
        async for segment in client.stream_transcribe(None):
            segments.append(segment)

        # Should have multiple mock segments
        assert len(segments) > 0

        # First segment should have expected structure
        first = segments[0]
        assert "speaker" in first
        assert "text" in first
        assert "confidence" in first
        assert "is_final" in first
        assert "timestamp" in first

    @pytest.mark.asyncio
    async def test_speech_client_mock_speaker_diarization(self):
        """Test mock speaker diarization."""
        client = SpeechClient(mock_mode=True)

        speakers = set()
        async for segment in client.stream_transcribe(None):
            speakers.add(segment["speaker"])

        # Should have multiple speakers in mock data
        assert len(speakers) > 1
        assert "Speaker 1" in speakers
        assert "Speaker 2" in speakers

    @pytest.mark.asyncio
    async def test_speech_client_word_offsets(self):
        """Test word-level timestamp extraction for pause detection."""
        client = SpeechClient(mock_mode=True)

        async for segment in client.stream_transcribe(None):
            words = segment.get("words", [])
            if words:
                # Should have word-level timestamps
                assert len(words) > 0
                first_word = words[0]
                assert "word" in first_word
                assert "start_time" in first_word
                assert "end_time" in first_word
                break  # Just test first segment with words


# Integration tests
class TestBotIntegration:
    """Integration tests for bot and transcription together."""

    @pytest.mark.asyncio
    async def test_bot_join_and_capture_mock_transcripts(self):
        """Test bot join and audio capture in mock mode."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        bot = MeetBot(auth, mock_mode=True)
        speech_client = SpeechClient(mock_mode=True)

        # Join meeting
        joined = await bot.join_meeting("https://meet.google.com/test")
        assert joined is True

        # Capture and transcribe
        transcripts = []
        async for segment in bot.capture_audio_stream():
            transcripts.append(segment)

        # Should have transcripts
        assert len(transcripts) > 0

        # Leave meeting
        left = await bot.leave_meeting()
        assert left is True

    @pytest.mark.asyncio
    async def test_end_to_end_call_workflow(self):
        """Test complete call workflow: join → transcribe → leave."""
        auth = GoogleAuthManager("/nonexistent/path/credentials.json")
        bot = MeetBot(auth, mock_mode=True)

        # Step 1: Join
        meeting_url = "https://meet.google.com/abc-defg-hij"
        joined = await bot.join_meeting(meeting_url)
        assert joined is True

        # Step 2: Get status
        status = bot.get_status()
        assert status["is_joined"] is True
        assert status["meeting_url"] == meeting_url

        # Step 3: Capture audio (collect few segments)
        segments_captured = 0
        async for segment in bot.capture_audio_stream():
            assert "speaker" in segment
            assert "text" in segment
            segments_captured += 1
            if segments_captured >= 2:  # Just capture a couple
                break

        assert segments_captured >= 2

        # Step 4: Leave
        left = await bot.leave_meeting()
        assert left is True

        # Step 5: Verify not joined
        status = bot.get_status()
        assert status["is_joined"] is False


if __name__ == "__main__":
    pytest.main([__file__, "-v"])

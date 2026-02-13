# Live Call Sales Agent

Real-time AI coaching bot for Google Meet sales discovery calls.

## Overview

The Live Call Sales Agent is an intelligent coaching system that:

- **Joins Google Meet** calls as a silent bot participant
- **Captures real-time transcription** with speaker diarization
- **Analyzes conversations** against existing discovery playbooks
- **Sends coaching messages** via WebSocket to a separate browser window
- **Stores call data** in PostgreSQL for compliance and analytics

## Architecture

### Core Components

1. **Meet Bot Service** (Python/FastAPI)
   - Joins Google Meet calls via service account authentication
   - Captures audio via WebRTC
   - Streams to Google Speech-to-Text API

2. **Coaching Engine**
   - Analyzes transcripts against 5 customer personas
   - Detects objections and phase transitions
   - Generates real-time coaching suggestions

3. **Pause Detector**
   - Waits 2-5 seconds for speaking pause
   - Prevents interrupting active conversation
   - Queues suggestions for optimal delivery

4. **Multi-Storage**
   - **Redis**: Active call state (2-hour TTL)
   - **File Storage**: Full call transcripts
   - **PostgreSQL**: Call metadata and analytics

## Getting Started

### Prerequisites

- Python 3.11+
- Docker and Docker Compose
- Google Cloud Project with service account credentials
- Redis 7+
- PostgreSQL 16+

### Installation

1. **Copy example config:**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

2. **Install dependencies:**
   ```bash
   pip install -r requirements.txt
   ```

3. **Set up Google Cloud credentials:**
   ```bash
   # Download service account JSON from Google Cloud Console
   mkdir -p ../../../../../../secrets
   cp your-service-account.json ../../../../../../secrets/google-meet-service-account.json
   chmod 600 ../../../../../../secrets/google-meet-service-account.json
   ```

### Development

**Run FastAPI service locally:**
```bash
# Start in development mode (hot reload)
python -m uvicorn src.main:app --reload --port 8095

# Or run directly
python src/main.py
```

**Test health endpoint:**
```bash
curl http://localhost:8095/health
# Response: {"status":"healthy","service":"live-call-sales-agent","version":"1.0.0"}
```

**View API documentation:**
- Interactive: http://localhost:8095/docs
- ReDoc: http://localhost:8095/redoc

## API Endpoints

### Meet Bot Control

**Join a meeting:**
```http
POST /api/meet/join
Content-Type: application/json

{
  "meeting_url": "https://meet.google.com/abc-defg-hij",
  "user_id": "user-123",
  "tenant_id": "acme-health-plan",
  "customer_name": "ACME Health Plan",
  "persona_type": "cmo"
}
```

**Leave current meeting:**
```http
POST /api/meet/leave/{user_id}
```

**Get call status:**
```http
GET /api/meet/status/{user_id}
```

### Coaching

**Generate coaching suggestion:**
```http
POST /api/sales/coach/live-call
Content-Type: application/json

{
  "transcript_history": [...],
  "current_segment": {...},
  "persona_type": "cmo",
  "call_phase": "pain_discovery"
}
```

## Database Schema

### Tables (customer_deployments_db)

**lc_deployments**
- Stores customer deployment information
- Fields: id, tenant_id, customer_name, deployment_status, contract_value, pilot dates, success_metrics

**lc_call_transcripts**
- Stores call metadata and transcript files
- Fields: id, tenant_id, deployment_id, call_date, duration, persona_type, qualification_status, call_score, sentiment_score, transcript_file_path, pain_points_discovered

**lc_coaching_sessions**
- Stores coaching session analytics
- Fields: id, tenant_id, call_transcript_id, session_type, coaching_count, objections_detected, phase_transitions, avg_response_score, effectiveness_rating

### Migrations

All migrations are managed via Liquibase in `src/main/resources/db/changelog/`:

- `0000-enable-extensions.xml` - Enable PostgreSQL extensions
- `0001-create-lc-deployments-table.xml` - Deployments table
- `0002-create-lc-call-transcripts-table.xml` - Call transcripts table
- `0003-create-lc-coaching-sessions-table.xml` - Coaching sessions table

## Testing

**Run tests:**
```bash
pytest tests/

# With coverage:
pytest tests/ --cov=src --cov-report=html
```

**Run specific test:**
```bash
pytest tests/test_meet_bot.py::test_join_meeting
```

## Docker Deployment

**Build Docker image:**
```bash
docker build -t hdim/live-call-sales-agent:latest .
```

**Run in Docker:**
```bash
docker run -it \
  -p 8095:8095 \
  -e GOOGLE_APPLICATION_CREDENTIALS=/secrets/google-meet-service-account.json \
  -v $(pwd)/secrets:/secrets:ro \
  -v $(pwd)/.env:/.env:ro \
  hdim/live-call-sales-agent:latest
```

**Docker Compose integration:**
```yaml
services:
  live-call-sales-agent:
    container_name: live-call-sales-agent
    build:
      context: ./backend/modules/services/live-call-sales-agent
      dockerfile: Dockerfile
    ports:
      - "8095:8095"
    environment:
      - GOOGLE_APPLICATION_CREDENTIALS=/secrets/google-meet-service-account.json
      - AI_SALES_AGENT_URL=http://ai-sales-agent:8090
      - POSTGRES_HOST=postgres
      - REDIS_HOST=redis
    volumes:
      - ./secrets:/secrets:ro
    networks:
      - healthdata-network
    depends_on:
      - postgres
      - redis
      - ai-sales-agent
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVICE_PORT` | 8095 | Service listening port |
| `DEBUG` | false | Enable debug mode (hot reload) |
| `GOOGLE_MEET_API_ENABLED` | true | Enable Google Meet API integration |
| `GOOGLE_SPEECH_API_ENABLED` | true | Enable Google Speech-to-Text API |
| `AI_SALES_AGENT_URL` | http://ai-sales-agent:8090 | AI Sales Agent service URL |
| `POSTGRES_HOST` | localhost | PostgreSQL host |
| `POSTGRES_DB` | customer_deployments_db | Database name |
| `REDIS_HOST` | localhost | Redis host |
| `PAUSE_THRESHOLD_MS` | 2000 | Pause detection threshold (milliseconds) |
| `MOCK_GOOGLE_MEET` | false | Mock Google Meet (testing mode) |
| `MOCK_GOOGLE_SPEECH` | false | Mock Google Speech-to-Text (testing mode) |

## Integration with HDIM Services

### AI Sales Agent

The coaching engine integrates with the AI Sales Agent to leverage:

- **5 Customer Personas** (CMO, CFO, Provider, Coordinator, IT Leader)
- **Discovery Playbook** (4-phase call script)
- **Observable SLO Talking Points**

### WebSocket Infrastructure

Uses HDIM's native WebSocket pattern for coaching UI communication:

- JWT authentication via URL parameter
- Topic-based message routing: `/topic/sales-coaching/{userId}`
- Auto-reconnection with exponential backoff
- HIPAA-compliant rate limiting and audit logging

### Multi-Tenant Isolation

All queries filter by `tenant_id` to ensure:

- Customer data isolation
- HIPAA compliance
- Audit trail per tenant

## Development Roadmap

### Phase 0 (Current) - Database Setup ✅
- [x] Database schema with Liquibase migrations
- [x] Python service skeleton
- [x] Mock FastAPI endpoints

### Phase 1 - Meet Bot Service (Week 1)
- [ ] Real Google Meet authentication (service account + OAuth fallback)
- [ ] Audio capture via WebRTC
- [ ] Google Speech-to-Text streaming integration
- [ ] Speaker diarization

### Phase 2 - Coaching Engine (Week 2)
- [ ] Transcript analysis against playbooks
- [ ] Objection detection (6 types)
- [ ] Phase transition identification
- [ ] Coaching suggestion generation
- [ ] Pause detection (2-5s buffer)

### Phase 3 - Coaching UI (Week 2)
- [ ] Separate Angular app for coaching window
- [ ] WebSocket connection to receive suggestions
- [ ] Real-time transcript display
- [ ] Message severity color coding (red/orange/green)

### Phase 4 - Docker Deployment (Week 3)
- [ ] Production Dockerfile with Chrome
- [ ] Docker Compose integration
- [ ] Health checks and monitoring
- [ ] Secret management

## Troubleshooting

### Service won't start

```bash
# Check Python version
python --version  # Should be 3.11+

# Verify dependencies installed
pip list | grep fastapi

# Check port availability
lsof -i :8095
```

### Google Meet API errors

1. Verify service account credentials:
   ```bash
   python -c "from google.oauth2 import service_account; print('✅ Credentials loaded')"
   ```

2. Check API is enabled in Google Cloud Console:
   - Google Meet API
   - Google Speech-to-Text API

3. Verify service account has required permissions:
   - `meet.meetings.bot.join`
   - `speech.recognize`

### Database connection errors

```bash
# Test PostgreSQL connection
psql -h localhost -U healthdata -d customer_deployments_db -c "SELECT 1"

# Check migrations applied
docker compose exec live-call-sales-agent python -c "from liquibase import Liquibase; ..."
```

### WebSocket connection errors

1. Verify Clinical Workflow Service is running:
   ```bash
   curl http://localhost:8080/health
   ```

2. Check WebSocket endpoint URL in config:
   ```bash
   echo $WEBSOCKET_HOST:$WEBSOCKET_PORT
   ```

## Contributing

When adding new features:

1. Add corresponding Liquibase migration if modifying schema
2. Include unit and integration tests
3. Document new endpoints in API section above
4. Update this README with changes

## License

Proprietary - HealthData-in-Motion Platform

## Support

For issues or questions:
- Check [HDIM Documentation Portal](../../../../../../docs/README.md)
- Review [Database Guide](../../../../../../backend/docs/DATABASE_ARCHITECTURE_GUIDE.md)
- Check [Liquibase Guide](../../../../../../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md)

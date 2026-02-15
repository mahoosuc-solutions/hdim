# HDIM AI VP Sales Agent

**Purpose:** Autonomously execute VP Sales responsibilities for Phase 2 launch (March 1, 2026).

**Goals for March:**
- Execute 50-100 discovery calls
- Sign 1-2 LOIs
- Achieve $50-100K committed revenue
- Call quality score average >80/100

---

## Architecture

**Multi-Agent Team:**
```
Coordinator Agent (Sales Director)
├── Routes incoming customer interactions
├── Tracks pipeline health
└── Makes strategic prioritization decisions

Worker Agents:
├── Discovery Agent (Week 1 - READY)
│   └── Executes 30-minute discovery calls with 5 personas
├── Demo Agent (Week 2 - PLANNED)
│   └── Generates & delivers persona-specific demos (15/30/45 min)
├── Objection Agent (Month 2 - PLANNED)
│   └── Handles 6 common objections with real-time reframes
└── Pipeline Agent (Month 2 - PLANNED)
    └── Scores deals, identifies stalled opportunities, forecasts revenue
```

---

## Phase 1: Discovery Agent (Week 1)

### Service Entry Point
```bash
cd backend/modules/services/ai-sales-agent
python -m uvicorn src.main:app --reload --port 8090
```

### API Endpoints

**Execute Discovery Call:**
```bash
POST /api/sales/discovery-call
Content-Type: application/json

{
  "customer_name": "HealthFirst Insurance",
  "customer_context": "500K members, CMO on call, pain: manual gap closure",
  "persona_type": "cmo",
  "call_duration_minutes": 30
}

Response:
{
  "call_transcript": [...],
  "qualification": "green|yellow|red",
  "pain_points_discovered": [...],
  "next_steps": "Schedule 30-min demo on Feb 20",
  "call_score": 85.0,
  "crm_logged": true
}
```

**Get Pipeline Health:**
```bash
GET /api/sales/pipeline
Response:
{
  "total_leads": 47,
  "stage_distribution": {
    "discovery": 15,
    "demo_scheduled": 8,
    "proposal": 3,
    "negotiation": 1
  },
  "weighted_pipeline": "$2.4M",
  "expected_close_rate": "25-35%",
  "forecasted_revenue_30days": "$150-250K"
}
```

---

## File Structure

```
backend/modules/services/ai-sales-agent/
├── pyproject.toml                    # Dependencies & config
├── README.md                         # You're reading it
├── src/
│   ├── __init__.py
│   ├── main.py                      # FastAPI service
│   ├── config.py                    # Configuration
│   ├── agents/
│   │   ├── __init__.py
│   │   ├── coordinator.py           # Sales Director
│   │   ├── discovery_agent.py       # Discovery call executor
│   │   ├── demo_agent.py            # Demo generator (Week 2)
│   │   ├── objection_agent.py       # Objection handler (Month 2)
│   │   ├── pipeline_agent.py        # Pipeline analyzer (Month 2)
│   │   └── tools.py                 # Shared tool definitions
│   ├── knowledge/
│   │   ├── __init__.py
│   │   ├── discovery_playbook.py    # 30-min discovery script
│   │   ├── demo_playbook.py         # Demo flow by persona
│   │   ├── objection_playbook.py    # 6 objections + reframes
│   │   ├── personas.py              # 5 customer personas
│   │   └── slo_data.py              # Observable SLO talking points
│   ├── integrations/
│   │   ├── __init__.py
│   │   └── crm_client.py            # CRM API integration
│   └── models/
│       ├── __init__.py
│       └── discovery_call.py        # Pydantic models
├── tests/
│   ├── __init__.py
│   ├── test_discovery_agent.py      # Discovery call tests
│   ├── test_demo_agent.py           # Demo generation tests
│   ├── test_objection_agent.py      # Objection handling tests
│   └── test_pipeline_agent.py       # Pipeline analysis tests
└── docker/
    └── Dockerfile                   # Container image
```

---

## Development Workflow

### Install Dependencies
```bash
cd backend/modules/services/ai-sales-agent
pip install -e ".[dev]"
```

### Run Service
```bash
python -m uvicorn src.main:app --reload --port 8090
```

### Run Tests
```bash
pytest -v --cov=src
```

### Test Discovery Agent
```bash
# Execute mock discovery call
python -c "
from src.agents.discovery_agent import DiscoveryAgent
agent = DiscoveryAgent()
result = agent.execute_discovery_call(
    customer_name='HealthFirst Insurance',
    persona_type='cmo'
)
print(result)
"
```

---

## Integration Points

### CRM Integration
- Logs discovery calls to Salesforce / HubSpot
- Auto-scores leads (green/yellow/red)
- Schedules next steps

### HDIM Knowledge Base
- Observable SLO contracts (Jaeger dashboard)
- Competitive battlecards
- Customer success metrics

### Email Integration (Future)
- Auto-sends follow-up emails after calls
- Schedules demo meeting invites

---

## Phase Roadmap

| Phase | Timeline | Agents | Status |
|-------|----------|--------|--------|
| **Phase 1** | Feb 15-21 | Discovery | 🚀 IN PROGRESS |
| **Phase 2** | Feb 22-28 | Demo, Objection | ⏳ Planned |
| **Phase 3** | Mar 1-15 | Pipeline | ⏳ Planned |
| **Production** | Mar 1+ | All 4 agents live | ⏳ Planned |

---

## Success Metrics

### Phase 1 (Discovery Agent)
- ✅ 5+ mock discovery calls pass testing
- ✅ Qualification logic accurate (green/yellow/red)
- ✅ Call scoring rubric returns 0-100 scale
- ✅ CRM integration working (call notes logged)
- ✅ Call transcripts available for human review

### Overall (All Agents)
- 50-100 autonomous discovery calls (Mar 2026)
- 1-2 LOIs signed
- $50-100K committed revenue
- 80+ average call quality score
- <5% hallucination rate
- Zero security/compliance issues

---

## Critical Dependencies

- **Anthropic SDK:** claude-3.5-sonnet or higher
- **FastAPI:** For HTTP service
- **Pydantic:** Data validation
- **CRM API:** Salesforce or HubSpot (environment variable)

---

## Security & Compliance

- ✅ No PHI in logs (HIPAA §164.312(b))
- ✅ Human-in-the-loop for LOIs >$50K
- ✅ All agent decisions auditable
- ✅ Call transcripts retained for QA
- ✅ Real-time monitoring dashboard

---

## Questions?

- Architecture: See `IMPLEMENTATION_PLAN.md`
- Discovery Script: See `src/knowledge/discovery_playbook.py`
- Test Cases: See `tests/test_discovery_agent.py`
- Deployment: See `docker/Dockerfile`

---

_Last Updated: February 14, 2026_
_Phase 1: Discovery Agent (Feb 15-21)_
_Launch Target: March 1, 2026_

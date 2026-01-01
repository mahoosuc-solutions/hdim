# HDIM AI Image Generation with Quality Control

Automated visual asset generation using Google Gemini 2.0 Flash with human-in-the-loop quality control.

## Quick Start

```bash
# 1. Set your API key
export GOOGLE_API_KEY='your-google-ai-api-key'

# 2. Install dependencies
./start.sh install

# 3. Start the review portal
./start.sh portal
# Opens at http://127.0.0.1:5555

# 4. In another terminal, generate an asset
./start.sh generate HERO-01
```

## Prerequisites

- Python 3.9+
- Google AI API key ([Get one here](https://aistudio.google.com/app/apikey))
- 500MB disk space for generated assets

## Commands

| Command | Description |
|---------|-------------|
| `./start.sh portal` | Start the web review portal |
| `./start.sh generate <ID>` | Generate a specific asset |
| `./start.sh priority` | Generate all high-priority assets |
| `./start.sh list` | List all available assets |
| `./start.sh install` | Install Python dependencies |
| `./start.sh check` | Check system dependencies |

## How It Works

### Generation + QC Loop

```
┌─────────────┐     ┌────────────┐     ┌─────────────┐
│   Generate  │────▶│   Quality  │────▶│   Human     │
│   Image     │     │   Control  │     │   Review    │
└─────────────┘     └────────────┘     └─────────────┘
       ▲                                      │
       │         ┌────────────────────┐       │
       └─────────│ Feedback + Refined │◀──────┘
                 │      Prompt        │
                 └────────────────────┘
```

1. **Generate**: Gemini 2.0 Flash creates image from prompt
2. **QC Engine**: Automated verification runs:
   - **Text Accuracy**: OCR extracts text, spell-checks against dictionary
   - **Clarity Score**: Vision AI rates quality 0-100
   - **Technical**: Validates dimensions, file size, format
3. **Human Review**: You approve, reject with feedback, or edit prompt
4. **Iterate**: If rejected, prompt is refined and new version generated
5. **Publish**: Approved assets are copied to production directory

## Web Portal

### Dashboard (`/`)
- Overview of all assets organized by category
- Stats: total, pending review, approved, rejected, published
- Quick links to individual asset reviews

### Review Page (`/review/<asset_id>`)
- Side-by-side comparison with previous version
- Full quality report with metrics
- Actions: Approve, Reject & Regenerate, Edit Prompt
- Version history showing all iterations

### Batch Review (`/batch`)
- Review multiple pending assets at once
- Quick approve/reject buttons
- Bulk operations

### Publish Page (`/publish`)
- List of approved assets ready to publish
- Batch publish to production directory
- Publishing history

## Asset Categories

| Category | Assets | Description |
|----------|--------|-------------|
| hero | 3 | Hero images (desktop, mobile, social) |
| portraits | 3 | Patient story portraits |
| badges | 5 | Trust badges (FHIR, CQL, HIPAA, tests, uptime) |
| dashboards | 3 | Product screenshots |
| icons | 3 | Feature icons |
| charts | 2 | Data visualizations |
| process | 2 | Integration diagrams |
| team | 2 | Team photos |
| backgrounds | 2 | Subtle patterns |
| social | 1 | Social media template |

### High Priority Assets (11)
Generated first when using `./start.sh priority`:

1. HERO-01 - Main desktop hero
2. HERO-02 - Mobile hero
3. HERO-03 - Social share image
4. PORTRAIT-MARIA - Patient story (diabetes)
5. PORTRAIT-JAMES - Patient story (depression)
6. PORTRAIT-SARAH - Patient story (cancer survivor)
7. BADGE-FHIR - FHIR R4 certification badge
8. BADGE-CQL - CQL standards badge
9. BADGE-HIPAA - HIPAA compliance badge
10. BADGE-TESTS - Test coverage badge
11. BADGE-UPTIME - 99.9% uptime badge

## Quality Metrics

### Text Accuracy
- **PASS**: No misspelled words detected
- **FAIL**: OCR found misspelled text in image

### Clarity Score
- **90-100**: Excellent - professional quality
- **75-89**: Good - minor improvements possible
- **50-74**: Fair - needs attention
- **0-49**: Poor - regenerate recommended

### Technical Validation
- **PASS**: Matches expected dimensions and format
- **WARNING**: Minor deviations (file size, slight dimension mismatch)
- **FAIL**: Significant dimension mismatch (>10%)

### Overall Status
- **READY_FOR_REVIEW**: Passed automated QC, awaiting human review
- **NEEDS_ATTENTION**: Some QC checks failed or warned

## Directory Structure

```
assets/
├── start.sh              # Quick-start script
├── config.py             # Configuration settings
├── models.py             # Data models
├── qc_engine.py          # Quality control engine
├── orchestrator.py       # State management
├── generate_with_qc.py   # Image generation with QC
├── review_portal.py      # Flask web portal
├── requirements.txt      # Python dependencies
├── db_schema.sql         # Database schema
├── templates/            # HTML templates
│   ├── dashboard.html
│   ├── review.html
│   ├── batch.html
│   └── publish.html
├── static/               # CSS/JS assets
│   └── style.css
├── generated/            # Generated images (by category)
│   ├── hero/
│   ├── portraits/
│   └── ...
├── production/           # Published assets
└── hdim_assets.db        # SQLite database (created on first run)
```

## Configuration

Edit `config.py` to customize:

```python
# API Settings
GOOGLE_API_KEY = os.environ.get("GOOGLE_API_KEY")
GEMINI_IMAGE_MODEL = "gemini-2.0-flash-exp"
RATE_LIMIT_SECONDS = 7

# Quality Thresholds
MIN_CLARITY_SCORE = 70
MAX_FILE_SIZE_KB = 2048

# Portal Settings
REVIEW_PORTAL_HOST = "127.0.0.1"
REVIEW_PORTAL_PORT = 5555
```

## Troubleshooting

### "GOOGLE_API_KEY not set"
```bash
export GOOGLE_API_KEY='your-api-key'
```

### "ModuleNotFoundError: No module named 'flask'"
```bash
./start.sh install
```

### "Generation failed: empty response"
- Check API key is valid
- Verify quota at https://aistudio.google.com/
- Wait 7+ seconds between generations

### "Image dimensions differ significantly"
- AI-generated images may not match exact specs
- Review and approve if quality is acceptable
- Reject with feedback to refine prompt

### Portal not loading
- Ensure port 5555 is available
- Check `review_portal.py` for errors
- Try: `python review_portal.py` directly

## API Reference

### Generate Asset
```bash
python generate_with_qc.py --asset HERO-01
python generate_with_qc.py --asset HERO-01 --feedback "Make text larger"
python generate_with_qc.py --priority
```

### Quality Check Only
```bash
python qc_engine.py ./generated/hero/HERO-01.png HERO-01 1
```

### Portal API Endpoints
```
POST /api/approve     {"asset_id": "HERO-01", "version": 1}
POST /api/reject      {"asset_id": "HERO-01", "version": 1, "feedback": "..."}
POST /api/regenerate  {"asset_id": "HERO-01", "prompt": "..."}
POST /api/publish     {"asset_ids": ["HERO-01", "HERO-02"]}
GET  /api/stats
```

## Brand Guidelines

Assets are generated following HDIM brand:

- **Primary Blue**: #0066CC
- **Accent Teal**: #00A5B5
- **Style**: Modern, professional, healthcare-appropriate
- **Tone**: Trustworthy, human, technology-enabled care

## License

Internal use only. Generated assets are subject to Google AI terms of service.

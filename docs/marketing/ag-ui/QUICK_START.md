# AG-UI Quick Start Guide

**Get started generating AI UI content in 5 minutes**

---

## Prerequisites

1. **Python 3.8+** installed
2. **OpenAI API Key** (for DALL-E 3) or **Stability AI API Key** (for Stable Diffusion)
3. **pip** package manager

---

## Installation

```bash
# Navigate to AG-UI directory
cd docs/marketing/ag-ui

# Install dependencies
pip install -r requirements.txt

# Set up API key
export OPENAI_API_KEY="your-openai-api-key"
# OR
export STABILITY_API_KEY="your-stability-api-key"
```

---

## Generate Your First UI

### Single Dashboard

```bash
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --ai-tool dalle-3
```

**Output:** `assets/dashboards/dashboard-clinical-light.png`

### With Custom Metrics

```bash
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --parameters '{
    "total_patients": "50,000",
    "hedis_score": "85.2"
  }'
```

### Batch Generate

```bash
# Generate all dashboard variations
python scripts/ag-ui-generator.py batch-generate \
  --config configs/batch-dashboards.yaml \
  --ai-tool dalle-3
```

---

## Available Templates

### Dashboards
- `dashboard-clinical` - Clinical care management dashboard
- `dashboard-admin` - System administration dashboard
- `dashboard-analytics` - Analytics and reporting dashboard

### Variations
- `light` - Light mode
- `dark` - Dark mode
- `mobile` - Mobile responsive (375x812)

---

## Example Outputs

### Clinical Dashboard (Light)
```
✅ Generated: assets/dashboards/dashboard-clinical-light.png
📁 Asset ID: dashboard-clinical-light-20260114140000
📂 File: assets/dashboards/dashboard-clinical-light.png
```

### Clinical Dashboard (Dark)
```
✅ Generated: assets/dashboards/dashboard-clinical-dark.png
📁 Asset ID: dashboard-clinical-dark-20260114140001
📂 File: assets/dashboards/dashboard-clinical-dark.png
```

---

## Next Steps

1. **Explore Templates**
   - Check `templates/` directory
   - Review `prompts/` for prompt examples
   - Customize templates for your needs

2. **Create Custom Templates**
   - Copy existing template
   - Modify prompt and parameters
   - Add variations

3. **Integrate with Workflows**
   - Add to CI/CD pipelines
   - Schedule batch generation
   - Use in documentation automation

---

## Troubleshooting

### API Key Not Found
```bash
# Set environment variable
export OPENAI_API_KEY="your-api-key"
```

### Template Not Found
```bash
# List available templates
ls templates/

# Use correct template name
python scripts/ag-ui-generator.py generate --template dashboard-clinical
```

### Generation Fails
- Check API quota
- Verify prompt is valid
- Check network connection
- Review error messages

---

## Resources

- **Framework Docs:** `AG_UI_FRAMEWORK.md`
- **Full README:** `README.md`
- **Prompt Examples:** `prompts/dashboard-prompts.md`
- **Template Examples:** `templates/dashboard-clinical.yaml`

---

**AG-UI Quick Start**

*Generate your first AI UI in 5 minutes!*

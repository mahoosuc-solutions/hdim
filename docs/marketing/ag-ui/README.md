# AG-UI: AI-Generated UI Content System

**Complete framework for generating UI mockups, screenshots, and visual assets using AI tools**

---

## Quick Start

### 1. Setup

```bash
# Install dependencies
cd docs/marketing/ag-ui
pip install -r requirements.txt

# Set up API keys
export OPENAI_API_KEY="your-openai-api-key"
export STABILITY_API_KEY="your-stability-api-key"  # Optional
```

### 2. Generate Single UI Mockup

```bash
# Generate clinical dashboard (light mode)
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --ai-tool dalle-3

# Generate with custom parameters
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation dark \
  --parameters '{"total_patients": "25,000", "hedis_score": "85.2"}'
```

### 3. Batch Generate Multiple UIs

```bash
# Create config file (configs/batch-dashboards.yaml)
# Then run:
python scripts/ag-ui-generator.py batch-generate \
  --config configs/batch-dashboards.yaml \
  --ai-tool dalle-3
```

---

## Directory Structure

```
docs/marketing/ag-ui/
├── README.md                    # This file
├── AG_UI_FRAMEWORK.md          # Framework documentation
├── prompts/                    # AI prompt templates
│   ├── dashboard-prompts.md
│   ├── feature-prompts.md
│   └── marketing-prompts.md
├── templates/                  # YAML template files
│   ├── dashboard-clinical.yaml
│   ├── dashboard-admin.yaml
│   └── feature-care-gaps.yaml
├── scripts/                    # Generation scripts
│   └── ag-ui-generator.py
├── assets/                     # Generated assets
│   ├── dashboards/
│   ├── features/
│   └── marketing/
├── metadata/                   # Asset metadata
│   └── *.json
└── configs/                    # Batch generation configs
    └── batch-*.yaml
```

---

## Usage Examples

### Example 1: Generate Dashboard

```bash
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --output assets/dashboards/clinical-light.png
```

### Example 2: Generate with Custom Metrics

```bash
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --parameters '{
    "total_patients": "50,000",
    "open_gaps": "5,200",
    "closed_month": "1,200",
    "hedis_score": "82.5"
  }'
```

### Example 3: Batch Generate All Variations

Create `configs/batch-all-dashboards.yaml`:

```yaml
items:
  - template: dashboard-clinical
    variations: [light, dark, mobile]
  
  - template: dashboard-admin
    variations: [light, dark]
  
  - template: dashboard-analytics
    variations: [light]
```

Then run:

```bash
python scripts/ag-ui-generator.py batch-generate \
  --config configs/batch-all-dashboards.yaml
```

---

## Template System

### Creating a New Template

1. Create YAML file in `templates/`:

```yaml
template:
  name: "Feature Name"
  category: "feature"
  ai_tool: "dalle-3"
  
  base_prompt: |
    Create a modern {feature_type} UI mockup.
    [Detailed prompt description]
  
  parameters:
    feature_type: "care gap detection"
    primary_color: "#1E3A5F"
    accent_color: "#00A9A5"
    width: 1920
    height: 1080
  
  variations:
    - name: "desktop"
      parameters:
        width: 1920
        height: 1080
    
    - name: "mobile"
      parameters:
        width: 375
        height: 812
```

2. Use the template:

```bash
python scripts/ag-ui-generator.py generate \
  --template feature-name \
  --variation desktop
```

---

## AI Tools Supported

### OpenAI DALL-E 3

**Best for:** Realistic UI mockups, detailed screenshots

**Setup:**
```bash
export OPENAI_API_KEY="your-api-key"
```

**Usage:**
```bash
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --ai-tool dalle-3
```

### Google Gemini API

**Best for:** High-quality UI mockups, prompt enhancement, multimodal capabilities

**Setup:**
```bash
export GEMINI_API_KEY="your-api-key"
# OR
export GOOGLE_API_KEY="your-api-key"
```

**Usage:**
```bash
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --ai-tool gemini
```

**Note:** Gemini API supports image generation through certain models. The generator will use the best available method for image generation.

### Stability AI Stable Diffusion

**Best for:** Batch generation, custom models

**Setup:**
```bash
export STABILITY_API_KEY="your-api-key"
```

**Usage:**
```bash
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --ai-tool stable-diffusion
```

---

## Asset Management

### Generated Assets

All generated assets are saved to `assets/` directory:
- Organized by category (dashboards, features, marketing)
- Named with template and variation
- Includes metadata JSON files

### Metadata

Each generated asset includes metadata:
- Asset ID
- Template and variation
- Generation parameters
- File path and size
- Generation timestamp

### Version Control

Assets are tracked with:
- Unique asset IDs
- Generation timestamps
- Parameter snapshots
- Quality metrics

---

## Best Practices

### Prompt Engineering

1. **Be Specific**
   - Include exact dimensions
   - Specify color codes
   - Describe layout in detail
   - Mention brand elements

2. **Use Negative Prompts**
   - Exclude unwanted elements
   - Avoid generic stock photo look
   - Prevent text artifacts

3. **Iterate and Refine**
   - Start with base prompt
   - Refine based on results
   - Save successful prompts
   - Build prompt library

### Asset Management

1. **Organization**
   - Use consistent naming
   - Organize by category
   - Tag for easy search

2. **Optimization**
   - Optimize file sizes
   - Use appropriate formats
   - Create multiple sizes

3. **Version Control**
   - Track versions
   - Keep original prompts
   - Document changes

---

## Integration

### With Playwright

```python
# Combine AI-generated mockups with Playwright screenshots
from ag_ui_generator import AGUIGenerator
from playwright.sync_api import sync_playwright

# Generate AI mockup
generator = AGUIGenerator()
metadata = generator.generate_ui("dashboard-clinical", "light")

# Capture real UI with Playwright
with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    page.goto("http://localhost:4200/dashboard")
    page.screenshot(path="real-ui.png")
    browser.close()

# Combine both (using PIL or similar)
# ...
```

### With Documentation

```python
# Generate UI assets for documentation
generator = AGUIGenerator()

# Generate all dashboard variations
for variation in ["light", "dark", "mobile"]:
    metadata = generator.generate_ui(
        "dashboard-clinical",
        variation=variation,
        output_path=f"docs/images/dashboard-{variation}.png"
    )
```

---

## Troubleshooting

### Common Issues

**1. API Key Not Found**
```bash
# Set environment variable
export OPENAI_API_KEY="your-api-key"
```

**2. Template Not Found**
```bash
# Check template exists
ls templates/dashboard-clinical.yaml

# Use correct template name
python scripts/ag-ui-generator.py generate --template dashboard-clinical
```

**3. Generation Fails**
```bash
# Check API quota
# Verify prompt is valid
# Check network connection
```

---

## Next Steps

1. **Create More Templates**
   - Feature screenshots
   - Marketing visuals
   - Architecture diagrams

2. **Build Component Library**
   - Reusable UI components
   - Design system integration
   - Brand consistency

3. **Automate Workflows**
   - CI/CD integration
   - Scheduled generation
   - Quality validation

---

**AG-UI: AI-Generated UI Content System**

*Systematic generation of high-quality UI content using AI tools.*

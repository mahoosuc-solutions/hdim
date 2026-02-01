# Gemini API Setup Guide

**Complete guide for using Google Gemini API with AG-UI Generator**

---

## Overview

Google Gemini API provides powerful image generation capabilities for creating UI mockups. The AG-UI generator supports Gemini API as an alternative to DALL-E 3 and Stable Diffusion.

---

## Prerequisites

1. **Google Cloud Account** or **Google AI Studio Account**
2. **Gemini API Key** (free tier available)
3. **Python 3.8+** installed

---

## Getting Your API Key

### Option 1: Google AI Studio (Recommended - Free)

1. **Visit Google AI Studio:**
   - Go to https://makersuite.google.com/app/apikey
   - Or https://aistudio.google.com/app/apikey

2. **Sign In:**
   - Use your Google account
   - Accept terms if prompted

3. **Create API Key:**
   - Click "Create API Key"
   - Choose "Create API key in new project" or existing project
   - Copy your API key

4. **Set Environment Variable:**
   ```bash
   export GEMINI_API_KEY="your-api-key-here"
   # OR
   export GOOGLE_API_KEY="your-api-key-here"
   ```

### Option 2: Google Cloud Console

1. **Create Google Cloud Project:**
   - Go to https://console.cloud.google.com
   - Create new project or select existing

2. **Enable Gemini API:**
   - Navigate to "APIs & Services" > "Library"
   - Search for "Generative Language API"
   - Click "Enable"

3. **Create API Key:**
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "API Key"
   - Copy your API key

4. **Set Environment Variable:**
   ```bash
   export GEMINI_API_KEY="your-api-key-here"
   ```

---

## Installation

### Install Required Package

```bash
# Install google-generativeai package
pip install google-generativeai>=0.3.0

# Or install all AG-UI requirements
cd docs/marketing/ag-ui
pip install -r requirements.txt
```

---

## Usage

### Basic Usage

```bash
# Set API key
export GEMINI_API_KEY="your-api-key"

# Generate UI with Gemini
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --ai-tool gemini
```

### With Custom Model

```bash
# Use specific Gemini model
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --ai-tool gemini \
  --parameters '{"gemini_model": "gemini-2.0-flash-exp"}'
```

### Batch Generation

```bash
# Generate multiple UIs with Gemini
python scripts/ag-ui-generator.py batch-generate \
  --config configs/batch-dashboards.yaml \
  --ai-tool gemini
```

---

## Available Gemini Models

### Image Generation Models

**gemini-2.0-flash-exp** (Recommended)
- Fast generation
- Good quality
- Supports image generation

**gemini-1.5-pro**
- High quality
- Better understanding
- May have image generation support

**gemini-1.5-flash**
- Fast generation
- Good for iterations
- May have image generation support

### Model Selection

The generator will automatically use the best available model for image generation. You can specify a model in the template parameters:

```yaml
parameters:
  gemini_model: "gemini-2.0-flash-exp"
```

---

## Configuration

### Environment Variables

```bash
# Primary (recommended)
export GEMINI_API_KEY="your-api-key"

# Alternative (also supported)
export GOOGLE_API_KEY="your-api-key"
```

### Template Configuration

Add Gemini-specific parameters to your templates:

```yaml
template:
  name: "Dashboard"
  ai_tool: "gemini"
  
  parameters:
    gemini_model: "gemini-2.0-flash-exp"
    width: 1920
    height: 1080
```

---

## Features

### Advantages of Gemini

1. **Free Tier Available**
   - Generous free quota
   - Good for testing and development

2. **Multimodal Capabilities**
   - Can understand complex prompts
   - Better context understanding

3. **Fast Generation**
   - Quick response times
   - Good for iterations

4. **Integration**
   - Works well with other Google services
   - Easy API integration

### Limitations

1. **Model Availability**
   - Image generation support varies by model
   - Some models may not support direct image generation

2. **Resolution**
   - May have resolution limitations
   - Check model capabilities

3. **Rate Limits**
   - Free tier has rate limits
   - Paid tier offers higher limits

---

## Troubleshooting

### API Key Not Found

```bash
# Check if API key is set
echo $GEMINI_API_KEY

# Set API key
export GEMINI_API_KEY="your-api-key"
```

### Package Not Installed

```bash
# Install google-generativeai
pip install google-generativeai

# Or install all requirements
pip install -r requirements.txt
```

### Image Generation Not Supported

If the model doesn't support direct image generation, the generator will:
1. Try alternative methods
2. Use REST API approach
3. Provide helpful error messages

### Rate Limit Errors

```bash
# Check your quota
# Visit: https://aistudio.google.com/app/apikey

# Use paid tier for higher limits
# Or wait for rate limit reset
```

---

## Best Practices

### 1. Prompt Engineering

**Be Specific:**
- Include exact dimensions
- Specify color codes
- Describe layout in detail

**Use Gemini's Strengths:**
- Leverage multimodal understanding
- Include context in prompts
- Use descriptive language

### 2. Model Selection

**For Speed:**
- Use `gemini-2.0-flash-exp`
- Fast generation
- Good for iterations

**For Quality:**
- Use `gemini-1.5-pro`
- Higher quality
- Better understanding

### 3. Cost Optimization

**Free Tier:**
- Use for testing
- Limited requests
- Good for development

**Paid Tier:**
- Higher limits
- Better performance
- Production use

---

## Example Workflows

### Workflow 1: Quick Test

```bash
# 1. Set API key
export GEMINI_API_KEY="your-api-key"

# 2. Generate test UI
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --ai-tool gemini

# 3. Check output
ls -lh assets/dashboards/
```

### Workflow 2: Batch Generation

```bash
# 1. Set API key
export GEMINI_API_KEY="your-api-key"

# 2. Create batch config
cat > configs/batch-test.yaml << EOF
items:
  - template: dashboard-clinical
    variations: [light, dark]
EOF

# 3. Generate all
python scripts/ag-ui-generator.py batch-generate \
  --config configs/batch-test.yaml \
  --ai-tool gemini
```

### Workflow 3: Custom Parameters

```bash
# Generate with custom metrics
python scripts/ag-ui-generator.py generate \
  --template dashboard-clinical \
  --variation light \
  --ai-tool gemini \
  --parameters '{
    "total_patients": "50,000",
    "hedis_score": "85.2",
    "gemini_model": "gemini-2.0-flash-exp"
  }'
```

---

## API Limits & Pricing

### Free Tier

- **Requests:** 15 requests per minute
- **Daily quota:** Varies by model
- **Best for:** Testing, development

### Paid Tier

- **Higher limits:** Based on usage
- **Better performance:** Faster responses
- **Best for:** Production, batch generation

### Check Your Quota

Visit: https://aistudio.google.com/app/apikey

---

## Resources

### Official Documentation

- **Gemini API Docs:** https://ai.google.dev/docs
- **Google AI Studio:** https://aistudio.google.com
- **API Reference:** https://ai.google.dev/api

### AG-UI Resources

- **Framework Docs:** `AG_UI_FRAMEWORK.md`
- **Usage Guide:** `README.md`
- **Quick Start:** `QUICK_START.md`

---

## Next Steps

1. **Get API Key**
   - Sign up at Google AI Studio
   - Create API key

2. **Test Generation**
   - Generate first UI
   - Verify output quality

3. **Customize Templates**
   - Modify for your needs
   - Add Gemini-specific parameters

4. **Scale Up**
   - Batch generation
   - Integrate with workflows

---

**Gemini API Setup Complete**

*Ready to generate UI content with Google Gemini API!*

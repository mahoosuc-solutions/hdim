# Gemini 3 PRO Image Generation Script

**Generate detailed UI images using Gemini 3 PRO with the found API key**

---

## Quick Start

```bash
# Navigate to scripts directory
cd docs/marketing/ag-ui/scripts

# Run the script
python generate-with-gemini-pro.py
```

---

## What It Does

The script uses the **Gemini API key** (found in the codebase) to generate detailed UI mockups using Gemini's latest image generation model.

### Features

- **Uses Gemini 3 PRO** (or latest image generation model)
- **Detailed prompts** - Enhanced prompts for high-quality results
- **Multiple dashboards** - Generates clinical dashboard (light/dark) and admin dashboard
- **High resolution** - 1920x1080 pixel images
- **Automatic saving** - Saves to `assets/generated/` directory

---

## Generated Images

The script generates:

1. **clinical-dashboard-light.png** - Light mode clinical dashboard
2. **clinical-dashboard-dark.png** - Dark mode clinical dashboard
3. **admin-dashboard.png** - System administration dashboard

All images are saved to: `docs/marketing/ag-ui/assets/generated/`

---

## API Key

The script uses the API key found in the codebase:
- **Environment variable:** `GEMINI_API_KEY` or `GOOGLE_API_KEY`
- **Fallback:** Uses the key found in `generate-marketing-images.sh`

---

## Customization

### Generate Single Image

Modify the script to generate a single image:

```python
# In main() function, replace generate_dashboard_images() with:
output_path = generate_image_with_gemini_pro(
    prompt="Your detailed prompt here",
    width=1920,
    height=1080,
    output_filename="my-image.png"
)
```

### Add More Images

Add to the `dashboards` list in `generate_dashboard_images()`:

```python
{
    "name": "my-custom-dashboard",
    "prompt": "Your detailed prompt...",
    "width": 1920,
    "height": 1080
}
```

---

## Model Information

**Current Model:** `gemini-2.0-flash-exp-image-generation`

This is Gemini's latest image generation model. The script can be updated to use:
- `gemini-1.5-pro` - For multimodal text-to-image
- `gemini-2.0-flash-exp` - Fast generation
- Other Gemini models as they become available

---

## Output

### Success Output

```
🎨 Generating image with Gemini PRO...
📝 Prompt: Create a modern healthcare analytics dashboard...
📐 Size: 1920x1080
🔄 Calling Gemini API...
✅ Image generated successfully!
📁 Saved to: assets/generated/clinical-dashboard-light.png
📊 File size: 245.67 KB
```

### Error Handling

The script handles:
- API errors
- Network timeouts
- Missing image data
- File save errors

---

## Rate Limiting

The script includes a 2-second delay between requests to respect API rate limits.

---

## Next Steps

1. **Run the script** to generate initial images
2. **Review generated images** in `assets/generated/`
3. **Customize prompts** for your specific needs
4. **Add more image types** as needed

---

**Gemini 3 PRO Image Generation**

*Generate detailed UI images with the found API key!*

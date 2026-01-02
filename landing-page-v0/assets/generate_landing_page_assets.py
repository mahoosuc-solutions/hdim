#!/usr/bin/env python3
"""
HDIM Landing Page Visual Asset Generator
Generates all visual assets for the HDIM landing page using Gemini 2.5 Flash Image API.

Requirements:
    pip install google-generativeai pillow requests

Usage:
    # Generate all assets
    python generate_landing_page_assets.py --all

    # Generate specific category
    python generate_landing_page_assets.py --category hero
    python generate_landing_page_assets.py --category portraits
    python generate_landing_page_assets.py --category technical
    python generate_landing_page_assets.py --category dashboard

    # Generate single asset
    python generate_landing_page_assets.py --asset HERO-01

    # List all available assets
    python generate_landing_page_assets.py --list

API Key:
    Set environment variable: export GOOGLE_API_KEY="your-key"
    Required - script will not run without a valid API key.
"""

import os
import sys
import json
import argparse
import base64
import time
import subprocess
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional, List, Any

# =============================================================================
# CONFIGURATION
# =============================================================================

# API Configuration - MUST be set via environment variable
GOOGLE_API_KEY = os.environ.get("GOOGLE_API_KEY")
MODEL_NAME = "gemini-2.0-flash-exp-image-generation"

# Brand Guidelines
BRAND = {
    "primary_color": "#0066CC",      # Deep Blue
    "accent_color": "#00A5B5",       # Warm Teal
    "background": "#FFFFFF",         # White
    "text_dark": "#1A1A1A",
    "text_light": "#F5F5F5",
    "company": "HDIM",
    "full_name": "HealthData-in-Motion",
    "tagline": "Healthcare Software Built By People Who Care",
    "mission": "We build healthcare software as if patients' lives depend on it - because they do."
}

# Output Configuration
OUTPUT_DIR = Path("./generated")
TIMESTAMP = datetime.now().strftime("%Y%m%d_%H%M%S")

# Rate limiting
REQUEST_DELAY_SECONDS = 7  # Respect API rate limits (10 requests/minute)

# =============================================================================
# VISUAL ASSET PROMPTS - From VISUAL_ASSET_PROMPTS.md
# =============================================================================

PROMPTS = {
    # -------------------------------------------------------------------------
    # HERO IMAGES
    # -------------------------------------------------------------------------
    "HERO-01": {
        "name": "Main Hero - Desktop",
        "category": "hero",
        "dimensions": "1920x1080",
        "prompt": f"""Create a warm, hopeful healthcare technology hero image for a website.

Scene: A diverse group of three patients - a 67-year-old Latina grandmother, a 42-year-old
professional man, and a 55-year-old professional woman - all looking healthy, happy, and
connected. They should appear in a modern, light-filled healthcare or home environment.

Visual elements: Subtle, elegant real-time data visualization elements flowing between the
people - representing how FHIR-connected systems and CQL insights protect them continuously.
Show gentle pulse-like animations suggesting "always watching, always protecting."
Small iconography hints: a subtle heartbeat line, a gentle alert glow, data streams.

The technology should feel invisible yet present - like a guardian watching over them.

Mood: Optimistic, human-centered, genuine - NOT stock photography feeling. The feeling
of "someone is looking out for me" without being intrusive.

Lighting: Soft, natural, warm.
Color palette: Deep blue ({BRAND['primary_color']}) accents, warm teal ({BRAND['accent_color']}) highlights, predominantly
light and airy backgrounds.

Style: Photorealistic, authentic expressions, modern healthcare technology aesthetic.
Dimensions: 1920x1080 pixels, landscape orientation."""
    },

    "HERO-02": {
        "name": "Hero - Mobile",
        "category": "hero",
        "dimensions": "750x1334",
        "prompt": f"""Create a warm, hopeful healthcare technology hero image optimized for mobile devices.

Same scene as desktop hero, but composed for portrait/mobile orientation. Focus on one
or two of the patient faces with the digital connection elements visible above/around them.

Feature: A 67-year-old Latina grandmother OR a 42-year-old professional man, looking
healthy, happy, and at peace. Subtle digital data visualization elements flow around them.

Mood: Optimistic, human-centered, genuine - NOT stock photography feeling.
Lighting: Soft, natural, warm.
Color palette: Deep blue ({BRAND['primary_color']}) accents, warm teal ({BRAND['accent_color']}) highlights.

Style: Photorealistic, authentic expressions.
Dimensions: 750x1334 pixels, portrait orientation."""
    },

    "HERO-03": {
        "name": "Social Share Image",
        "category": "hero",
        "dimensions": "1200x630",
        "prompt": f"""Create a social media share image for HDIM healthcare technology platform.

Simplified version of hero image optimized for social media sharing. One or two patient
faces - a healthy, happy grandmother and/or professional - with subtle digital connection
elements. Space on left third for headline text overlay.

Mood: Optimistic, trustworthy, innovative.
Color palette: Deep blue ({BRAND['primary_color']}), warm teal ({BRAND['accent_color']}), light backgrounds.

Style: Photorealistic, clean composition for text overlay.
Dimensions: 1200x630 pixels, landscape orientation."""
    },

    # -------------------------------------------------------------------------
    # PATIENT STORY PORTRAITS
    # -------------------------------------------------------------------------
    "PORTRAIT-MARIA": {
        "name": "Maria - Diabetes Patient",
        "category": "portraits",
        "dimensions": "800x1000",
        "prompt": f"""Create a warm, genuine portrait photograph of a 67-year-old Latina grandmother named Maria.

Subject: Maria looks healthy, vibrant, and grateful. She has kind eyes, silver-streaked
dark hair, and a gentle, authentic smile. She wears comfortable everyday clothing -
perhaps a cardigan over a blouse.

Setting: Soft focus background suggesting a warm home kitchen with family photos visible,
or a garden setting with flowers.

Mood: Hope, gratitude, family love, resilience.
Lighting: Warm, natural window light, soft shadows.
Style: Photorealistic, authentic - must NOT look like stock photography.
Composition: 3/4 portrait, slight angle, eyes engaging viewer.

This is Maria, who has diabetes and whose care gap was identified early because of
healthcare technology. She represents the human impact of quality software.

Dimensions: 800x1000 pixels, portrait orientation."""
    },

    "PORTRAIT-JAMES": {
        "name": "James - Depression Survivor",
        "category": "portraits",
        "dimensions": "800x1000",
        "prompt": f"""Create a hopeful portrait photograph of a 42-year-old man named James who has overcome depression.

Subject: James appears relieved, calm, and present. He's in business casual attire
(button-down shirt, maybe sleeves rolled up). His smile is genuine and suggests someone
who has come through difficulty. He looks healthy and engaged with life.

Setting: Outdoor environment - a park, trail, or outdoor cafe. Natural greenery visible.
Suggests an active, healthy lifestyle reclaimed.

Mood: Recovery, hope, second chances, vitality.
Lighting: Natural daylight, early morning or golden hour feel.
Style: Photorealistic, candid feel - like a genuine moment captured.
Composition: Environmental portrait, subject slightly off-center.

This is James, whose depression was caught early through proactive healthcare screening.
He represents recovery and the power of early intervention.

Dimensions: 800x1000 pixels, portrait orientation."""
    },

    "PORTRAIT-SARAH": {
        "name": "Sarah - Cancer Survivor",
        "category": "portraits",
        "dimensions": "800x1000",
        "prompt": f"""Create a confident portrait photograph of a 55-year-old professional woman named Sarah, a cancer survivor.

Subject: Sarah is a strong, successful woman who beat breast cancer early. She wears
professional attire but appears relaxed and healthy. Her expression conveys confidence,
gratitude, and forward momentum. She clearly knows she dodged a bullet and is living
fully.

Setting: Modern office with large windows, or outdoor corporate campus with contemporary
architecture. Setting suggests success and professional achievement.

Mood: Strength, gratitude, resilience, confidence.
Lighting: Clean, modern, natural light from windows.
Style: Photorealistic, editorial quality - like a business magazine portrait.
Composition: Power pose, direct eye contact, commanding presence.

This is Sarah, whose cancer was caught at Stage 1 because of a care gap alert. She
represents triumph and the life-saving impact of quality healthcare technology.

Dimensions: 800x1000 pixels, portrait orientation."""
    },

    # -------------------------------------------------------------------------
    # THE 5-MINUTE STORY VISUAL
    # -------------------------------------------------------------------------
    "STORY-SPLIT": {
        "name": "5-Minute Story Split Screen",
        "category": "story",
        "dimensions": "1920x1080",
        "prompt": f"""Create a powerful split-screen image representing "code that saves lives."

LEFT SIDE (50%):
A computer terminal/code editor with a dark background (like VS Code dark theme).
The commit message is clearly visible:
"fix(hipaa): Reduce PHI cache TTL to <=5min for HIPAA compliance"
Date visible: December 27, 2025, 10:31 PM
The code screen should look authentic, with syntax highlighting.

RIGHT SIDE (50%):
A warm, touching family moment. A grandmother (could be Maria) with grandchildren,
or a father (could be James) playing with his kids. The image should convey
"this is what we're protecting."

TRANSITION:
The split between the two sides should feel connected - perhaps flowing lines of
light/data that transition from the code side to the family side, suggesting the
code protects the people.

Overall mood: The juxtaposition of cold technology and warm humanity, connected by purpose.
Color palette: Left side dark with syntax colors, right side warm with natural lighting.

Dimensions: 1920x1080 pixels, landscape orientation."""
    },

    "STORY-SQUARE": {
        "name": "5-Minute Story Square",
        "category": "story",
        "dimensions": "1080x1080",
        "prompt": f"""Create a square format version of the "code that saves lives" concept.

Same concept as split screen, but in square format for Instagram/social.

TOP THIRD:
A computer terminal showing the commit message:
"fix(hipaa): Reduce PHI cache TTL to <=5min for HIPAA compliance"
Dark code editor background, authentic syntax highlighting.

BOTTOM TWO-THIRDS:
A warm family image - grandmother with grandchildren, or father with kids.
Natural lighting, genuine happiness, representing "what we're protecting."

Visual connection between them - flowing data/light lines suggesting the
code protects the people.

Dimensions: 1080x1080 pixels, square format."""
    },

    # -------------------------------------------------------------------------
    # TECHNICAL/ARCHITECTURE VISUALS
    # -------------------------------------------------------------------------
    "TECH-ARCHITECTURE": {
        "name": "Integration Platform Architecture",
        "category": "technical",
        "dimensions": "1200x800",
        "prompt": f"""Create an elegant, modern technical diagram showing a healthcare integration platform.

Layout: LEFT TO RIGHT data flow showing the "Connect Anything → Understand Everything → Act Immediately" story.

LEFT SECTION - "Connect Anything" (Data Sources):
- Multiple system icons representing: Epic, Cerner, Labs, Claims, Devices, Payers
- Each with subtle connection lines flowing rightward
- Slightly muted colors suggesting disparate, siloed systems

CENTER SECTION - "The Platform" (three layers):
1. n8n Integration Layer - workflow/automation icon, showing data normalization
2. FHIR R4 Data Layer - database icon with medical cross, the universal translator
3. CQL Engine - code brackets with heartbeat pulse, real-time event processing
- This section glows brighter, suggesting transformation happening
- Animated-feeling pulse lines showing real-time processing

RIGHT SECTION - "Act Immediately" (Outputs):
- Care Gap Alerts (bell icon)
- Quality Dashboards (chart icon)
- Clinical Insights (lightbulb icon)
- Patient Outreach (person with checkmark)
- Bright, action-oriented colors (teal {BRAND['accent_color']})

Connection lines: Flowing data streams, animated feeling, showing continuous real-time flow.
Use gradient colors transitioning from muted (sources) to bright (outputs).

Color palette: Deep blue ({BRAND['primary_color']}) for platform core, teal ({BRAND['accent_color']}) for outputs,
grays for source systems, white background.

Style: Clean, professional, modern SaaS. Should communicate "we connect everything and
make it actionable in real-time."

Dimensions: 1200x800 pixels, landscape orientation."""
    },

    "TECH-DATAFLOW": {
        "name": "Real-Time Data Flow",
        "category": "technical",
        "dimensions": "1920x600",
        "prompt": f"""Create a cinematic, animated-feeling visualization of healthcare data flowing in real-time.

Layout: Wide panoramic view showing data journey from left to right.

LEFT - Data Sources (appearing as gentle pulses):
- EHR systems emitting patient data
- Lab results flowing in
- Claims data streaming
- Device readings pulsing
Each source has its own color but they all flow toward center.

CENTER - The Transformation Engine:
- FHIR standardization layer (data streams becoming uniform)
- CQL Engine (processing, with subtle code/logic visualization)
- Show data being enriched, normalized, analyzed
- Glowing core suggesting intelligence/processing
- Visual of clinical rules being applied

RIGHT - Actionable Outputs (bright, resolved):
- Care gap alerts firing off
- Dashboard updates propagating
- Notifications reaching care teams
- Patients receiving outreach

Motion feeling: Like watching a river of data flow, transform, and become actionable insights.
The visualization should feel ALIVE and CONTINUOUS.

Color palette: Source data in varied muted colors, transformation in deep blue ({BRAND['primary_color']}),
outputs in bright teal ({BRAND['accent_color']}).

Style: Abstract data visualization, premium tech aesthetic, like a Stripe or Plaid marketing visual.

Dimensions: 1920x600 pixels, wide panoramic."""
    },

    "TECH-SCALE": {
        "name": "Enterprise Scale Visualization",
        "category": "technical",
        "dimensions": "1200x800",
        "prompt": f"""Create a powerful visualization showing HDIM operating at enterprise healthcare scale.

Central concept: A glowing central hub (HDIM platform) with radiating connections to:

Inner ring (large, prominent):
- 47 Connected Health Systems (hospital icons)
- Show major EHR logos subtly: Epic, Cerner, Meditech, Athena

Middle ring:
- 250+ Data Sources (labs, pharmacies, devices, payers)
- Each as a small node with connection line to hub

Outer ring (representing reach):
- 500,000+ Patients Monitored
- Represented as a gentle glow/aura

Statistics overlaid elegantly:
- "47 Health Systems"
- "250+ Integrations"
- "500K+ Patients"
- "Real-time Processing"

All connections should show subtle data flow animations toward the center and back out.
The hub pulses gently, suggesting always-on processing.

Color palette: Deep blue ({BRAND['primary_color']}) hub, teal ({BRAND['accent_color']}) connections,
white/light background.

Style: Infographic meets technical diagram. Should feel massive yet organized.
Communicate: "We handle enterprise scale without breaking a sweat."

Dimensions: 1200x800 pixels, landscape orientation."""
    },

    "TECH-N8N": {
        "name": "Integration Workflow",
        "category": "technical",
        "dimensions": "1200x800",
        "prompt": f"""Create a clean, modern visualization of an n8n-style integration workflow for healthcare.

Show a specific workflow example: "New Lab Result → Care Gap Check → Alert"

Layout: Left to right workflow with connected nodes.

NODE 1 - Trigger (left):
- Lab System icon
- Label: "Lab Result Received"
- Incoming data visualization

NODE 2 - Transform:
- FHIR icon
- Label: "Normalize to FHIR R4"
- Data transformation visual

NODE 3 - Process:
- CQL Engine icon with code brackets
- Label: "Evaluate Quality Measures"
- Clinical logic processing

NODE 4 - Decision (diamond shape):
- Label: "Care Gap Detected?"
- Two paths: Yes (bright) / No (muted)

NODE 5 - Action (right):
- Alert/notification icon
- Label: "Create Care Gap Alert"
- Output flowing to dashboard

Connections: Smooth curved lines with animated data dots flowing through.
Each node has a subtle glow suggesting activity.

Style: Clean, modern workflow visualization like n8n or Zapier.
Professional enough for enterprise, clear enough to understand instantly.

Color palette: Deep blue ({BRAND['primary_color']}) nodes, teal ({BRAND['accent_color']}) for active/success paths,
white background.

Dimensions: 1200x800 pixels, landscape orientation."""
    },

    # -------------------------------------------------------------------------
    # TRUST BADGE ICONS
    # -------------------------------------------------------------------------
    "ICON-HIPAA": {
        "name": "HIPAA Compliance Icon",
        "category": "icons",
        "dimensions": "128x128",
        "prompt": f"""Create a modern icon for HIPAA Compliance.

Design: A shield shape with a heart in the center. The heart represents patient care
protected by security (the shield).

Style guidelines:
- Minimal line art, consistent 2px stroke weight
- Modern, clean, professional aesthetic
- Primary color: Deep blue ({BRAND['primary_color']})
- Accent/highlight: Teal ({BRAND['accent_color']})
- Transparent background

Dimensions: 128x128 pixels with transparent background."""
    },

    "ICON-TESTS": {
        "name": "Test Coverage Icon",
        "category": "icons",
        "dimensions": "128x128",
        "prompt": f"""Create a modern icon for Test Coverage/Quality.

Design: A checkmark inside a circle, with a small sparkle/star accent suggesting
excellence and completeness.

Style guidelines:
- Minimal line art, consistent 2px stroke weight
- Modern, clean, professional aesthetic
- Primary color: Deep blue ({BRAND['primary_color']})
- Accent/highlight: Teal ({BRAND['accent_color']})
- Transparent background

Dimensions: 128x128 pixels with transparent background."""
    },

    "ICON-FHIR": {
        "name": "FHIR Integration Icon",
        "category": "icons",
        "dimensions": "128x128",
        "prompt": f"""Create a modern icon for FHIR Integration/Interoperability.

Design: Two puzzle pieces connecting seamlessly, suggesting interoperability and
perfect fit between systems.

Style guidelines:
- Minimal line art, consistent 2px stroke weight
- Modern, clean, professional aesthetic
- Primary color: Deep blue ({BRAND['primary_color']})
- Accent/highlight: Teal ({BRAND['accent_color']})
- Transparent background

Dimensions: 128x128 pixels with transparent background."""
    },

    "ICON-MICROSERVICES": {
        "name": "Microservices Icon",
        "category": "icons",
        "dimensions": "128x128",
        "prompt": f"""Create a modern icon for Microservices Architecture.

Design: A hexagonal network of 6-7 small nodes connected in a stable pattern,
suggesting modular, interconnected components.

Style guidelines:
- Minimal line art, consistent 2px stroke weight
- Modern, clean, professional aesthetic
- Primary color: Deep blue ({BRAND['primary_color']})
- Accent/highlight: Teal ({BRAND['accent_color']})
- Transparent background

Dimensions: 128x128 pixels with transparent background."""
    },

    "ICON-CQL": {
        "name": "CQL Engine Icon",
        "category": "icons",
        "dimensions": "128x128",
        "prompt": f"""Create a modern icon for CQL Engine (Clinical Quality Language).

Design: A medical cross combined with code brackets < >, suggesting the intersection
of clinical knowledge and technology.

Style guidelines:
- Minimal line art, consistent 2px stroke weight
- Modern, clean, professional aesthetic
- Primary color: Deep blue ({BRAND['primary_color']})
- Accent/highlight: Teal ({BRAND['accent_color']})
- Transparent background

Dimensions: 128x128 pixels with transparent background."""
    },

    "ICON-UPTIME": {
        "name": "Uptime SLA Icon",
        "category": "icons",
        "dimensions": "128x128",
        "prompt": f"""Create a modern icon for Uptime/Reliability (99.9% SLA).

Design: A clock face or circular gauge showing excellent performance, suggesting
reliability and always-on availability.

Style guidelines:
- Minimal line art, consistent 2px stroke weight
- Modern, clean, professional aesthetic
- Primary color: Deep blue ({BRAND['primary_color']})
- Accent/highlight: Teal ({BRAND['accent_color']})
- Transparent background

Dimensions: 128x128 pixels with transparent background."""
    },

    # -------------------------------------------------------------------------
    # DASHBOARD/PRODUCT SCREENSHOTS
    # -------------------------------------------------------------------------
    "DASHBOARD-MAIN": {
        "name": "Real-Time Care Gap Dashboard",
        "category": "dashboard",
        "dimensions": "1440x900",
        "prompt": f"""Create a modern, clean healthcare analytics dashboard UI mockup showing REAL-TIME insights at SCALE.

Layout:
- Top bar with HDIM logo, navigation, user menu
- Subtle "LIVE" indicator with green pulse dot (showing real-time updates)
- Left sidebar with navigation: Dashboard, Patients, Measures, Integrations, Settings

Main content area showing:
- Header: "Care Gap Command Center" with last updated timestamp (seconds ago)
- Summary stats row with LIVE indicators:
  - Connected Systems: 47 (showing Epic, Cerner, Lab icons)
  - Patients Monitored: 128,450
  - Open Gaps: 2,340 (with trend arrow)
  - Closed Today: +127 (green, real-time counter feeling)
  - HEDIS Score: 78.4% → 79.1% (showing improvement)

- Real-time activity feed (right side): Recent alerts flowing in
  "Maria S. - A1C care gap identified - 2 min ago"
  "James T. - Depression screening due - 5 min ago"

- Bar chart showing gap closure trend over 10 weeks (ascending trend)
- Priority patient list with risk scores, showing "Call Now" action buttons
- Small integration status panel: "All 47 systems connected ✓"

Style:
- Clean, modern SaaS aesthetic
- White background with subtle gray (#F5F5F5) card backgrounds
- Primary blue ({BRAND['primary_color']}) for charts and highlights
- Teal ({BRAND['accent_color']}) for positive trends and live indicators
- Green pulse effects for real-time elements
- Clear typography, generous whitespace

The dashboard should communicate: "This is LIVE. This is SCALE. This is ACTIONABLE."

Dimensions: 1440x900 pixels, landscape orientation."""
    },

    "DASHBOARD-MOBILE": {
        "name": "Mobile Dashboard",
        "category": "dashboard",
        "dimensions": "375x812",
        "prompt": f"""Create a mobile-responsive version of a healthcare analytics dashboard.

Layout:
- Collapsed navigation (hamburger menu)
- Key stats prominently displayed
- Simplified chart view
- Swipe-able patient cards

Style:
- Clean, modern SaaS aesthetic optimized for mobile
- White background with subtle gray card backgrounds
- Primary blue ({BRAND['primary_color']}) for highlights
- Touch-friendly spacing

Dimensions: 375x812 pixels (iPhone viewport), portrait orientation."""
    },

    # -------------------------------------------------------------------------
    # COMPARISON/BEFORE-AFTER
    # -------------------------------------------------------------------------
    "COMPARISON": {
        "name": "Fragmented vs Connected Healthcare",
        "category": "comparison",
        "dimensions": "1200x600",
        "prompt": f"""Create a powerful side-by-side comparison visual showing healthcare data transformation.

LEFT SIDE - "The Fragmentation Problem":
- Darker, grayer, chaotic color palette
- Multiple disconnected system logos (Epic, Cerner, Labs) with broken/no connection lines
- Frustrated healthcare worker surrounded by multiple screens, each showing different systems
- Red "X" marks showing failed data transfers
- Clock showing "3 MONTHS LATER" - reactive, batch-based
- Patient chart marked "OUTDATED" or "INCOMPLETE"
- Visual metaphor: isolated islands with no bridges
Mood: Fragmented, siloed, reactive, dangerous gaps

RIGHT SIDE - "Connected with HDIM":
- Bright, unified, flowing color palette (whites, blues, teals)
- All system logos connected through a central glowing hub (FHIR + CQL)
- Confident healthcare worker with ONE unified dashboard
- Green checkmarks showing live data flowing
- Timestamp showing "2 SECONDS AGO" - real-time
- Patient chart with "COMPLETE VIEW" and care gap alert visible
- Visual metaphor: everything connected, data flowing like a river
Mood: Unified, real-time, proactive, comprehensive

CENTER DIVIDER:
A transformative arrow or light bridge suggesting the journey from fragmented to connected.

Key message: "From months of manual work to real-time insights. From data silos to unified patient view."

Dimensions: 1200x600 pixels, landscape orientation."""
    },

    # -------------------------------------------------------------------------
    # SOCIAL MEDIA TEMPLATES
    # -------------------------------------------------------------------------
    "SOCIAL-LINKEDIN": {
        "name": "LinkedIn Post Template",
        "category": "social",
        "dimensions": "1200x627",
        "prompt": f"""Create a template for LinkedIn posts with HDIM branding.

Design:
- HDIM brand colors (blue {BRAND['primary_color']}/teal {BRAND['accent_color']})
- Space for headline text (left 60%)
- Circular image placeholder (right 40%)
- Subtle geometric patterns in background
- HDIM logo watermark bottom right
- Clean, professional, modern feel

Dimensions: 1200x627 pixels, landscape orientation."""
    },

    "SOCIAL-QUOTE": {
        "name": "Quote Card Template",
        "category": "social",
        "dimensions": "1080x1080",
        "prompt": f"""Create a quote card template for Instagram/social media.

Design:
- Large quotation mark graphic in top left
- Space for quote text (center)
- Attribution line (bottom)
- HDIM branding (bottom)
- Gradient background: deep blue ({BRAND['primary_color']}) to teal ({BRAND['accent_color']})
- White text

Dimensions: 1080x1080 pixels, square format."""
    },

    # -------------------------------------------------------------------------
    # VIDEO THUMBNAILS
    # -------------------------------------------------------------------------
    "VIDEO-OVERVIEW": {
        "name": "Platform Overview Thumbnail",
        "category": "video",
        "dimensions": "1920x1080",
        "prompt": f"""Create a video thumbnail/title card for platform overview.

Design:
- Text placeholder for: "Healthcare Software Built By People Who Care"
- Warm image of diverse patients looking healthy
- Subtle play button overlay
- HDIM logo
- Professional, inviting, clickable feel

Colors: Deep blue ({BRAND['primary_color']}), warm teal ({BRAND['accent_color']}), warm skin tones

Dimensions: 1920x1080 pixels, landscape orientation."""
    },

    "VIDEO-DEMO": {
        "name": "Demo Video Title Card",
        "category": "video",
        "dimensions": "1920x1080",
        "prompt": f"""Create a video title card for product demo.

Design:
- Text placeholder for: "See HDIM in Action"
- Dashboard screenshot preview
- HDIM branding
- Clean, modern, professional
- Suggests valuable educational content
- Subtle play button indicator

Colors: Deep blue ({BRAND['primary_color']}), teal ({BRAND['accent_color']}) accents, white background

Dimensions: 1920x1080 pixels, landscape orientation."""
    },

    # -------------------------------------------------------------------------
    # EMAIL GRAPHICS
    # -------------------------------------------------------------------------
    "EMAIL-BANNER": {
        "name": "Email Header Banner",
        "category": "email",
        "dimensions": "600x150",
        "prompt": f"""Create an email header banner for HDIM communications.

Design:
- HDIM logo on left
- Subtle healthcare pattern/graphic on right
- Deep blue background ({BRAND['primary_color']})
- Optimized for email clients (no transparency needed)
- Professional, trustworthy feel

Dimensions: 600x150 pixels, landscape orientation."""
    },
}

# =============================================================================
# API FUNCTIONS
# =============================================================================

def generate_image(prompt_id: str, prompt_data: Dict, output_dir: Path) -> Optional[Path]:
    """Generate a single image using Gemini API via direct REST call."""
    print(f"\n{'='*60}")
    print(f"Generating: {prompt_id} - {prompt_data['name']}")
    print(f"Category: {prompt_data['category']}")
    print(f"Dimensions: {prompt_data['dimensions']}")
    print(f"{'='*60}")

    try:
        # Create output directory for category
        category_dir = output_dir / prompt_data['category']
        category_dir.mkdir(parents=True, exist_ok=True)

        print(f"Using model: {MODEL_NAME}")

        # Prepare the prompt - escape for JSON
        prompt = prompt_data['prompt']
        escaped_prompt = prompt.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')

        # Build the API request using curl (proven working approach)
        api_url = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:generateContent?key={GOOGLE_API_KEY}"

        request_body = f'''{{
            "contents": [{{
                "parts": [{{
                    "text": "Generate this image: {escaped_prompt}"
                }}]
            }}],
            "generationConfig": {{
                "responseModalities": ["image", "text"],
                "responseMimeType": "text/plain"
            }}
        }}'''

        # Make the API call
        result = subprocess.run(
            ['curl', '-s', api_url, '-H', 'Content-Type: application/json', '-d', request_body],
            capture_output=True,
            text=True,
            timeout=120
        )

        response_text = result.stdout

        # Save response for debugging
        response_filepath = category_dir / f"{prompt_id}_{TIMESTAMP}_response.json"
        with open(response_filepath, 'w') as f:
            f.write(response_text)

        # Parse response
        try:
            response = json.loads(response_text)
        except json.JSONDecodeError:
            print(f"ERROR: Invalid JSON response")
            return None

        # Check for API error
        if 'error' in response:
            error_msg = response['error'].get('message', 'Unknown error')
            print(f"API Error: {error_msg}")
            return None

        # Extract base64 image data
        image_data = None
        candidates = response.get('candidates', [])
        if candidates:
            parts = candidates[0].get('content', {}).get('parts', [])
            for part in parts:
                if 'inlineData' in part:
                    image_data = part['inlineData'].get('data', '')
                    break

        if image_data:
            # Decode and save image
            filename = f"{prompt_id}_{TIMESTAMP}.png"
            filepath = category_dir / filename

            with open(filepath, 'wb') as f:
                f.write(base64.b64decode(image_data))

            print(f"SUCCESS: Saved to {filepath}")

            # Remove response file on success
            response_filepath.unlink(missing_ok=True)

            # Save metadata
            meta_filepath = category_dir / f"{prompt_id}_{TIMESTAMP}_meta.json"
            with open(meta_filepath, 'w') as f:
                json.dump({
                    "prompt_id": prompt_id,
                    "name": prompt_data['name'],
                    "category": prompt_data['category'],
                    "dimensions": prompt_data['dimensions'],
                    "model": MODEL_NAME,
                    "timestamp": TIMESTAMP,
                    "prompt": prompt_data['prompt'][:500] + "..."
                }, f, indent=2)

            return filepath
        else:
            print(f"WARNING: No image data in response")
            print(f"Response saved to: {response_filepath}")
            return None

    except subprocess.TimeoutExpired:
        print(f"ERROR: API request timed out")
        return None
    except Exception as e:
        print(f"ERROR generating {prompt_id}: {str(e)}")

        # Save error for debugging
        error_dir = output_dir / "errors"
        error_dir.mkdir(parents=True, exist_ok=True)
        error_filepath = error_dir / f"{prompt_id}_{TIMESTAMP}_error.json"
        with open(error_filepath, 'w') as f:
            json.dump({
                "prompt_id": prompt_id,
                "error": str(e),
                "timestamp": TIMESTAMP
            }, f, indent=2)

        return None

def generate_category(category: str, output_dir: Path) -> List[Path]:
    """Generate all images in a category."""
    results = []
    prompts_in_category = {k: v for k, v in PROMPTS.items() if v['category'] == category}

    if not prompts_in_category:
        print(f"No prompts found for category: {category}")
        return results

    print(f"\n{'#'*60}")
    print(f"# GENERATING CATEGORY: {category.upper()}")
    print(f"# {len(prompts_in_category)} assets to generate")
    print(f"{'#'*60}")

    for i, (prompt_id, prompt_data) in enumerate(prompts_in_category.items()):
        result = generate_image(prompt_id, prompt_data, output_dir)
        if result:
            results.append(result)

        # Rate limiting
        if i < len(prompts_in_category) - 1:
            print(f"Waiting {REQUEST_DELAY_SECONDS}s for rate limiting...")
            time.sleep(REQUEST_DELAY_SECONDS)

    return results

def generate_all(output_dir: Path) -> Dict[str, List[Path]]:
    """Generate all images."""
    results = {}
    categories = sorted(set(p['category'] for p in PROMPTS.values()))

    print(f"\n{'#'*60}")
    print(f"# GENERATING ALL ASSETS")
    print(f"# {len(PROMPTS)} total assets across {len(categories)} categories")
    print(f"# Categories: {', '.join(categories)}")
    print(f"{'#'*60}")

    for category in categories:
        results[category] = generate_category(category, output_dir)

    return results

def list_assets():
    """List all available assets."""
    print("\n" + "="*70)
    print("HDIM LANDING PAGE VISUAL ASSETS")
    print("="*70)

    categories = {}
    for prompt_id, data in PROMPTS.items():
        cat = data['category']
        if cat not in categories:
            categories[cat] = []
        categories[cat].append((prompt_id, data))

    for category, assets in sorted(categories.items()):
        print(f"\n{category.upper()} ({len(assets)} assets)")
        print("-" * 40)
        for prompt_id, data in assets:
            print(f"  {prompt_id:20} | {data['name']:30} | {data['dimensions']}")

    print(f"\nTOTAL: {len(PROMPTS)} assets")
    print("="*70)

# =============================================================================
# MAIN
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description="HDIM Landing Page Visual Asset Generator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )

    parser.add_argument('--all', action='store_true', help='Generate all assets')
    parser.add_argument('--category', type=str, help='Generate assets in specific category')
    parser.add_argument('--asset', type=str, help='Generate specific asset by ID')
    parser.add_argument('--list', action='store_true', help='List all available assets')
    parser.add_argument('--output', type=str, default='./generated', help='Output directory')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be generated without calling API')

    args = parser.parse_args()

    # Handle list
    if args.list:
        list_assets()
        return

    # Setup output directory
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)

    # Dry run
    if args.dry_run:
        print("\n[DRY RUN MODE - No API calls will be made]")
        if args.all:
            print(f"Would generate {len(PROMPTS)} assets")
            list_assets()
        elif args.category:
            count = len([p for p in PROMPTS.values() if p['category'] == args.category])
            print(f"Would generate {count} assets in category: {args.category}")
        elif args.asset:
            if args.asset in PROMPTS:
                print(f"Would generate: {args.asset} - {PROMPTS[args.asset]['name']}")
            else:
                print(f"Unknown asset ID: {args.asset}")
        return

    # Validate API key
    if not GOOGLE_API_KEY or GOOGLE_API_KEY == "your-key-here":
        print("ERROR: GOOGLE_API_KEY not set")
        print("Set it with: export GOOGLE_API_KEY='your-key'")
        sys.exit(1)

    print(f"\n{'='*60}")
    print("HDIM LANDING PAGE VISUAL ASSET GENERATOR")
    print(f"{'='*60}")
    print(f"API Key: {GOOGLE_API_KEY[:10]}...{GOOGLE_API_KEY[-4:]}")
    print(f"Model: {MODEL_NAME}")
    print(f"Output: {output_dir.absolute()}")
    print(f"Timestamp: {TIMESTAMP}")
    print(f"{'='*60}")

    # Generate based on args
    if args.all:
        results = generate_all(output_dir)
        print(f"\n{'='*60}")
        print("GENERATION COMPLETE")
        print(f"{'='*60}")
        for category, paths in results.items():
            print(f"{category}: {len(paths)} generated")
        total = sum(len(p) for p in results.values())
        print(f"TOTAL: {total}/{len(PROMPTS)} assets generated")

    elif args.category:
        results = generate_category(args.category, output_dir)
        print(f"\n{len(results)} assets generated in {args.category}")

    elif args.asset:
        if args.asset not in PROMPTS:
            print(f"Unknown asset ID: {args.asset}")
            print("Use --list to see available assets")
            sys.exit(1)
        result = generate_image(args.asset, PROMPTS[args.asset], output_dir)
        if result:
            print(f"\nGenerated: {result}")
        else:
            print(f"\nFailed to generate {args.asset}")

    else:
        parser.print_help()

if __name__ == "__main__":
    main()

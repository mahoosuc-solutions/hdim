# HDIM Marketing Image Generation Prompts

## Multi-Platform Support

This prompt library supports multiple image generation platforms:

| Platform | Type | Best For |
|----------|------|----------|
| **Gemini 2.5 Pro** | API/CLI | Detailed prompts, text understanding |
| **DALL-E 3** | API | Photorealistic, high quality |
| **Midjourney v6** | Discord | Creative/artistic style |
| **Stable Diffusion 3** | Local | Self-hosted, customizable |
| **Imagen 3** | API | Google's latest image model |

### Quick Start

```bash
# Python script (multi-platform)
python generate_images.py --platform gemini --model gemini-2.5-pro --all
python generate_images.py --platform dalle --category linkedin
python generate_images.py --export-midjourney
python generate_images.py --export-sd

# Shell script
./generate-marketing-images.sh --platform gemini --all
./generate-marketing-images.sh --export-midjourney
./generate-marketing-images.sh --list
```

---

## Brand Guidelines

**Primary Color:** #1E3A5F (Deep Navy Blue)
**Secondary Color:** #00A9A5 (Teal/Cyan)
**Success Color:** #2E7D32 (Green)
**Warning Color:** #E65100 (Orange)

**Target Audience:**
- Hospital CIOs (Chief Information Officers)
- CMIOs (Chief Medical Information Officers)
- CTOs (Chief Technology Officers)
- VP of IT / IT Directors
- Healthcare IT Decision Makers

**Key Messages:**
- 82% faster prior authorizations
- 85% reduction in IT maintenance tickets
- 60-90 day deployment (vs 12-18 months)
- $24.8B market opportunity
- CMS 2026 FHIR compliance deadline

---

## LinkedIn Ad Images (1200x1200 or 1200x628)

### AD-01: Pain Point - Integration Nightmare

**Prompt:**
```
Create a photorealistic 4K marketing image for LinkedIn advertising.

Scene: A stressed hospital IT professional (male, 40s, professional attire) standing in front of a massive wall of tangled network cables and server racks. Red warning lights blink throughout. The environment is chaotic - papers scattered, multiple monitors showing error messages, phones ringing.

Mood: Frustration, overwhelm, urgency
Style: Corporate photography with dramatic lighting
Colors: Dark blues, warning reds, harsh fluorescent lighting
Composition: Subject left of center, chaos filling the frame, clear space upper-right for text overlay

This represents the $500K/year integration nightmare that hospital IT teams face daily.
```

**Text Overlay (add in Canva/Figma):**
- Headline: "Still Fighting Integration Fires?"
- Subhead: "$500K/year. 6-month backlogs. Sound familiar?"
- CTA: "There's a better way →"

---

### AD-02: Solution - Modern Dashboard

**Prompt:**
```
Create a photorealistic 4K marketing image for LinkedIn advertising.

Scene: A confident healthcare CIO (female, 50s, executive attire) in a modern, bright hospital command center. She stands before a wall of organized monitors showing green status indicators and smooth data flow visualizations. The room has glass walls with natural light, modern furniture, and a calm atmosphere.

Mood: Confidence, control, success, relief
Style: Modern corporate photography, Apple-esque clean aesthetic
Colors: Clean whites, deep navy blue (#1E3A5F), teal accents (#00A9A5)
Composition: Subject right of center, clean monitors behind, space on left for text

This represents the calm, controlled environment after implementing HDIM.
```

**Text Overlay:**
- Headline: "From Chaos to Control"
- Subhead: "82% faster. 85% fewer tickets. 60-90 days to deploy."
- CTA: "See how →"

---

### AD-03: Before/After Split Screen

**Prompt:**
```
Create a dramatic 4K split-screen comparison image for LinkedIn advertising.

LEFT SIDE - "BEFORE" (Legacy Integration):
- Chaotic server room with tangled cables
- Multiple disconnected systems on old CRT monitors
- Stressed IT worker on phone with head in hands
- Red/orange warning lights and error messages
- Cluttered, cramped space
- Label: "3.5 DAYS for prior auth"

RIGHT SIDE - "AFTER" (HDIM Platform):
- Clean, organized modern IT center
- Single unified dashboard on sleek curved monitor
- Relaxed IT professional smiling at camera
- Green status indicators everywhere
- Spacious, well-lit environment
- Label: "6 HOURS for prior auth"

Dividing Line: Sharp vertical split with subtle glow effect
Style: High-end commercial photography, magazine quality
```

**Text Overlay:**
- Headline: "Same Hospital. Different World."
- CTA: "Make the switch →"

---

### AD-04: CMS 2026 Deadline Urgency

**Prompt:**
```
Create a 4K marketing image conveying urgency around healthcare compliance.

Scene: Hospital boardroom with concerned executives around a conference table. A large screen displays "CMS-0057-F COMPLIANCE" with a countdown timer showing "JANUARY 1, 2026". Documents with regulatory headers are spread on the table. The mood is serious but not panicked - professionals taking a deadline seriously.

Key Elements:
- Large digital countdown timer or calendar visualization
- "2026" prominently visible
- Concerned but professional expressions
- Regulatory compliance documents visible
- Modern hospital boardroom setting

Mood: Urgency, concern, time pressure, professionalism
Style: Corporate boardroom photography
Colors: Deep blues, urgent amber/gold for countdown, professional lighting
```

**Text Overlay:**
- Headline: "January 1, 2026"
- Subhead: "The largest healthcare IT mandate since HITECH. Are you ready?"
- CTA: "Get compliant →"

---

### AD-05: ROI / Cost Savings

**Prompt:**
```
Create a 4K marketing image visualizing significant cost savings.

Scene: A hospital CFO or CIO at an executive desk, looking pleased while reviewing a tablet showing positive financial data. Behind them, a large monitor displays an upward-trending graph with clear ROI visualization. The office is modern with hospital campus visible through windows. A subtle "$420K SAVED" indicator is visible on screen.

Mood: Success, satisfaction, financial confidence
Style: Executive portrait photography, warm professional lighting
Colors: Navy blue (#1E3A5F), teal (#00A9A5), gold/green for positive indicators
Composition: Executive 2/3 frame, positive metrics visible behind
```

**Text Overlay:**
- Headline: "$420K Saved in Year One"
- Subhead: "Regional Medical Center's real results"
- CTA: "Calculate your ROI →"

---

### AD-06: CIO Leadership

**Prompt:**
```
Create a 4K image targeting hospital CIOs.

Scene: A confident CIO (male, 50s, executive presence, navy suit) presenting to a hospital board in a modern boardroom. Large screen behind shows a strategic technology roadmap with "FHIR Compliance", "AI Integration", and "Cost Reduction" milestones. Board members are engaged and nodding. Through glass walls, hospital operations are subtly visible.

Mood: Leadership, strategic vision, confidence, influence
Style: Executive boardroom photography
Colors: Navy suits, warm boardroom lighting, teal (#00A9A5) on screen
Composition: CIO as focal point presenting, board engaged, strategy visible
```

**Text Overlay:**
- Headline: "Lead the Digital Transformation"
- Subhead: "Your board is watching. Deliver results."
- CTA: "Get the playbook →"

---

### AD-07: CMIO Clinical + Tech Bridge

**Prompt:**
```
Create a 4K image targeting hospital CMIOs.

Scene: A CMIO (female physician, white coat, 45-55, executive presence) at the intersection of clinical care and technology. Split composition: one side shows subtle patient care imagery (stethoscope, caring interaction), the other shows clean data dashboards and EHR screens. The CMIO bridges both worlds confidently, standing at the center.

Mood: Bridge-builder, clinical excellence meets technology
Style: Professional healthcare photography
Colors: White coat prominent, navy (#1E3A5F) tech elements, warm clinical lighting
Composition: CMIO centered, bridging clinical and tech worlds
```

**Text Overlay:**
- Headline: "Where Clinical Meets Technology"
- Subhead: "Better data. Better decisions. Better outcomes."
- CTA: "See the impact →"

---

### AD-08: VP IT Operations Excellence

**Prompt:**
```
Create a 4K image targeting VP of IT / IT Directors.

Scene: A VP of IT (any gender, 40s, business casual professional) in a modern network operations center. All monitors show green/healthy status. The environment is calm and organized - the opposite of crisis mode. Team members in background work normally, no urgency or stress visible.

Mood: Control, efficiency, operational excellence, calm confidence
Style: Corporate IT environment photography
Colors: Modern office lighting, green status indicators, teal dashboard accents
Composition: VP in foreground, healthy operations center behind
```

**Text Overlay:**
- Headline: "Finally. Green Across the Board."
- Subhead: "IT tickets down 85%. Integration backlog: zero."
- CTA: "Get there →"

---

## Hero Images (1920x1080 or 3840x2160)

### HERO-01: Main Website - Data Flow

**Prompt:**
```
Create a stunning abstract 4K hero image for a healthcare technology website.

Visual: An elegant visualization of healthcare data flowing seamlessly between connected systems. Network of glowing nodes (representing hospitals, clinics, payers) connected by smooth, flowing data streams in teal (#00A9A5). Background transitions from deep navy (#1E3A5F) to lighter blue. Subtle medical icons (heart rate, DNA helix, medical cross) integrated into the flowing data.

Mood: Innovation, connection, seamless flow, trust
Style: Abstract digital art, premium tech aesthetic (Stripe/Notion style)
Colors: Navy (#1E3A5F) to blue gradient, teal (#00A9A5) glowing streams
Composition: Data flows left to right, open space on left for text
```

---

### HERO-02: Platform Architecture Isometric

**Prompt:**
```
Create a 4K isometric visualization of a healthcare integration platform.

Visual: A central hub platform glowing in teal (#00A9A5) with "AI" symbolism. Connected to it are various healthcare systems as clean, modern blocks: Epic, Cerner, payer systems, labs, pharmacies. Data flows as smooth glowing streams through the central hub. Everything connects through the platform, not point-to-point.

Mood: Unified, intelligent, modern, comprehensive
Style: 3D isometric illustration, modern SaaS infographic
Colors: Navy (#1E3A5F) background, teal (#00A9A5) hub, white/blue systems
Composition: Central hub with radial connections, symmetrical
```

---

### HERO-03: AI Healthcare Fusion

**Prompt:**
```
Create a 4K abstract image representing AI-powered healthcare technology.

Visual: Abstract representation of AI and healthcare merging. A stylized neural network pattern forming a subtle human/brain outline. Medical symbols (caduceus, heart, DNA) integrated into the AI visualization. Glowing connections suggest intelligence and learning. Balance of human care and machine intelligence.

Mood: Innovation, intelligence, care, future-forward
Style: Abstract digital art, premium tech aesthetic
Colors: Navy (#1E3A5F) background, teal (#00A9A5) and white glows
Composition: Centered subject, room for text on sides
```

---

## Pitch Deck Slides (1920x1080 16:9)

### PITCH-01: Problem Visualization

**Prompt:**
```
Create a 4K conceptual image for an investor pitch deck "Problem" slide.

Visual: A complex maze viewed from above, with tiny figures (IT staff) lost and struggling to navigate. Dead ends, wrong turns, wasted effort visible. Some paths glow red showing errors/failures. The maze is made of server-like structures and cable-like paths. Overwhelming complexity.

Mood: Complexity, frustration, wasted resources
Style: Conceptual illustration, investor presentation quality
Colors: Grays, muted blues, red for problem areas
Composition: Bird's eye view, dramatic lighting, shows scale of problem
```

---

### PITCH-02: Solution Breakthrough

**Prompt:**
```
Create a 4K conceptual image for an investor pitch deck "Solution" slide.

Visual: The same maze, but now with a clear, glowing teal (#00A9A5) path cutting straight through. Or the maze dissolving/transforming into a simple elegant bridge. The HDIM solution as a beacon of light cutting through complexity. Visual transformation from chaos to clarity.

Mood: Clarity, breakthrough, elegance, simplicity
Style: Conceptual illustration matching problem slide
Colors: Solution path in teal (#00A9A5), complexity fading to gray
Composition: Clear visual transformation story
```

---

### PITCH-03: Market Opportunity ($24.8B)

**Prompt:**
```
Create a 4K conceptual image visualizing massive market opportunity.

Visual: Expansive visualization of market size - could be a vast landscape with sunrise suggesting growth, or an expanding universe of opportunity. Healthcare symbols scattered throughout expanding space. Growth trajectory visible. Sense of enormous scale and potential.

Mood: Massive opportunity, growth, expansion, optimism
Style: Abstract conceptual art, premium investor presentation
Colors: Navy (#1E3A5F) space, teal (#00A9A5) and gold for opportunity
Composition: Expansive, opening toward viewer, conveys scale
```

---

### PITCH-04: Traction Momentum

**Prompt:**
```
Create a 4K conceptual image showing startup traction and momentum.

Visual: A stylized rocket or spacecraft in flight - professional, not cartoonish. Clear upward trajectory. Trail behind shows milestones achieved. Stars ahead show future goals. Subtle healthcare/tech elements incorporated. Vast potential ahead.

Mood: Momentum, achievement, velocity, ambition
Style: Stylized conceptual illustration, premium quality
Colors: Navy (#1E3A5F) space, teal (#00A9A5) trajectory, white stars
Composition: Diagonal upward movement, clear trajectory
```

---

## Social Proof Images

### SOCIAL-01: Hospital Success Story

**Prompt:**
```
Create a 4K image representing hospital transformation success.

Scene: Modern hospital exterior at golden hour with optimistic lighting. Contemporary, well-maintained building. In foreground, happy diverse healthcare workers (doctor, nurse, IT professional) walking together confidently. Subtle digital elements suggest tech advancement. Clear sky, bright future.

Mood: Success, optimism, transformation, teamwork
Style: Commercial healthcare photography
Colors: Warm golden hour, clean whites, teal (#00A9A5) accents
Composition: Hospital backdrop, people in foreground, aspirational
```

---

### SOCIAL-02: IT Team Celebration

**Prompt:**
```
Create a 4K image of hospital IT team celebrating success.

Scene: Modern hospital IT department. Diverse team of 4-5 IT professionals celebrating - high fives, genuine smiles, looking at positive metrics. Monitors behind show green status and positive trends. Clean, organized, modern environment.

Mood: Achievement, relief, team success, genuine celebration
Style: Corporate photography, authentic emotion
Colors: Bright modern lighting, teal (#00A9A5) accents, green indicators
Composition: Team centered, success metrics visible behind
```

---

## Image Specifications by Platform

| Platform | Dimensions | Format |
|----------|------------|--------|
| LinkedIn Feed | 1200x1200 | Square |
| LinkedIn Sponsored | 1200x628 | Landscape |
| Website Hero | 1920x1080+ | Wide |
| Email Header | 600x200 | Banner |
| Pitch Deck | 1920x1080 | 16:9 |
| Twitter/X | 1200x675 | 16:9 |
| Instagram | 1080x1080 | Square |

---

## Usage Instructions

### Method 1: Python Script (Recommended)

```bash
# Install dependencies
pip install google-generativeai openai pillow requests

# Set API keys
export GOOGLE_API_KEY="your-google-api-key"
export OPENAI_API_KEY="your-openai-api-key"

# Generate with Gemini 2.5 Pro
python generate_images.py --platform gemini --model gemini-2.5-pro --all
python generate_images.py --platform gemini --category linkedin
python generate_images.py --platform gemini --single AD-01

# Generate with DALL-E 3
python generate_images.py --platform dalle --all
python generate_images.py --platform dalle --single HERO-01

# Export for Midjourney (no API needed)
python generate_images.py --export-midjourney

# Export for Stable Diffusion (no API needed)
python generate_images.py --export-sd

# List all prompts
python generate_images.py --list

# Show usage guide
python generate_images.py --guide
```

### Method 2: Shell Script

```bash
# Make executable
chmod +x generate-marketing-images.sh

# Generate with Gemini
./generate-marketing-images.sh --platform gemini --model gemini-2.5-pro --all

# Export for Midjourney
./generate-marketing-images.sh --export-midjourney

# List prompts
./generate-marketing-images.sh --list
```

### Method 3: Direct API Calls

#### Gemini 2.5 Pro:
```bash
gemini -p "[PROMPT]" --output "./output.png" --model gemini-2.5-pro
```

#### Gemini API (Python):
```python
import google.generativeai as genai
genai.configure(api_key="YOUR_API_KEY")
model = genai.GenerativeModel('gemini-2.5-pro-preview-06-05')
response = model.generate_content("[PROMPT]")
```

#### DALL-E 3 API:
```python
from openai import OpenAI
client = OpenAI()
response = client.images.generate(
    model="dall-e-3",
    prompt="[PROMPT]",
    size="1024x1024",
    quality="hd",
    style="natural"
)
```

### Method 4: Manual Generation

#### Midjourney:
Copy prompts from exported file and paste in Discord:
```
/imagine prompt: [PROMPT] --ar 1:1 --v 6.1 --style raw --q 2
```

#### Stable Diffusion:
Use exported prompts with these settings:
- **Negative prompt:** `cartoon, anime, illustration, low quality, blurry, text, watermark, logo, signature, deformed, ugly, bad anatomy`
- **Steps:** 50
- **CFG Scale:** 7.5
- **Sampler:** DPM++ 2M Karras

### Supported Models

| Platform | Model ID | Notes |
|----------|----------|-------|
| Gemini | `gemini-2.5-pro` | Latest, best quality |
| Gemini | `gemini-2.0-flash` | Faster, good quality |
| Gemini | `imagen-3` | Image-specific model |
| OpenAI | `dall-e-3` | HD quality available |
| Midjourney | v6.1 | Via Discord |
| Stable Diffusion | SD3, SDXL | Local generation |

---

## A/B Testing Recommendations

1. **Headlines**: Test benefit-focused vs. pain-focused
2. **Imagery**: Test photos vs. abstract illustrations
3. **CTA**: Test "See how" vs. "Get started" vs. "Calculate ROI"
4. **Color**: Test teal CTA vs. orange CTA
5. **Social Proof**: Test with/without customer logos

---

## Post-Processing Checklist

- [ ] Add HDIM logo (upper left corner)
- [ ] Add text overlays (headline, subhead, CTA)
- [ ] Add subtle gradient overlay for text readability
- [ ] Export at platform-specific sizes
- [ ] Check mobile preview
- [ ] Verify brand color accuracy
- [ ] Add tracking UTM parameters to landing page URLs

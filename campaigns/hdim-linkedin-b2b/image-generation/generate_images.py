#!/usr/bin/env python3
"""
HDIM Marketing Image Generator - Multi-Platform Support
Supports: Gemini 2.5 Pro, DALL-E 3, Midjourney, Stable Diffusion

Requirements:
    pip install google-generativeai openai pillow requests

Usage:
    # Gemini (default)
    python generate_images.py --platform gemini --all
    python generate_images.py --platform gemini --model gemini-2.5-pro --category linkedin

    # DALL-E 3
    python generate_images.py --platform dalle --all

    # Export prompts for Midjourney/Stable Diffusion
    python generate_images.py --export-midjourney
    python generate_images.py --export-sd
"""

import os
import sys
import json
import argparse
from datetime import datetime
from pathlib import Path
from typing import Dict, Optional, Any

# =============================================================================
# CONFIGURATION
# =============================================================================

BRAND = {
    "primary": "#1E3A5F",      # Deep Navy Blue
    "secondary": "#00A9A5",    # Teal
    "success": "#2E7D32",      # Green
    "warning": "#E65100",      # Orange
    "company": "HDIM",
    "tagline": "Healthcare Data Integration Made Intelligent"
}

OUTPUT_DIR = Path("./generated-images")
TIMESTAMP = datetime.now().strftime("%Y%m%d_%H%M%S")

# Model configurations
MODELS = {
    "gemini": {
        "gemini-2.5-pro": "gemini-2.5-pro-preview-06-05",
        "gemini-2.0-flash": "gemini-2.0-flash-exp",
        "gemini-pro-vision": "gemini-pro-vision",
        "imagen-3": "imagen-3.0-generate-001",
    },
    "dalle": {
        "dalle-3": "dall-e-3",
        "dalle-3-hd": "dall-e-3",  # with quality=hd
    },
    "stability": {
        "sd-3": "stable-diffusion-3",
        "sdxl": "stable-diffusion-xl-1024-v1-0",
    }
}

# =============================================================================
# IMAGE PROMPTS - BASE PROMPTS
# =============================================================================

PROMPTS = {
    # -------------------------------------------------------------------------
    # LINKEDIN ADS (1200x1200 or 1200x628)
    # -------------------------------------------------------------------------
    "AD-01": {
        "name": "Pain Point - Integration Nightmare",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x1200", "square": "1080x1080"},
        "prompt": f"""Create a photorealistic 4K marketing image for LinkedIn advertising targeting hospital CIOs.

Scene: A stressed hospital IT professional (male, 40s, professional attire) standing in front of a massive wall of tangled network cables and server racks. Red warning lights blink throughout. The environment is chaotic - papers scattered, multiple monitors showing error messages. The mood is frustration and overwhelm.

Style: Corporate photography with dramatic lighting, cinematic quality
Colors: Dark blues, warning reds, harsh fluorescent lighting
Composition: Subject left of center, clear space upper-right for text overlay
Resolution: High detail, 4K quality

This represents the $500K/year integration nightmare that hospital IT teams face daily. Make it feel real and relatable to enterprise IT leaders."""
    },

    "AD-02": {
        "name": "Solution - Modern Dashboard",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x1200", "square": "1080x1080"},
        "prompt": f"""Create a photorealistic 4K marketing image showing healthcare IT transformation success.

Scene: A confident healthcare CIO (female, 50s, executive attire) in a modern, bright hospital command center. She stands before a wall of organized monitors showing green status indicators and smooth data flow visualizations. Natural light, glass walls, modern furniture, calm atmosphere.

Mood: Confidence, control, success, relief
Style: Modern corporate photography, Apple-esque clean aesthetic
Colors: Clean whites, deep navy blue ({BRAND['primary']}), teal accents ({BRAND['secondary']})
Composition: Subject right of center, clean monitors behind, space on left for text

This represents the future after implementing {BRAND['company']} - calm, controlled, efficient."""
    },

    "AD-03": {
        "name": "Before/After Split Screen",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x628", "wide": "1920x1080"},
        "prompt": f"""Create a dramatic 4K split-screen comparison image showing healthcare IT transformation.

LEFT SIDE - "BEFORE" (Legacy):
- Chaotic server room, tangled cables
- Old monitors with error messages
- Stressed IT worker on phone
- Red warning lights, cluttered space
- Subtle text: "3.5 DAYS"

RIGHT SIDE - "AFTER" ({BRAND['company']}):
- Clean modern IT center
- Unified dashboard, curved monitor
- Relaxed professional smiling
- Green status indicators
- Subtle text: "6 HOURS"

Sharp vertical dividing line with subtle glow effect.
Style: High-end commercial photography, magazine quality
Colors: Left side dark/red, right side bright with teal ({BRAND['secondary']}) accents"""
    },

    "AD-04": {
        "name": "CMS 2026 Deadline Urgency",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x1200", "square": "1080x1080"},
        "prompt": f"""Create a 4K marketing image conveying urgency around the CMS 2026 healthcare compliance deadline.

Scene: Hospital boardroom with concerned but professional executives. Large screen displays "JANUARY 1, 2026" with countdown timer. Documents with "CMS-0057-F" visible on table. Mood is serious professionalism, not panic.

Key Elements:
- Large countdown visualization showing 2026
- Concerned executive expressions
- Regulatory documents visible
- Modern hospital boardroom

Colors: Deep blues, urgent amber/gold for countdown elements
Style: Corporate boardroom photography
Composition: Wide shot showing room, screen prominent"""
    },

    "AD-05": {
        "name": "ROI Cost Savings",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x1200", "square": "1080x1080"},
        "prompt": f"""Create a 4K marketing image visualizing significant healthcare IT cost savings.

Scene: Hospital executive at modern desk, pleased expression, reviewing tablet with positive financial data. Large monitor behind shows upward-trending ROI graph. Modern executive office, hospital campus visible through windows. Subtle "$420K SAVED" visible on screen.

Mood: Success, satisfaction, financial confidence
Style: Executive portrait photography, warm professional lighting
Colors: Navy blue ({BRAND['primary']}), teal ({BRAND['secondary']}), gold/green positive indicators
Composition: Executive 2/3 frame, metrics visible behind"""
    },

    "AD-06": {
        "name": "CIO Leadership",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x1200", "square": "1080x1080"},
        "prompt": f"""Create a 4K image targeting hospital CIOs showing strategic leadership.

Scene: Confident CIO (male, 50s, navy suit) presenting to hospital board in modern boardroom. Screen shows technology roadmap with "FHIR Compliance", "AI Integration", "Cost Reduction" milestones. Board members engaged. Hospital operations visible through glass walls.

Mood: Leadership, strategic vision, confidence
Style: Executive boardroom photography
Colors: Navy suits, warm lighting, teal ({BRAND['secondary']}) screen accents
Composition: CIO presenting, board engaged, strategy visible on screen"""
    },

    "AD-07": {
        "name": "CMIO Clinical Tech Bridge",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x1200", "square": "1080x1080"},
        "prompt": f"""Create a 4K image targeting hospital CMIOs - bridging clinical and technology.

Scene: CMIO (female physician, white coat, 45-55) at intersection of clinical care and technology. Split composition: one side shows patient care (stethoscope, caring interaction), other side shows clean data dashboards. CMIO bridges both confidently.

Mood: Bridge-builder, clinical excellence meets technology
Style: Professional healthcare photography
Colors: White coat prominent, navy ({BRAND['primary']}) tech elements, warm clinical lighting
Composition: CMIO centered, bridging two worlds"""
    },

    "AD-08": {
        "name": "VP IT Operations",
        "category": "linkedin",
        "dimensions": {"linkedin": "1200x1200", "square": "1080x1080"},
        "prompt": f"""Create a 4K image targeting VP of IT showing operational excellence.

Scene: VP of IT (40s, business casual) in modern network operations center. All monitors show green healthy status. Calm, organized environment. Team works normally in background, no stress.

Mood: Control, efficiency, calm confidence
Style: Corporate IT environment photography
Colors: Modern lighting, green status indicators, teal ({BRAND['secondary']}) accents
Composition: VP in foreground, healthy operations behind"""
    },

    # -------------------------------------------------------------------------
    # HERO IMAGES (1920x1080 or 3840x2160)
    # -------------------------------------------------------------------------
    "HERO-01": {
        "name": "Data Flow Abstract",
        "category": "hero",
        "dimensions": {"hero": "1920x1080", "4k": "3840x2160"},
        "prompt": f"""Create a stunning abstract 4K hero image for a healthcare technology website.

Visual: Elegant visualization of healthcare data flowing between connected systems. Network of glowing nodes (hospitals, clinics, payers) connected by smooth flowing data streams in teal ({BRAND['secondary']}). Background transitions from deep navy ({BRAND['primary']}) to lighter blue. Subtle medical icons integrated.

Mood: Innovation, connection, seamless flow
Style: Abstract digital art, premium tech aesthetic like Stripe or Notion
Colors: Navy ({BRAND['primary']}) gradient, teal ({BRAND['secondary']}) glowing streams
Composition: Data flows left to right, open space left for text overlay"""
    },

    "HERO-02": {
        "name": "Platform Architecture",
        "category": "hero",
        "dimensions": {"hero": "1920x1080", "4k": "3840x2160"},
        "prompt": f"""Create a 4K isometric visualization of a healthcare integration platform.

Visual: Central hub platform glowing in teal ({BRAND['secondary']}) with AI symbolism. Connected healthcare systems as clean modern blocks: Epic, Cerner, payers, labs, pharmacies. Data flows as smooth streams through central hub. Platform-centric, not point-to-point.

Style: 3D isometric illustration, modern SaaS infographic
Colors: Navy ({BRAND['primary']}) background, teal ({BRAND['secondary']}) hub, white systems
Composition: Central hub with radial connections, symmetrical, clean"""
    },

    "HERO-03": {
        "name": "AI Healthcare Fusion",
        "category": "hero",
        "dimensions": {"hero": "1920x1080", "4k": "3840x2160"},
        "prompt": f"""Create a 4K abstract image representing AI-powered healthcare technology.

Visual: Neural network pattern forming subtle brain/human outline. Medical symbols (caduceus, heart, DNA) integrated into AI visualization. Glowing connections suggesting intelligence. Balance of human care and machine intelligence.

Mood: Innovation, intelligence, care
Style: Abstract digital art, premium tech aesthetic
Colors: Navy ({BRAND['primary']}) background, teal ({BRAND['secondary']}) and white glows
Composition: Centered, room for text on sides"""
    },

    # -------------------------------------------------------------------------
    # PITCH DECK (1920x1080 16:9)
    # -------------------------------------------------------------------------
    "PITCH-01": {
        "name": "Problem Maze",
        "category": "pitch",
        "dimensions": {"slide": "1920x1080"},
        "prompt": f"""Create a 4K conceptual image for investor pitch deck "Problem" slide.

Visual: Complex maze viewed from above. Tiny figures (IT staff) lost and struggling. Dead ends, wrong turns visible. Some paths glow red showing failures. Maze made of server-like structures and cable paths.

Mood: Complexity, frustration, scale of problem
Style: Conceptual illustration, investor presentation quality
Colors: Grays, muted blues, red problem areas
Composition: Bird's eye view, dramatic lighting"""
    },

    "PITCH-02": {
        "name": "Solution Breakthrough",
        "category": "pitch",
        "dimensions": {"slide": "1920x1080"},
        "prompt": f"""Create a 4K conceptual image for investor pitch deck "Solution" slide.

Visual: Same maze concept but now with clear glowing teal ({BRAND['secondary']}) path cutting through. Or maze dissolving into simple elegant bridge. Solution as beacon of light through complexity.

Mood: Clarity, breakthrough, elegance
Style: Conceptual illustration matching problem slide
Colors: Solution path teal ({BRAND['secondary']}), complexity fading gray
Composition: Visual transformation story"""
    },

    "PITCH-03": {
        "name": "Market Opportunity",
        "category": "pitch",
        "dimensions": {"slide": "1920x1080"},
        "prompt": f"""Create a 4K conceptual image visualizing $24.8B market opportunity.

Visual: Expansive landscape with sunrise suggesting growth, or expanding universe of opportunity. Healthcare symbols throughout. Growth trajectory visible. Enormous scale and potential.

Mood: Massive opportunity, growth, optimism
Style: Abstract conceptual art, premium investor presentation
Colors: Navy ({BRAND['primary']}) space, teal ({BRAND['secondary']}) and gold for opportunity
Composition: Expansive, opening toward viewer"""
    },

    "PITCH-04": {
        "name": "Traction Momentum",
        "category": "pitch",
        "dimensions": {"slide": "1920x1080"},
        "prompt": f"""Create a 4K conceptual image showing startup momentum and traction.

Visual: Stylized rocket/spacecraft in flight - professional not cartoonish. Clear upward trajectory. Trail shows milestones. Stars ahead show goals. Healthcare/tech elements incorporated.

Mood: Momentum, achievement, velocity
Style: Stylized conceptual illustration, premium
Colors: Navy ({BRAND['primary']}) space, teal ({BRAND['secondary']}) trajectory
Composition: Diagonal upward movement"""
    },

    # -------------------------------------------------------------------------
    # SOCIAL PROOF
    # -------------------------------------------------------------------------
    "SOCIAL-01": {
        "name": "Hospital Success",
        "category": "social",
        "dimensions": {"linkedin": "1200x1200", "twitter": "1200x675"},
        "prompt": f"""Create a 4K image representing hospital transformation success.

Scene: Modern hospital exterior at golden hour. Happy diverse healthcare workers (doctor, nurse, IT professional) walking together confidently. Digital elements suggest tech advancement. Clear sky, bright future feeling.

Mood: Success, optimism, teamwork
Style: Commercial healthcare photography
Colors: Warm golden hour, whites, teal ({BRAND['secondary']}) accents
Composition: Hospital backdrop, people foreground"""
    },

    "SOCIAL-02": {
        "name": "IT Team Celebration",
        "category": "social",
        "dimensions": {"linkedin": "1200x1200", "twitter": "1200x675"},
        "prompt": f"""Create a 4K image of hospital IT team celebrating success.

Scene: Modern IT department. Diverse team of 4-5 professionals celebrating - high fives, genuine smiles, looking at positive metrics. Monitors show green status. Clean, organized environment.

Mood: Achievement, genuine celebration
Style: Corporate photography, authentic emotion
Colors: Bright modern lighting, teal ({BRAND['secondary']}), green indicators
Composition: Team centered, success metrics visible"""
    },
}

# =============================================================================
# PLATFORM-SPECIFIC PROMPT FORMATTERS
# =============================================================================

def format_for_midjourney(prompt_data: Dict) -> str:
    """Format prompt for Midjourney with proper parameters."""
    base = prompt_data['prompt']
    dims = prompt_data.get('dimensions', {})

    # Get aspect ratio
    if 'linkedin' in dims:
        if dims['linkedin'] == "1200x1200":
            ar = "1:1"
        else:
            ar = "16:9"
    elif 'hero' in dims:
        ar = "16:9"
    elif 'slide' in dims:
        ar = "16:9"
    else:
        ar = "1:1"

    # Midjourney formatting
    mj_prompt = base.replace('\n', ' ').strip()
    mj_prompt = f"{mj_prompt} --ar {ar} --v 6.1 --style raw --q 2"

    return mj_prompt


def format_for_stable_diffusion(prompt_data: Dict) -> Dict:
    """Format prompt for Stable Diffusion with parameters."""
    base = prompt_data['prompt']
    dims = prompt_data.get('dimensions', {})

    # Get dimensions
    if 'linkedin' in dims:
        if dims['linkedin'] == "1200x1200":
            width, height = 1024, 1024
        else:
            width, height = 1024, 576
    elif 'hero' in dims or 'slide' in dims:
        width, height = 1024, 576
    else:
        width, height = 1024, 1024

    # Clean prompt for SD
    sd_prompt = base.replace('\n', ' ').strip()

    return {
        "prompt": sd_prompt,
        "negative_prompt": "cartoon, anime, illustration, low quality, blurry, text, watermark, logo, signature, deformed, ugly, bad anatomy",
        "width": width,
        "height": height,
        "steps": 50,
        "cfg_scale": 7.5,
        "sampler": "DPM++ 2M Karras"
    }


def format_for_dalle(prompt_data: Dict) -> Dict:
    """Format prompt for DALL-E 3 with parameters."""
    base = prompt_data['prompt']
    dims = prompt_data.get('dimensions', {})

    # DALL-E 3 supports: 1024x1024, 1792x1024, 1024x1792
    if 'linkedin' in dims:
        if dims['linkedin'] == "1200x1200":
            size = "1024x1024"
        else:
            size = "1792x1024"
    elif 'hero' in dims or 'slide' in dims:
        size = "1792x1024"
    else:
        size = "1024x1024"

    # Clean prompt for DALL-E
    dalle_prompt = base.replace('\n', ' ').strip()

    return {
        "model": "dall-e-3",
        "prompt": dalle_prompt,
        "size": size,
        "quality": "hd",
        "style": "natural"
    }


def format_for_gemini(prompt_data: Dict, model: str = "gemini-2.5-pro") -> Dict:
    """Format prompt for Gemini/Imagen with parameters."""
    return {
        "model": MODELS["gemini"].get(model, model),
        "prompt": prompt_data['prompt'],
        "generation_config": {
            "temperature": 0.9,
            "max_output_tokens": 8192,
        }
    }

# =============================================================================
# GENERATION FUNCTIONS
# =============================================================================

def setup_output_dirs():
    """Create output directory structure."""
    categories = ["linkedin", "hero", "pitch", "social", "exports"]
    for cat in categories:
        (OUTPUT_DIR / cat).mkdir(parents=True, exist_ok=True)
    print(f"Output directory: {OUTPUT_DIR}")


def generate_with_gemini(prompt_id: str, api_key: str, model: str = "gemini-2.5-pro"):
    """Generate image using Gemini API."""
    try:
        import google.generativeai as genai
    except ImportError:
        print("Error: google-generativeai not installed")
        print("Run: pip install google-generativeai")
        return None

    if prompt_id not in PROMPTS:
        print(f"Error: Unknown prompt ID '{prompt_id}'")
        return None

    prompt_data = PROMPTS[prompt_id]
    print(f"\n>>> Generating with Gemini: {prompt_id} - {prompt_data['name']}")

    try:
        genai.configure(api_key=api_key)

        # Get the correct model ID
        model_id = MODELS["gemini"].get(model, model)
        model_instance = genai.GenerativeModel(model_id)

        formatted = format_for_gemini(prompt_data, model)

        response = model_instance.generate_content(
            formatted['prompt'],
            generation_config=genai.types.GenerationConfig(**formatted['generation_config'])
        )

        # Save response
        output_path = OUTPUT_DIR / prompt_data['category'] / f"{prompt_id}_{TIMESTAMP}.txt"
        with open(output_path, 'w') as f:
            f.write(f"Prompt ID: {prompt_id}\n")
            f.write(f"Name: {prompt_data['name']}\n")
            f.write(f"Model: {model_id}\n")
            f.write(f"Timestamp: {TIMESTAMP}\n")
            f.write(f"\n{'='*60}\nPROMPT:\n{'='*60}\n")
            f.write(prompt_data['prompt'])
            f.write(f"\n\n{'='*60}\nRESPONSE:\n{'='*60}\n")
            f.write(str(response.text) if hasattr(response, 'text') else str(response))

        print(f"    Saved to: {output_path}")
        return response

    except Exception as e:
        print(f"    Error: {e}")
        return None


def generate_with_dalle(prompt_id: str, api_key: str):
    """Generate image using DALL-E 3 API."""
    try:
        from openai import OpenAI
    except ImportError:
        print("Error: openai not installed")
        print("Run: pip install openai")
        return None

    if prompt_id not in PROMPTS:
        print(f"Error: Unknown prompt ID '{prompt_id}'")
        return None

    prompt_data = PROMPTS[prompt_id]
    print(f"\n>>> Generating with DALL-E 3: {prompt_id} - {prompt_data['name']}")

    try:
        client = OpenAI(api_key=api_key)
        formatted = format_for_dalle(prompt_data)

        response = client.images.generate(
            model=formatted['model'],
            prompt=formatted['prompt'],
            size=formatted['size'],
            quality=formatted['quality'],
            style=formatted['style'],
            n=1
        )

        # Save image URL and revised prompt
        output_path = OUTPUT_DIR / prompt_data['category'] / f"{prompt_id}_dalle_{TIMESTAMP}.json"
        result = {
            "prompt_id": prompt_id,
            "name": prompt_data['name'],
            "timestamp": TIMESTAMP,
            "image_url": response.data[0].url,
            "revised_prompt": response.data[0].revised_prompt,
            "original_prompt": formatted['prompt']
        }

        with open(output_path, 'w') as f:
            json.dump(result, f, indent=2)

        print(f"    Image URL: {response.data[0].url}")
        print(f"    Saved to: {output_path}")
        return response

    except Exception as e:
        print(f"    Error: {e}")
        return None


def export_for_midjourney():
    """Export all prompts formatted for Midjourney."""
    output_path = OUTPUT_DIR / "exports" / f"midjourney_prompts_{TIMESTAMP}.md"

    with open(output_path, 'w') as f:
        f.write("# HDIM Marketing Images - Midjourney Prompts\n\n")
        f.write("Copy each prompt and paste into Midjourney Discord or web interface.\n\n")
        f.write("---\n\n")

        for prompt_id, data in PROMPTS.items():
            mj_prompt = format_for_midjourney(data)
            f.write(f"## {prompt_id}: {data['name']}\n")
            f.write(f"**Category:** {data['category']}\n\n")
            f.write("```\n")
            f.write(f"/imagine prompt: {mj_prompt}\n")
            f.write("```\n\n")
            f.write("---\n\n")

    print(f"Exported {len(PROMPTS)} Midjourney prompts to: {output_path}")
    return output_path


def export_for_stable_diffusion():
    """Export all prompts formatted for Stable Diffusion."""
    output_path = OUTPUT_DIR / "exports" / f"stable_diffusion_prompts_{TIMESTAMP}.json"

    sd_prompts = {}
    for prompt_id, data in PROMPTS.items():
        sd_prompts[prompt_id] = {
            "name": data['name'],
            "category": data['category'],
            **format_for_stable_diffusion(data)
        }

    with open(output_path, 'w') as f:
        json.dump(sd_prompts, f, indent=2)

    print(f"Exported {len(PROMPTS)} Stable Diffusion prompts to: {output_path}")

    # Also create a txt version for easy copy-paste
    txt_path = OUTPUT_DIR / "exports" / f"stable_diffusion_prompts_{TIMESTAMP}.txt"
    with open(txt_path, 'w') as f:
        for prompt_id, data in sd_prompts.items():
            f.write(f"{'='*60}\n")
            f.write(f"{prompt_id}: {data['name']}\n")
            f.write(f"{'='*60}\n\n")
            f.write(f"PROMPT:\n{data['prompt']}\n\n")
            f.write(f"NEGATIVE:\n{data['negative_prompt']}\n\n")
            f.write(f"SIZE: {data['width']}x{data['height']}\n")
            f.write(f"STEPS: {data['steps']}, CFG: {data['cfg_scale']}\n")
            f.write(f"SAMPLER: {data['sampler']}\n\n\n")

    print(f"Also saved text version: {txt_path}")
    return output_path


def export_all_prompts():
    """Export prompts in all formats."""
    output_path = OUTPUT_DIR / "exports" / f"all_prompts_{TIMESTAMP}.json"

    all_exports = {
        "metadata": {
            "brand": BRAND,
            "generated": TIMESTAMP,
            "prompt_count": len(PROMPTS)
        },
        "prompts": {}
    }

    for prompt_id, data in PROMPTS.items():
        all_exports["prompts"][prompt_id] = {
            "name": data['name'],
            "category": data['category'],
            "dimensions": data.get('dimensions', {}),
            "base_prompt": data['prompt'],
            "formatted": {
                "midjourney": format_for_midjourney(data),
                "stable_diffusion": format_for_stable_diffusion(data),
                "dalle": format_for_dalle(data),
                "gemini": format_for_gemini(data)
            }
        }

    with open(output_path, 'w') as f:
        json.dump(all_exports, f, indent=2)

    print(f"Exported all prompts to: {output_path}")
    return output_path


def generate_category(category: str, platform: str, api_key: str, model: str = None):
    """Generate all images in a category."""
    prompts = {k: v for k, v in PROMPTS.items() if v['category'] == category}
    print(f"\nGenerating {len(prompts)} images in category: {category}")

    for prompt_id in prompts:
        if platform == "gemini":
            generate_with_gemini(prompt_id, api_key, model or "gemini-2.5-pro")
        elif platform == "dalle":
            generate_with_dalle(prompt_id, api_key)


def generate_all(platform: str, api_key: str, model: str = None):
    """Generate all images."""
    print(f"\nGenerating all {len(PROMPTS)} images with {platform}...")

    for prompt_id in PROMPTS:
        if platform == "gemini":
            generate_with_gemini(prompt_id, api_key, model or "gemini-2.5-pro")
        elif platform == "dalle":
            generate_with_dalle(prompt_id, api_key)


def list_prompts():
    """List all available prompts."""
    print("\n" + "=" * 60)
    print("HDIM MARKETING IMAGE PROMPTS")
    print("=" * 60)

    categories = {}
    for prompt_id, data in PROMPTS.items():
        cat = data['category']
        if cat not in categories:
            categories[cat] = []
        categories[cat].append((prompt_id, data['name'], data.get('dimensions', {})))

    for cat, prompts in categories.items():
        print(f"\n{cat.upper()} ({len(prompts)} images):")
        for pid, name, dims in prompts:
            dim_str = ", ".join(f"{k}:{v}" for k, v in dims.items())
            print(f"  {pid}: {name}")
            if dim_str:
                print(f"        [{dim_str}]")


def print_usage_guide():
    """Print comprehensive usage guide."""
    print("""
╔══════════════════════════════════════════════════════════════════╗
║          HDIM MARKETING IMAGE GENERATOR - USAGE GUIDE            ║
╚══════════════════════════════════════════════════════════════════╝

SUPPORTED PLATFORMS:
  • Gemini 2.5 Pro (Google) - Best for detailed prompts, API generation
  • DALL-E 3 (OpenAI) - High quality photorealistic images
  • Midjourney - Creative/artistic style (export prompts)
  • Stable Diffusion - Local/self-hosted generation (export prompts)

─────────────────────────────────────────────────────────────────────
GEMINI GENERATION (API):
─────────────────────────────────────────────────────────────────────

  # Set API key
  export GOOGLE_API_KEY="your-api-key"

  # Generate all images
  python generate_images.py --platform gemini --all

  # Use specific model
  python generate_images.py --platform gemini --model gemini-2.5-pro --all
  python generate_images.py --platform gemini --model imagen-3 --single AD-01

  # Generate by category
  python generate_images.py --platform gemini --category linkedin

─────────────────────────────────────────────────────────────────────
DALL-E 3 GENERATION (API):
─────────────────────────────────────────────────────────────────────

  # Set API key
  export OPENAI_API_KEY="your-api-key"

  # Generate all images
  python generate_images.py --platform dalle --all

  # Generate single image
  python generate_images.py --platform dalle --single HERO-01

─────────────────────────────────────────────────────────────────────
MIDJOURNEY (Export Prompts):
─────────────────────────────────────────────────────────────────────

  # Export prompts formatted for Midjourney
  python generate_images.py --export-midjourney

  # Then copy prompts to Discord:
  /imagine prompt: [paste prompt here]

─────────────────────────────────────────────────────────────────────
STABLE DIFFUSION (Export Prompts):
─────────────────────────────────────────────────────────────────────

  # Export prompts for Stable Diffusion
  python generate_images.py --export-sd

  # Includes: prompt, negative prompt, dimensions, steps, CFG, sampler

─────────────────────────────────────────────────────────────────────
OTHER COMMANDS:
─────────────────────────────────────────────────────────────────────

  --list              List all available prompts
  --export            Export all prompts in all formats (JSON)
  --guide             Show this usage guide

─────────────────────────────────────────────────────────────────────
ENVIRONMENT VARIABLES:
─────────────────────────────────────────────────────────────────────

  GOOGLE_API_KEY      Google AI API key (for Gemini)
  OPENAI_API_KEY      OpenAI API key (for DALL-E 3)

─────────────────────────────────────────────────────────────────────
""")


# =============================================================================
# MAIN
# =============================================================================

def main():
    parser = argparse.ArgumentParser(
        description="HDIM Marketing Image Generator - Multi-Platform Support",
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    # Platform selection
    parser.add_argument(
        "--platform",
        choices=["gemini", "dalle"],
        default="gemini",
        help="Generation platform (default: gemini)"
    )
    parser.add_argument(
        "--model",
        help="Model to use (e.g., gemini-2.5-pro, gemini-2.0-flash, imagen-3)"
    )

    # API keys
    parser.add_argument(
        "--api-key",
        help="API key (or use GOOGLE_API_KEY/OPENAI_API_KEY env vars)"
    )

    # Generation commands
    parser.add_argument(
        "--all",
        action="store_true",
        help="Generate all images"
    )
    parser.add_argument(
        "--category",
        choices=["linkedin", "hero", "pitch", "social"],
        help="Generate all images in a category"
    )
    parser.add_argument(
        "--single",
        help="Generate a single image by ID (e.g., AD-01)"
    )

    # Export commands
    parser.add_argument(
        "--export-midjourney",
        action="store_true",
        help="Export prompts formatted for Midjourney"
    )
    parser.add_argument(
        "--export-sd",
        action="store_true",
        help="Export prompts for Stable Diffusion"
    )
    parser.add_argument(
        "--export",
        action="store_true",
        help="Export all prompts in all formats"
    )

    # Info commands
    parser.add_argument(
        "--list",
        action="store_true",
        help="List all available prompts"
    )
    parser.add_argument(
        "--guide",
        action="store_true",
        help="Show comprehensive usage guide"
    )

    args = parser.parse_args()

    # Setup output directories
    setup_output_dirs()

    # Handle info/export commands first (no API needed)
    if args.guide:
        print_usage_guide()
        return

    if args.list:
        list_prompts()
        return

    if args.export_midjourney:
        export_for_midjourney()
        return

    if args.export_sd:
        export_for_stable_diffusion()
        return

    if args.export:
        export_all_prompts()
        export_for_midjourney()
        export_for_stable_diffusion()
        return

    # Get API key based on platform
    if args.platform == "gemini":
        api_key = args.api_key or os.environ.get("GOOGLE_API_KEY")
    elif args.platform == "dalle":
        api_key = args.api_key or os.environ.get("OPENAI_API_KEY")
    else:
        api_key = args.api_key

    # Handle generation commands (require API key)
    if args.all or args.category or args.single:
        if not api_key:
            print(f"Error: No API key provided for {args.platform}")
            print(f"Use --api-key or set {'GOOGLE_API_KEY' if args.platform == 'gemini' else 'OPENAI_API_KEY'}")
            print("\nAlternatively, use export commands to get prompts for manual generation:")
            print("  --export-midjourney  Export for Midjourney")
            print("  --export-sd          Export for Stable Diffusion")
            sys.exit(1)

        if args.all:
            generate_all(args.platform, api_key, args.model)
        elif args.category:
            generate_category(args.category, args.platform, api_key, args.model)
        elif args.single:
            if args.platform == "gemini":
                generate_with_gemini(args.single, api_key, args.model or "gemini-2.5-pro")
            elif args.platform == "dalle":
                generate_with_dalle(args.single, api_key)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()

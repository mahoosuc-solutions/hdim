#!/usr/bin/env python3
"""
Generate Detailed UI Images Using Gemini 3 PRO
Uses the GEMINI_API_KEY to generate high-quality UI mockups
"""

import os
import json
import base64
import requests
from pathlib import Path
from typing import Dict, Optional
from datetime import datetime
import time

# API Key from environment or hardcoded fallback
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY") or os.getenv("GOOGLE_API_KEY") or "AIzaSyBJKY_Hml7wvwxdppZQjET_imtwnAELhck"

# Output directory
OUTPUT_DIR = Path(__file__).parent.parent / "assets" / "generated"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

# Gemini image generation model
# Note: Use image-generation specific models
GEMINI_MODEL = "gemini-2.0-flash-exp-image-generation"  # Image generation model
# Alternative models that may support image generation:
# - "gemini-2.5-flash-image"
# - "gemini-1.5-pro" (multimodal, may not generate images directly)


def generate_image_with_gemini_pro(
    prompt: str,
    width: int = 1920,
    height: int = 1080,
    output_filename: Optional[str] = None
) -> Path:
    """
    Generate a detailed image using Gemini 3 PRO (or latest image generation model)
    
    Args:
        prompt: Detailed prompt describing the image to generate
        width: Image width in pixels
        height: Image height in pixels
        output_filename: Optional output filename (auto-generated if not provided)
    
    Returns:
        Path to the generated image file
    """
    print(f"🎨 Generating image with Gemini PRO...")
    print(f"📝 Prompt: {prompt[:150]}...")
    print(f"📐 Size: {width}x{height}")
    
    # Enhanced prompt for detailed UI generation
    enhanced_prompt = f"""Create a highly detailed, professional UI mockup image.

{prompt}

Technical Requirements:
- Resolution: {width}x{height} pixels
- High quality, professional appearance
- Realistic UI elements, not wireframes
- Clear, readable text and elements
- Modern, clean design aesthetic
- Detailed and precise rendering
- Professional healthcare software appearance

Style Requirements:
- Clean, modern SaaS aesthetic
- Professional color scheme
- Generous whitespace
- Clear typography
- Subtle shadows and depth
- Realistic interface elements

Generate this as a detailed, high-quality image."""
    
    # API endpoint
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{GEMINI_MODEL}:generateContent?key={GEMINI_API_KEY}"
    
    # Request payload - Use responseModalities for image generation
    payload = {
        "contents": [{
            "parts": [{
                "text": f"Generate this image: {enhanced_prompt}"
            }]
        }],
        "generationConfig": {
            "temperature": 0.7,
            "topK": 40,
            "topP": 0.95,
            "maxOutputTokens": 8192,
            "responseModalities": ["image", "text"],
            "responseMimeType": "text/plain"
        }
    }
    
    try:
        print(f"🔄 Calling Gemini API...")
        response = requests.post(
            url,
            json=payload,
            headers={"Content-Type": "application/json"},
            timeout=120  # 2 minute timeout for image generation
        )
        
        if response.status_code != 200:
            error_msg = response.text
            print(f"❌ API Error ({response.status_code}): {error_msg}")
            raise Exception(f"Gemini API error: {error_msg}")
        
        result = response.json()
        
        # Extract image data from response
        image_data = None
        
        if "candidates" in result and result["candidates"]:
            candidate = result["candidates"][0]
            if "content" in candidate and "parts" in candidate["content"]:
                for part in candidate["content"]["parts"]:
                    if "inlineData" in part:
                        image_data = part["inlineData"]["data"]
                        break
        
        if not image_data:
            # Try alternative response format
            if "candidates" in result:
                for candidate in result["candidates"]:
                    if "content" in candidate:
                        content = candidate["content"]
                        if isinstance(content, dict) and "parts" in content:
                            for part in content["parts"]:
                                if isinstance(part, dict) and "inlineData" in part:
                                    image_data = part["inlineData"]["data"]
                                    break
                        elif isinstance(content, str):
                            # Sometimes the image data might be in a different format
                            print(f"⚠️  Unexpected response format: {type(content)}")
        
        if not image_data:
            print(f"❌ No image data found in response")
            print(f"📄 Response: {json.dumps(result, indent=2)[:500]}...")
            raise Exception("No image data found in Gemini API response")
        
        # Decode base64 image data
        try:
            image_bytes = base64.b64decode(image_data)
        except Exception as e:
            print(f"❌ Error decoding image data: {e}")
            raise
        
        # Generate output filename
        if not output_filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            output_filename = f"gemini-pro-{timestamp}.png"
        
        output_path = OUTPUT_DIR / output_filename
        
        # Save image
        with open(output_path, 'wb') as f:
            f.write(image_bytes)
        
        file_size = len(image_bytes)
        print(f"✅ Image generated successfully!")
        print(f"📁 Saved to: {output_path}")
        print(f"📊 File size: {file_size / 1024:.2f} KB")
        
        return output_path
    
    except requests.exceptions.Timeout:
        raise Exception("Request timeout - image generation took too long")
    except requests.exceptions.RequestException as e:
        raise Exception(f"Network error: {str(e)}")
    except Exception as e:
        raise Exception(f"Generation failed: {str(e)}")


def generate_dashboard_images():
    """Generate detailed dashboard UI images"""
    
    dashboards = [
        {
            "name": "clinical-dashboard-light",
            "prompt": """Create a modern healthcare analytics dashboard UI mockup for a clinical care management platform.

LAYOUT:
- Top navigation bar (white background, subtle shadow) with HDIM logo on the left, user menu on the right
- Left sidebar navigation (240px wide, light gray background #F5F7FA) with icons: Dashboard, Patients, Care Gaps, Quality Measures, Analytics, Settings
- Main content area (white background) showing:
  - Summary statistics row at top: 4 cards showing "Total Patients: 12,847", "Open Care Gaps: 2,340", "Closed This Month: +847", "HEDIS Score: 78.4%"
  - Large bar chart showing care gap closure trend over 10 weeks (ascending blue bars, teal accent #00A9A5)
  - Data table below showing priority patients with columns: Name, MRN, Risk Score, Care Gaps, Status
  - Map visualization on right side showing geographic distribution of patients

STYLE:
- Clean, modern SaaS aesthetic (similar to Stripe, Linear, or Vercel dashboards)
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Background: White #FFFFFF with subtle gray cards #F5F7FA
- Typography: Clean sans-serif (Inter or similar), clear hierarchy
- Generous whitespace, minimal borders
- Subtle shadows on cards for depth
- Professional, trustworthy, modern

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- No text artifacts or distortions
- Realistic UI elements, not wireframes
- Professional healthcare software appearance""",
            "width": 1920,
            "height": 1080
        },
        {
            "name": "clinical-dashboard-dark",
            "prompt": """Create a modern healthcare analytics dashboard UI mockup in dark mode for a clinical care management platform.

LAYOUT:
- Top navigation bar (dark gray background #1A1A1A, subtle border) with HDIM logo on the left, user menu on the right
- Left sidebar navigation (240px wide, dark background #2C2C2C) with icons: Dashboard, Patients, Care Gaps, Quality Measures, Analytics, Settings
- Main content area (dark background #1A1A1A) showing:
  - Summary statistics row at top: 4 cards showing "Total Patients: 12,847", "Open Care Gaps: 2,340", "Closed This Month: +847", "HEDIS Score: 78.4%" (cards with dark gray background #2C2C2C)
  - Large bar chart showing care gap closure trend over 10 weeks (ascending blue bars, teal accent #00A9A5, dark background)
  - Data table below showing priority patients with columns: Name, MRN, Risk Score, Care Gaps, Status (dark rows with hover effects)
  - Map visualization on right side showing geographic distribution (dark theme)

STYLE:
- Clean, modern dark mode SaaS aesthetic (similar to modern dark mode dashboards like GitHub Dark, Linear Dark, or Vercel Dark)
- Primary color: Light Blue #2C5282 (for highlights)
- Accent color: Teal #00A9A5
- Background: Dark #1A1A1A with dark gray cards #2C2C2C
- Text: Light gray #E0E0E0 for readability
- Typography: Clean sans-serif, clear hierarchy
- Generous whitespace, subtle borders
- Professional, modern, easy on the eyes

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- No text artifacts
- Realistic dark mode UI, not wireframe
- Professional healthcare software appearance

AVOID:
- Too dark (unreadable)
- Harsh contrasts
- Generic dark themes
- Text rendering issues
- Unrealistic dark mode interfaces""",
            "width": 1920,
            "height": 1080
        },
        {
            "name": "admin-dashboard",
            "prompt": """Create a modern healthcare system administration dashboard UI mockup.

LAYOUT:
- Top navigation bar with HDIM logo, notifications icon, user menu
- Left sidebar with admin navigation: Dashboard, Users, Roles, Audit Logs, Integrations, System Health, Settings
- Main content area showing:
  - System health overview: 6 service status cards (all green/healthy) showing service names and uptime percentages (e.g., "99.9% uptime", "2.3ms avg response time")
  - Recent activity feed showing audit events, user actions, system events with timestamps
  - Integration status panel showing connected systems (Epic, Cerner, AllScripts) with connection status indicators (green/yellow/red)
  - Performance metrics panel: API response times, event throughput (10,000 events/sec), database performance with line charts and bar charts

STYLE:
- Clean, modern admin interface (similar to modern SaaS admin panels like Stripe Dashboard, AWS Console, or Datadog)
- Primary color: Deep Blue #1E3A5F
- Accent color: Teal #00A9A5
- Status colors: Green (#2E7D32) for healthy, Yellow (#E65100) for warning, Red (#C62828) for error
- White background (#FFFFFF) with subtle gray cards (#F5F7FA)
- Clear typography, data-focused design
- Professional, technical, trustworthy
- Realistic admin interface, not wireframe

TECHNICAL:
- Resolution: 1920x1080 pixels
- Format: High-quality PNG
- Realistic admin dashboard appearance
- Clear status indicators
- Professional healthcare software

AVOID:
- Cluttered interface
- Unclear status indicators
- Generic admin panels
- Wireframe appearance
- Unrealistic data displays""",
            "width": 1920,
            "height": 1080
        }
    ]
    
    results = []
    
    for dashboard in dashboards:
        try:
            print(f"\n{'='*60}")
            print(f"Generating: {dashboard['name']}")
            print(f"{'='*60}")
            
            output_path = generate_image_with_gemini_pro(
                prompt=dashboard['prompt'],
                width=dashboard['width'],
                height=dashboard['height'],
                output_filename=f"{dashboard['name']}.png"
            )
            
            results.append({
                "name": dashboard['name'],
                "status": "success",
                "path": str(output_path)
            })
            
            # Rate limiting - wait between requests
            time.sleep(2)
        
        except Exception as e:
            print(f"❌ Error generating {dashboard['name']}: {str(e)}")
            results.append({
                "name": dashboard['name'],
                "status": "error",
                "error": str(e)
            })
    
    return results


def main():
    """Main function"""
    print("="*60)
    print("Gemini 3 PRO Image Generation Script")
    print("="*60)
    print(f"API Key: {GEMINI_API_KEY[:10]}...{GEMINI_API_KEY[-4:]}")
    print(f"Model: {GEMINI_MODEL}")
    print(f"Output Directory: {OUTPUT_DIR}")
    print("="*60)
    
    # Check API key
    if not GEMINI_API_KEY or GEMINI_API_KEY == "your-api-key":
        print("❌ Error: GEMINI_API_KEY not set")
        print("Set it with: export GEMINI_API_KEY='your-api-key'")
        return
    
    # Generate dashboard images
    print("\n🚀 Starting batch generation...")
    results = generate_dashboard_images()
    
    # Summary
    print("\n" + "="*60)
    print("Generation Summary")
    print("="*60)
    successful = [r for r in results if r['status'] == 'success']
    failed = [r for r in results if r['status'] == 'error']
    
    print(f"✅ Successful: {len(successful)}")
    for result in successful:
        print(f"   - {result['name']}: {result['path']}")
    
    if failed:
        print(f"\n❌ Failed: {len(failed)}")
        for result in failed:
            print(f"   - {result['name']}: {result.get('error', 'Unknown error')}")
    
    print(f"\n📁 All images saved to: {OUTPUT_DIR}")


if __name__ == "__main__":
    main()

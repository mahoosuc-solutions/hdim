#!/usr/bin/env python3
"""Enhanced Image Generation with Quality Control.

This script wraps the existing Gemini image generation with:
1. Automated quality verification (OCR, clarity, technical)
2. Database persistence for iteration tracking
3. Integration with the review portal

Usage:
    # Generate a single asset
    python generate_with_qc.py --asset HERO-01

    # Generate all assets in a category
    python generate_with_qc.py --category hero

    # Regenerate with feedback
    python generate_with_qc.py --asset HERO-01 --feedback "Make text larger"

    # Generate all high-priority assets
    python generate_with_qc.py --priority
"""

import argparse
import os
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Optional

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent))

from config import (
    GOOGLE_API_KEY,
    GEMINI_IMAGE_MODEL,
    OUTPUT_DIR,
    RATE_LIMIT_SECONDS,
    ASSET_PRIORITY,
    IMAGE_SPECS,
    REVIEW_PORTAL_HOST,
    REVIEW_PORTAL_PORT,
    get_asset_category,
    get_asset_spec,
)
from models import GenerationResult, QualityReport
from qc_engine import QualityController
from orchestrator import Orchestrator


# Asset prompts - these would ideally come from VISUAL_ASSET_PROMPTS.md
# For now, define the core prompts here
ASSET_PROMPTS = {
    "HERO-01": """Create a professional healthcare technology marketing hero image for desktop (1920x1080).

Scene: A warm, authentic moment showing a middle-aged woman (50s) looking at her phone with a smile, sitting in a bright living room. In the background, a grandmother figure interacts with a young child.

Key elements:
- Natural, warm lighting suggesting hope and connection
- Modern, clean aesthetic appropriate for healthcare technology
- Colors: Deep blue (#0066CC) and warm teal (#00A5B5) accents
- No visible text or logos in the image
- Photorealistic style, professional quality
- Evokes feeling of "healthcare that cares about families"

Style: Modern stock photography, warm and inviting, healthcare appropriate.""",

    "HERO-02": """Create a professional healthcare technology marketing hero image for mobile (750x1334).

Scene: Close-up portrait of a caring healthcare professional (nurse or care coordinator) with a warm, genuine smile, looking at camera. Soft, professional lighting.

Key elements:
- Vertical composition optimized for mobile
- Professional medical attire (scrubs or lab coat)
- Warm, approachable expression
- Subtle healthcare environment in background
- Colors: Deep blue (#0066CC) and warm teal (#00A5B5) accents
- No visible text or logos
- Photorealistic, professional quality

Style: Modern healthcare photography, trustworthy and caring.""",

    "HERO-03": """Create a social media share image for healthcare technology (1200x630).

Scene: Abstract representation of connected healthcare - stylized nodes/dots connected by lines forming a gentle network, with subtle healthcare symbols (heart, cross) integrated.

Key elements:
- Clean, modern design suitable for LinkedIn/Facebook sharing
- Colors: Deep blue (#0066CC) background with warm teal (#00A5B5) accents
- Leave space in center-left for text overlay
- Professional and trustworthy feel
- No text in the image itself
- Technology meets healthcare aesthetic

Style: Modern digital illustration, corporate professional.""",

    "PORTRAIT-MARIA": """Create a portrait for a patient story - Maria, diabetes patient.

Subject: Hispanic woman in her late 50s, warm smile, looking hopeful and healthy.

Setting: Bright kitchen background suggesting healthy lifestyle, natural morning light.

Key elements:
- Authentic, relatable appearance (not a model)
- Wearing casual, comfortable clothing
- Expression conveys resilience and optimism
- Subtle evidence of healthy choices (fruit bowl, water glass in background)
- Natural lighting, warm tones
- Photorealistic, documentary style

Style: Editorial portrait photography, authentic and inspiring.""",

    "PORTRAIT-JAMES": """Create a portrait for a patient story - James, depression survivor.

Subject: African American man in his 40s, gentle confident smile, conveying recovery and hope.

Setting: Outdoor setting - park or garden with soft natural light, suggesting peaceful recovery.

Key elements:
- Authentic, relatable appearance
- Casual comfortable clothing
- Expression shows quiet strength and peace
- Connection to nature/outdoors
- Warm, healing color palette
- Photorealistic, documentary style

Style: Editorial portrait photography, authentic and inspiring.""",

    "PORTRAIT-SARAH": """Create a portrait for a patient story - Sarah, cancer survivor.

Subject: Woman in her 60s with short gray hair (suggesting post-treatment), radiant healthy smile.

Setting: Sitting in a comfortable home environment with family photos visible in background.

Key elements:
- Authentic, relatable appearance
- Comfortable casual clothing in warm colors
- Expression conveys gratitude and vitality
- Surrounded by elements suggesting life and family
- Soft, warm lighting
- Photorealistic, documentary style

Style: Editorial portrait photography, authentic and inspiring.""",

    "BADGE-FHIR": """Create a trust badge icon for FHIR R4 certification (200x200).

Design: Clean, modern badge/shield design with "FHIR R4" incorporated.

Key elements:
- Deep blue (#0066CC) as primary color
- Clean geometric design
- Professional certification badge style
- The letters "FHIR" clearly visible
- Simple, scalable for various sizes
- White or light background

Style: Modern icon design, professional certification badge.""",

    "BADGE-CQL": """Create a trust badge icon for CQL standards (200x200).

Design: Clean, modern badge/shield design representing clinical query language.

Key elements:
- Deep blue (#0066CC) as primary color
- Incorporate subtle code/query symbol
- The letters "CQL" clearly visible
- Professional certification badge style
- Simple, scalable design
- White or light background

Style: Modern icon design, technical standards badge.""",

    "BADGE-HIPAA": """Create a trust badge icon for HIPAA compliance (200x200).

Design: Clean security-focused badge design for healthcare data protection.

Key elements:
- Deep blue (#0066CC) as primary color
- Incorporate shield or lock symbol
- "HIPAA" text clearly visible
- Professional compliance badge style
- Trust and security focused
- White or light background

Style: Modern icon design, security/compliance badge.""",

    "BADGE-TESTS": """Create a trust badge icon for test coverage (200x200).

Design: Clean badge showing quality/testing metrics.

Key elements:
- Teal (#00A5B5) as primary color
- Incorporate checkmark or quality symbol
- "11,000+" number visible if possible
- Professional quality badge style
- Conveys thoroughness and reliability
- White or light background

Style: Modern icon design, quality metrics badge.""",

    "BADGE-UPTIME": """Create a trust badge icon for 99.9% uptime (200x200).

Design: Clean badge representing reliability and availability.

Key elements:
- Green/teal accent color for reliability
- Incorporate upward arrow or stability symbol
- "99.9%" visible if possible
- Professional reliability badge style
- Conveys stability and dependability
- White or light background

Style: Modern icon design, reliability/uptime badge.""",
}


class GenerationWithQC:
    """Enhanced image generation with quality control integration."""

    def __init__(self):
        """Initialize the generator with QC and orchestration."""
        self.api_key = GOOGLE_API_KEY or os.environ.get("GOOGLE_API_KEY", "")
        if not self.api_key:
            raise ValueError("GOOGLE_API_KEY environment variable required")

        self.qc = QualityController(self.api_key)
        self.orchestrator = Orchestrator()
        self._client = None

    @property
    def client(self):
        """Lazy-load the Google GenAI client."""
        if self._client is None:
            from google import genai
            self._client = genai.Client(api_key=self.api_key)
        return self._client

    def _get_aspect_ratio(self, width: int, height: int) -> str:
        """Determine the closest supported aspect ratio for Imagen.

        Supported ratios: 1:1, 16:9, 9:16, 4:3, 3:4
        """
        ratio = width / height

        # Define supported ratios and their string representations
        supported = [
            (1.0, "1:1"),       # 1:1 = 1.0
            (16/9, "16:9"),     # 16:9 = 1.78
            (9/16, "9:16"),     # 9:16 = 0.56
            (4/3, "4:3"),       # 4:3 = 1.33
            (3/4, "3:4"),       # 3:4 = 0.75
        ]

        # Find closest match
        closest = min(supported, key=lambda x: abs(x[0] - ratio))
        return closest[1]

    def generate_asset(
        self,
        asset_id: str,
        custom_prompt: Optional[str] = None,
        feedback: Optional[str] = None,
    ) -> GenerationResult:
        """Generate an image asset with quality control.

        Args:
            asset_id: The asset identifier (e.g., "HERO-01")
            custom_prompt: Override the default prompt
            feedback: Feedback for prompt refinement (triggers new version)

        Returns:
            GenerationResult with image path and QC report
        """
        start_time = time.time()

        # Determine version number
        if feedback:
            # Create refined prompt and get new version
            version = self.orchestrator.create_refined_prompt(asset_id, feedback)
            prompt = self.orchestrator.get_prompt(asset_id)
        else:
            version = self.orchestrator.get_next_version(asset_id)
            if custom_prompt:
                prompt = custom_prompt
            else:
                # Get prompt from orchestrator or defaults
                prompt = self.orchestrator.get_prompt(asset_id)
                if not prompt:
                    prompt = ASSET_PROMPTS.get(asset_id)
                    if prompt:
                        self.orchestrator.set_base_prompt(asset_id, prompt)
                    else:
                        return GenerationResult(
                            success=False,
                            asset_id=asset_id,
                            error=f"No prompt found for asset {asset_id}"
                        )

        print(f"\n{'='*60}")
        print(f"Generating {asset_id} version {version}")
        print(f"{'='*60}")

        # Generate image
        try:
            print("Calling Imagen API...")
            from google.genai import types

            # Determine aspect ratio from asset specs
            spec = get_asset_spec(asset_id)
            width = spec.get('width', 1920)
            height = spec.get('height', 1080)
            aspect_ratio = self._get_aspect_ratio(width, height)
            print(f"Using aspect ratio: {aspect_ratio} (for {width}x{height})")

            # Use Imagen 4 for image generation
            response = self.client.models.generate_images(
                model="imagen-4.0-generate-001",
                prompt=prompt,
                config=types.GenerateImagesConfig(
                    number_of_images=1,
                    aspect_ratio=aspect_ratio,
                    safety_filter_level="BLOCK_LOW_AND_ABOVE",
                )
            )

            # Check for generated images
            if not response.generated_images:
                return GenerationResult(
                    success=False,
                    asset_id=asset_id,
                    version=version,
                    error="No image generated - empty response"
                )

            # Get the first generated image
            generated_image = response.generated_images[0]
            image_data = generated_image.image.image_bytes

            if not image_data:
                return GenerationResult(
                    success=False,
                    asset_id=asset_id,
                    version=version,
                    error="No image data in response"
                )

            # Save image
            category = get_asset_category(asset_id)
            output_dir = OUTPUT_DIR / category
            output_dir.mkdir(parents=True, exist_ok=True)

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"{asset_id}_{timestamp}_v{version}.png"
            image_path = output_dir / filename

            with open(image_path, 'wb') as f:
                f.write(image_data)

            print(f"SUCCESS: Saved to {image_path}")

        except Exception as e:
            return GenerationResult(
                success=False,
                asset_id=asset_id,
                version=version,
                error=f"Generation failed: {e}"
            )

        # Run quality verification
        print("\nRunning quality verification...")
        qc_report = self.qc.verify_image(image_path, asset_id, version)

        # Save to database
        self.orchestrator.save_iteration(
            asset_id=asset_id,
            version=version,
            filepath=image_path,
            prompt=prompt,
            qc_report=qc_report,
        )

        # Calculate generation time
        generation_time = (time.time() - start_time) * 1000

        # Display results
        self._print_quality_report(qc_report)

        print(f"\nSaved to database: version {version}")
        print(f"Review at: http://{REVIEW_PORTAL_HOST}:{REVIEW_PORTAL_PORT}/review/{asset_id}")

        return GenerationResult(
            success=True,
            asset_id=asset_id,
            version=version,
            image_path=image_path,
            qc_report=qc_report,
            generation_time_ms=generation_time,
        )

    def _print_quality_report(self, report: QualityReport) -> None:
        """Print formatted quality report to console."""
        print("\nQuality Report:")
        print("-" * 40)

        if report.text_accuracy:
            status_icon = "✓" if report.text_accuracy.status == "PASS" else "✗"
            print(f"  Text Accuracy: {status_icon} {report.text_accuracy.status}")
            if report.text_accuracy.extracted_text:
                print(f"    Extracted: {report.text_accuracy.extracted_text[:3]}...")
            if report.text_accuracy.misspelled:
                print(f"    Misspelled: {', '.join(report.text_accuracy.misspelled)}")

        if report.clarity:
            score = report.clarity.score
            score_icon = "✓" if score >= 75 else "⚠" if score >= 50 else "✗"
            print(f"  Clarity Score: {score_icon} {score:.0f}/100")
            if report.clarity.issues:
                for issue in report.clarity.issues[:2]:
                    print(f"    Issue: {issue}")

        if report.technical:
            status_icon = "✓" if report.technical.status == "PASS" else "⚠" if report.technical.status == "WARNING" else "✗"
            print(f"  Technical: {status_icon} {report.technical.status}")
            print(f"    Dimensions: {report.technical.dimensions}")
            print(f"    Size: {report.technical.file_size_kb:.0f} KB")

        print("-" * 40)
        overall_icon = "✓" if report.overall_status == "READY_FOR_REVIEW" else "✗"
        print(f"  Overall: {overall_icon} {report.overall_status} ({report.overall_score:.0f}/100)")

    def generate_category(self, category: str) -> list:
        """Generate all assets in a category.

        Args:
            category: Category name (e.g., "hero", "portraits")

        Returns:
            List of GenerationResult for each asset
        """
        if category not in IMAGE_SPECS:
            print(f"Unknown category: {category}")
            print(f"Available: {', '.join(IMAGE_SPECS.keys())}")
            return []

        results = []
        assets = IMAGE_SPECS[category]

        for asset_id in assets.keys():
            result = self.generate_asset(asset_id)
            results.append(result)

            # Rate limiting
            if len(results) < len(assets):
                print(f"\nWaiting {RATE_LIMIT_SECONDS}s for rate limit...")
                time.sleep(RATE_LIMIT_SECONDS)

        return results

    def generate_priority(self) -> list:
        """Generate all high-priority assets.

        Returns:
            List of GenerationResult for each priority asset
        """
        results = []

        for i, asset_id in enumerate(ASSET_PRIORITY):
            result = self.generate_asset(asset_id)
            results.append(result)

            # Rate limiting
            if i < len(ASSET_PRIORITY) - 1:
                print(f"\nWaiting {RATE_LIMIT_SECONDS}s for rate limit...")
                time.sleep(RATE_LIMIT_SECONDS)

        return results


def main():
    """Main entry point for CLI."""
    parser = argparse.ArgumentParser(
        description="Generate images with quality control",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python generate_with_qc.py --asset HERO-01
  python generate_with_qc.py --category hero
  python generate_with_qc.py --asset HERO-01 --feedback "Make text larger"
  python generate_with_qc.py --priority
        """
    )

    parser.add_argument(
        "--asset",
        help="Generate a specific asset (e.g., HERO-01)"
    )
    parser.add_argument(
        "--category",
        help="Generate all assets in a category (e.g., hero, portraits)"
    )
    parser.add_argument(
        "--priority",
        action="store_true",
        help="Generate all high-priority assets"
    )
    parser.add_argument(
        "--feedback",
        help="Feedback for prompt refinement (creates new version)"
    )
    parser.add_argument(
        "--prompt",
        help="Custom prompt to use instead of default"
    )
    parser.add_argument(
        "--list",
        action="store_true",
        help="List available assets and categories"
    )

    args = parser.parse_args()

    # Handle list command
    if args.list:
        print("\nAvailable Categories and Assets:")
        print("=" * 40)
        for category, assets in IMAGE_SPECS.items():
            print(f"\n{category.upper()}:")
            for asset_id, spec in assets.items():
                print(f"  {asset_id}: {spec.get('name', 'No name')}")
        print(f"\nPriority Assets ({len(ASSET_PRIORITY)}):")
        for asset_id in ASSET_PRIORITY:
            print(f"  {asset_id}")
        return

    # Validate arguments
    if not any([args.asset, args.category, args.priority]):
        parser.print_help()
        print("\nError: Specify --asset, --category, or --priority")
        sys.exit(1)

    # Initialize generator
    try:
        generator = GenerationWithQC()
    except ValueError as e:
        print(f"Error: {e}")
        print("Set GOOGLE_API_KEY environment variable")
        sys.exit(1)

    # Execute generation
    if args.asset:
        result = generator.generate_asset(
            args.asset,
            custom_prompt=args.prompt,
            feedback=args.feedback
        )
        if not result.success:
            print(f"\nGeneration failed: {result.error}")
            sys.exit(1)

    elif args.category:
        results = generator.generate_category(args.category)
        success_count = sum(1 for r in results if r.success)
        print(f"\n{'='*60}")
        print(f"Category '{args.category}' complete: {success_count}/{len(results)} successful")

    elif args.priority:
        results = generator.generate_priority()
        success_count = sum(1 for r in results if r.success)
        print(f"\n{'='*60}")
        print(f"Priority generation complete: {success_count}/{len(results)} successful")

    print(f"\nView results at: http://{REVIEW_PORTAL_HOST}:{REVIEW_PORTAL_PORT}/")


if __name__ == "__main__":
    main()

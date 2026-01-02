#!/usr/bin/env python3
"""V2 Image Generation - Research-Informed Healthcare IT Visuals.

This script generates visual assets using the V2 prompts that are informed by:
- Real healthcare IT integration patterns (Epic, Cerner, Meditech, athenahealth)
- CMS regulatory requirements and compliance timelines
- Healthcare persona workflows and pain points from stakeholder interviews
- Visual examples from actual healthcare IT implementations

Usage:
    # Generate all Tier 1 (highest priority) assets
    python generate_v2.py --tier 1

    # Generate a specific asset
    python generate_v2.py --asset HERO-V2-MAIN

    # Generate all V2 assets
    python generate_v2.py --all

    # List available V2 assets
    python generate_v2.py --list
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
    OUTPUT_DIR,
    RATE_LIMIT_SECONDS,
    REVIEW_PORTAL_HOST,
    REVIEW_PORTAL_PORT,
)
from models import GenerationResult, QualityReport
from qc_engine import QualityController
from orchestrator import Orchestrator

# Import V2 prompts
from prompts_v2 import (
    ALL_V2_PROMPTS,
    V2_IMAGE_SPECS,
    V2_PRIORITY,
    get_v2_prompt,
    get_v2_asset_category,
    get_v2_asset_spec,
    get_tier1_assets,
    get_tier2_assets,
    get_tier3_assets,
)


class V2Generator:
    """Enhanced image generation with V2 research-informed prompts."""

    def __init__(self):
        """Initialize the V2 generator."""
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

        supported = [
            (1.0, "1:1"),       # 1:1 = 1.0
            (16/9, "16:9"),     # 16:9 = 1.78
            (9/16, "9:16"),     # 9:16 = 0.56
            (4/3, "4:3"),       # 4:3 = 1.33
            (3/4, "3:4"),       # 3:4 = 0.75
        ]

        closest = min(supported, key=lambda x: abs(x[0] - ratio))
        return closest[1]

    def generate_asset(
        self,
        asset_id: str,
        custom_prompt: Optional[str] = None,
    ) -> GenerationResult:
        """Generate a V2 image asset with quality control.

        Args:
            asset_id: The V2 asset identifier (e.g., "HERO-V2-MAIN")
            custom_prompt: Override the V2 prompt

        Returns:
            GenerationResult with image path and QC report
        """
        start_time = time.time()

        # Get prompt
        prompt = custom_prompt or get_v2_prompt(asset_id)
        if not prompt:
            return GenerationResult(
                success=False,
                asset_id=asset_id,
                error=f"No V2 prompt found for asset {asset_id}"
            )

        # Get version number
        version = self.orchestrator.get_next_version(asset_id)

        # Store prompt if first version
        if version == 1:
            self.orchestrator.set_base_prompt(asset_id, prompt)

        print(f"\n{'='*60}")
        print(f"🚀 Generating V2 Asset: {asset_id} (version {version})")
        print(f"{'='*60}")

        # Get asset specifications
        spec = get_v2_asset_spec(asset_id)
        width = spec.get('width', 1920)
        height = spec.get('height', 1080)
        aspect_ratio = self._get_aspect_ratio(width, height)

        print(f"📐 Dimensions: {width}x{height} (aspect ratio: {aspect_ratio})")
        print(f"📝 Using V2 research-informed prompt")

        try:
            print("\n📡 Calling Imagen 4.0 API...")
            from google.genai import types

            response = self.client.models.generate_images(
                model="imagen-4.0-generate-001",
                prompt=prompt,
                config=types.GenerateImagesConfig(
                    number_of_images=1,
                    aspect_ratio=aspect_ratio,
                    safety_filter_level="BLOCK_LOW_AND_ABOVE",
                )
            )

            if not response.generated_images:
                return GenerationResult(
                    success=False,
                    asset_id=asset_id,
                    version=version,
                    error="No image generated - empty response"
                )

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
            category = get_v2_asset_category(asset_id)
            output_dir = OUTPUT_DIR / category
            output_dir.mkdir(parents=True, exist_ok=True)

            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"{asset_id}_{timestamp}_v{version}.png"
            image_path = output_dir / filename

            with open(image_path, 'wb') as f:
                f.write(image_data)

            print(f"✅ SUCCESS: Saved to {image_path}")

        except Exception as e:
            return GenerationResult(
                success=False,
                asset_id=asset_id,
                version=version,
                error=f"Generation failed: {e}"
            )

        # Run quality verification
        print("\n🔍 Running quality verification...")
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

        print(f"\n💾 Saved to database: version {version}")
        print(f"🌐 Review at: http://{REVIEW_PORTAL_HOST}:{REVIEW_PORTAL_PORT}/review/{asset_id}")

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
        print("\n📊 Quality Report:")
        print("-" * 40)

        if report.text_accuracy:
            status_icon = "✅" if report.text_accuracy.status == "PASS" else "❌"
            print(f"  Text Accuracy: {status_icon} {report.text_accuracy.status}")
            if report.text_accuracy.extracted_text:
                print(f"    Extracted: {report.text_accuracy.extracted_text[:3]}...")
            if report.text_accuracy.misspelled:
                print(f"    Misspelled: {', '.join(report.text_accuracy.misspelled)}")

        if report.clarity:
            score = report.clarity.score
            score_icon = "✅" if score >= 75 else "⚠️" if score >= 50 else "❌"
            print(f"  Clarity Score: {score_icon} {score:.0f}/100")
            if report.clarity.issues:
                for issue in report.clarity.issues[:2]:
                    print(f"    Issue: {issue}")

        if report.technical:
            status_icon = "✅" if report.technical.status == "PASS" else "⚠️" if report.technical.status == "WARNING" else "❌"
            print(f"  Technical: {status_icon} {report.technical.status}")
            print(f"    Dimensions: {report.technical.dimensions}")
            print(f"    Size: {report.technical.file_size_kb:.0f} KB")

        print("-" * 40)
        overall_icon = "✅" if report.overall_status == "READY_FOR_REVIEW" else "⚠️"
        print(f"  Overall: {overall_icon} {report.overall_status} ({report.overall_score:.0f}/100)")

    def generate_tier(self, tier: int) -> list:
        """Generate all assets in a specific tier.

        Args:
            tier: 1, 2, or 3

        Returns:
            List of GenerationResult for each asset
        """
        if tier == 1:
            assets = get_tier1_assets()
            tier_name = "Tier 1 (Highest Priority)"
        elif tier == 2:
            assets = get_tier2_assets()
            tier_name = "Tier 2 (Secondary Priority)"
        elif tier == 3:
            assets = get_tier3_assets()
            tier_name = "Tier 3 (Supporting)"
        else:
            print(f"❌ Invalid tier: {tier}. Must be 1, 2, or 3.")
            return []

        print(f"\n{'='*60}")
        print(f"📦 Generating {tier_name} Assets ({len(assets)} total)")
        print(f"{'='*60}")

        results = []
        for i, asset_id in enumerate(assets, 1):
            print(f"\n[{i}/{len(assets)}] Generating {asset_id}...")
            result = self.generate_asset(asset_id)
            results.append(result)

            # Rate limiting between generations
            if i < len(assets):
                print(f"\n⏳ Rate limiting: waiting {RATE_LIMIT_SECONDS}s before next generation...")
                time.sleep(RATE_LIMIT_SECONDS)

        # Summary
        successful = sum(1 for r in results if r.success)
        print(f"\n{'='*60}")
        print(f"📊 {tier_name} Generation Complete")
        print(f"   ✅ Successful: {successful}/{len(assets)}")
        print(f"   ❌ Failed: {len(assets) - successful}/{len(assets)}")
        print(f"{'='*60}")

        return results

    def generate_all(self) -> list:
        """Generate all V2 assets in priority order.

        Returns:
            List of GenerationResult for all assets
        """
        print(f"\n{'='*60}")
        print(f"🚀 Generating ALL V2 Assets ({len(V2_PRIORITY)} total)")
        print(f"{'='*60}")

        all_results = []

        for tier in [1, 2, 3]:
            results = self.generate_tier(tier)
            all_results.extend(results)

        # Final summary
        successful = sum(1 for r in all_results if r.success)
        print(f"\n{'='*60}")
        print(f"🏁 ALL V2 GENERATION COMPLETE")
        print(f"   ✅ Total Successful: {successful}/{len(V2_PRIORITY)}")
        print(f"   ❌ Total Failed: {len(V2_PRIORITY) - successful}/{len(V2_PRIORITY)}")
        print(f"\n📱 Review all assets at: http://{REVIEW_PORTAL_HOST}:{REVIEW_PORTAL_PORT}/")
        print(f"{'='*60}")

        return all_results


def list_v2_assets():
    """Print all available V2 assets organized by tier and category."""
    print("\n📋 V2 Research-Informed Visual Assets")
    print("=" * 60)

    print("\n🥇 TIER 1 - Highest Priority (Week 1)")
    print("-" * 40)
    for asset_id in get_tier1_assets():
        spec = get_v2_asset_spec(asset_id)
        name = spec.get('name', asset_id)
        dims = f"{spec.get('width', '?')}x{spec.get('height', '?')}"
        print(f"  • {asset_id}: {name} ({dims})")

    print("\n🥈 TIER 2 - Secondary Priority (Week 2)")
    print("-" * 40)
    for asset_id in get_tier2_assets():
        spec = get_v2_asset_spec(asset_id)
        name = spec.get('name', asset_id)
        dims = f"{spec.get('width', '?')}x{spec.get('height', '?')}"
        print(f"  • {asset_id}: {name} ({dims})")

    print("\n🥉 TIER 3 - Supporting Assets (Weeks 3-4)")
    print("-" * 40)
    for asset_id in get_tier3_assets():
        spec = get_v2_asset_spec(asset_id)
        name = spec.get('name', asset_id)
        dims = f"{spec.get('width', '?')}x{spec.get('height', '?')}"
        print(f"  • {asset_id}: {name} ({dims})")

    print(f"\n📊 Total V2 Assets: {len(V2_PRIORITY)}")
    print("=" * 60)


def main():
    """Command-line interface for V2 generation."""
    parser = argparse.ArgumentParser(
        description="Generate V2 research-informed healthcare visual assets"
    )

    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument(
        "--asset", "-a",
        type=str,
        help="Generate a specific asset (e.g., HERO-V2-MAIN)"
    )
    group.add_argument(
        "--tier", "-t",
        type=int,
        choices=[1, 2, 3],
        help="Generate all assets in tier (1=highest priority, 3=supporting)"
    )
    group.add_argument(
        "--all",
        action="store_true",
        help="Generate all V2 assets"
    )
    group.add_argument(
        "--list", "-l",
        action="store_true",
        help="List all available V2 assets"
    )

    args = parser.parse_args()

    if args.list:
        list_v2_assets()
        return

    # Check API key
    api_key = GOOGLE_API_KEY or os.environ.get("GOOGLE_API_KEY", "")
    if not api_key:
        print("❌ Error: GOOGLE_API_KEY environment variable not set")
        print("   Set it with: export GOOGLE_API_KEY='your-api-key'")
        sys.exit(1)

    generator = V2Generator()

    if args.asset:
        if args.asset not in ALL_V2_PROMPTS:
            print(f"❌ Unknown asset: {args.asset}")
            print("   Run with --list to see available assets")
            sys.exit(1)
        generator.generate_asset(args.asset)
    elif args.tier:
        generator.generate_tier(args.tier)
    elif args.all:
        generator.generate_all()


if __name__ == "__main__":
    main()

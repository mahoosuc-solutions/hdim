"""Quality Control Engine for AI Image Generation System.

This module provides automated quality verification for generated images:
1. Text Accuracy - OCR extraction and spell-checking
2. Clarity Assessment - Vision AI evaluation of image quality
3. Technical Validation - Dimensions, file size, format checks
"""

import os
import re
from pathlib import Path
from typing import Optional

from PIL import Image

from models import (
    QualityReport,
    TextAccuracyResult,
    ClarityResult,
    TechnicalValidationResult,
)
from config import (
    GOOGLE_API_KEY,
    GEMINI_VISION_MODEL,
    MIN_CLARITY_SCORE,
    MAX_FILE_SIZE_KB,
    CUSTOM_DICTIONARY_WORDS,
    get_asset_spec,
)


class QualityController:
    """Automated quality verification for generated images."""

    def __init__(self, api_key: Optional[str] = None):
        """Initialize the Quality Controller.

        Args:
            api_key: Google API key for Gemini. Falls back to environment variable.
        """
        self.api_key = api_key or GOOGLE_API_KEY or os.environ.get("GOOGLE_API_KEY", "")
        self._vision_model = None
        self._custom_words = set(word.lower() for word in CUSTOM_DICTIONARY_WORDS)

    @property
    def vision_model(self):
        """Lazy-load the Gemini vision model."""
        if self._vision_model is None:
            try:
                import google.generativeai as genai
                genai.configure(api_key=self.api_key)
                self._vision_model = genai.GenerativeModel(GEMINI_VISION_MODEL)
            except ImportError:
                raise ImportError("google-generativeai package is required. Install with: pip install google-generativeai")
            except Exception as e:
                raise RuntimeError(f"Failed to initialize Gemini model: {e}")
        return self._vision_model

    def verify_image(
        self,
        image_path: Path,
        asset_id: str,
        version: int = 1,
    ) -> QualityReport:
        """Run all quality checks on a generated image.

        Args:
            image_path: Path to the image file
            asset_id: The asset identifier (e.g., "HERO-01")
            version: Version number of this generation

        Returns:
            QualityReport with all verification results
        """
        report = QualityReport(asset_id=asset_id, version=version)

        # 1. Text Accuracy Check
        try:
            report.text_accuracy = self._check_text_accuracy(image_path)
        except Exception as e:
            report.text_accuracy = TextAccuracyResult(
                status="ERROR",
                error=str(e)
            )

        # 2. Clarity Assessment
        try:
            report.clarity = self._assess_clarity(image_path)
        except Exception as e:
            report.clarity = ClarityResult(
                score=0,
                error=str(e)
            )

        # 3. Technical Validation
        try:
            report.technical = self._validate_technical(image_path, asset_id)
        except Exception as e:
            report.technical = TechnicalValidationResult(
                status="ERROR",
                errors=[str(e)]
            )

        # Calculate overall score and determine status
        report.calculate_overall_score()
        report.determine_status()

        return report

    def _check_text_accuracy(self, image_path: Path) -> TextAccuracyResult:
        """Extract text via OCR and spell-check.

        Uses Gemini Vision to extract all visible text from the image,
        then spell-checks each word against a dictionary plus custom terms.
        """
        # Load image for Gemini
        pil_image = Image.open(image_path)

        # OCR prompt for text extraction
        ocr_prompt = """Analyze this image and extract ALL visible text.

Instructions:
1. Find every piece of text visible in the image
2. Include text on buttons, labels, headings, captions, etc.
3. Return each distinct text item on a new line
4. Preserve exact spelling as shown in the image
5. If no text is visible, respond with: NO_TEXT_FOUND

Return ONLY the extracted text, nothing else."""

        try:
            response = self.vision_model.generate_content([ocr_prompt, pil_image])
            extracted_raw = response.text.strip()
        except Exception as e:
            return TextAccuracyResult(
                status="ERROR",
                error=f"OCR extraction failed: {e}"
            )

        # Handle no text case
        if extracted_raw == "NO_TEXT_FOUND" or not extracted_raw:
            return TextAccuracyResult(
                status="PASS",
                extracted_text=[],
                misspelled=[]
            )

        # Parse extracted text
        extracted_lines = [line.strip() for line in extracted_raw.split('\n') if line.strip()]

        # Extract individual words for spell-checking
        all_words = []
        for line in extracted_lines:
            # Split by whitespace and punctuation
            words = re.findall(r'\b[a-zA-Z]+\b', line)
            all_words.extend(words)

        # Spell-check words
        misspelled = self._spell_check(all_words)

        return TextAccuracyResult(
            status="PASS" if not misspelled else "FAIL",
            extracted_text=extracted_lines,
            misspelled=misspelled
        )

    def _spell_check(self, words: list) -> list:
        """Check words against dictionary and custom terms.

        Args:
            words: List of words to check

        Returns:
            List of misspelled words
        """
        misspelled = []

        # Try to use pyenchant if available
        try:
            import enchant
            dictionary = enchant.Dict("en_US")
            has_enchant = True
        except (ImportError, enchant.errors.DictNotFoundError):
            has_enchant = False
            # Fallback: basic common word list
            common_words = self._get_common_words()

        for word in words:
            word_lower = word.lower()

            # Skip very short words
            if len(word) <= 2:
                continue

            # Skip if in custom dictionary
            if word_lower in self._custom_words:
                continue

            # Skip all-caps acronyms
            if word.isupper() and len(word) <= 6:
                continue

            # Check spelling
            if has_enchant:
                if not dictionary.check(word) and not dictionary.check(word.lower()):
                    misspelled.append(word)
            else:
                # Fallback check
                if word_lower not in common_words:
                    # Only flag if it looks misspelled (not a proper noun or acronym)
                    if not word[0].isupper():
                        misspelled.append(word)

        return misspelled

    def _get_common_words(self) -> set:
        """Return a set of common English words for fallback spell-checking."""
        # Basic set of common words (expand as needed)
        return {
            'the', 'be', 'to', 'of', 'and', 'a', 'in', 'that', 'have', 'i',
            'it', 'for', 'not', 'on', 'with', 'he', 'as', 'you', 'do', 'at',
            'this', 'but', 'his', 'by', 'from', 'they', 'we', 'say', 'her', 'she',
            'or', 'an', 'will', 'my', 'one', 'all', 'would', 'there', 'their', 'what',
            'so', 'up', 'out', 'if', 'about', 'who', 'get', 'which', 'go', 'me',
            'when', 'make', 'can', 'like', 'time', 'no', 'just', 'him', 'know', 'take',
            'people', 'into', 'year', 'your', 'good', 'some', 'could', 'them', 'see', 'other',
            'than', 'then', 'now', 'look', 'only', 'come', 'its', 'over', 'think', 'also',
            'back', 'after', 'use', 'two', 'how', 'our', 'work', 'first', 'well', 'way',
            'even', 'new', 'want', 'because', 'any', 'these', 'give', 'day', 'most', 'us',
            # Healthcare-specific common words
            'health', 'healthcare', 'care', 'patient', 'patients', 'quality', 'measure',
            'measures', 'data', 'clinical', 'provider', 'providers', 'outcomes', 'gap',
            'gaps', 'risk', 'score', 'scores', 'screening', 'diabetes', 'cancer',
            'depression', 'medication', 'adherence', 'compliance', 'star', 'rating',
            'ratings', 'performance', 'improvement', 'better', 'faster', 'close',
            'identify', 'detect', 'automated', 'automation', 'software', 'platform',
            'solution', 'enterprise', 'built', 'people', 'who', 'by',
        }

    def _assess_clarity(self, image_path: Path) -> ClarityResult:
        """Use Gemini Vision to assess image clarity and quality.

        Evaluates:
        - Visual clarity and focus
        - Composition quality
        - Professional appearance
        - Color balance and lighting
        - Subject clarity (if people present)
        """
        pil_image = Image.open(image_path)

        clarity_prompt = """Analyze this healthcare technology marketing image for quality.

Evaluate the following aspects:
1. Visual Clarity: Is the image sharp and in focus?
2. Composition: Is the layout balanced and visually appealing?
3. Professionalism: Does it look like a professional marketing asset?
4. Colors: Are the colors well-balanced and appropriate?
5. Subject Clarity: If people are shown, are they clearly visible?

Provide your assessment in this EXACT format:
SCORE: [number 0-100]
STRENGTHS:
- [strength 1]
- [strength 2]
ISSUES:
- [issue 1, or "None" if no issues]

Be strict but fair. Marketing images for healthcare should look professional and trustworthy."""

        try:
            response = self.vision_model.generate_content([clarity_prompt, pil_image])
            response_text = response.text.strip()
        except Exception as e:
            return ClarityResult(
                score=0,
                error=f"Clarity assessment failed: {e}"
            )

        # Parse response
        score = self._extract_score(response_text)
        strengths = self._extract_list(response_text, "STRENGTHS:")
        issues = self._extract_list(response_text, "ISSUES:")

        # Filter out "None" from issues
        issues = [i for i in issues if i.lower() != "none"]

        return ClarityResult(
            score=score,
            feedback=response_text,
            strengths=strengths,
            issues=issues
        )

    def _extract_score(self, text: str) -> float:
        """Extract numeric score from response text."""
        # Look for SCORE: pattern
        match = re.search(r'SCORE:\s*(\d+(?:\.\d+)?)', text, re.IGNORECASE)
        if match:
            return float(match.group(1))

        # Fallback: look for any number at start of response
        match = re.search(r'^(\d+(?:\.\d+)?)', text.strip())
        if match:
            return float(match.group(1))

        return 50.0  # Default middle score if parsing fails

    def _extract_list(self, text: str, header: str) -> list:
        """Extract bulleted list items after a header."""
        items = []

        # Find the section
        match = re.search(rf'{re.escape(header)}\s*(.*?)(?=\n[A-Z]+:|$)', text, re.IGNORECASE | re.DOTALL)
        if not match:
            return items

        section = match.group(1)

        # Extract bullet points
        bullet_pattern = r'[-*•]\s*(.+?)(?=\n[-*•]|\n\n|$)'
        for bullet_match in re.finditer(bullet_pattern, section):
            item = bullet_match.group(1).strip()
            if item:
                items.append(item)

        return items

    def _validate_technical(self, image_path: Path, asset_id: str) -> TechnicalValidationResult:
        """Validate technical specifications of the image.

        Checks:
        - Image dimensions match expected specs
        - File size is within limits
        - Image format is correct (PNG)
        """
        warnings = []
        errors = []

        # Get expected specs
        expected_spec = get_asset_spec(asset_id)
        expected_width = expected_spec.get('width', 0)
        expected_height = expected_spec.get('height', 0)

        # Open image and get actual dimensions
        with Image.open(image_path) as img:
            actual_width, actual_height = img.size
            actual_format = img.format or "UNKNOWN"

        # Get file size
        file_size_kb = image_path.stat().st_size / 1024

        # Check dimensions
        dimensions_match = True
        if expected_width and expected_height:
            if actual_width != expected_width or actual_height != expected_height:
                dimensions_match = False
                # Allow some tolerance for AI-generated images
                width_diff = abs(actual_width - expected_width) / expected_width
                height_diff = abs(actual_height - expected_height) / expected_height
                if width_diff > 0.1 or height_diff > 0.1:
                    errors.append(f"Dimensions {actual_width}x{actual_height} differ significantly from expected {expected_width}x{expected_height}")
                else:
                    warnings.append(f"Dimensions {actual_width}x{actual_height} differ slightly from expected {expected_width}x{expected_height}")

        # Check file size
        if file_size_kb > MAX_FILE_SIZE_KB:
            warnings.append(f"File size {file_size_kb:.0f}KB exceeds recommended maximum {MAX_FILE_SIZE_KB}KB")

        # Check format
        if actual_format.upper() not in ['PNG', 'JPEG', 'JPG']:
            warnings.append(f"Format {actual_format} may not be optimal for web (PNG recommended)")

        # Determine overall status
        if errors:
            status = "FAIL"
        elif warnings:
            status = "WARNING"
        else:
            status = "PASS"

        return TechnicalValidationResult(
            status=status,
            dimensions=f"{actual_width}x{actual_height}",
            expected_dimensions=f"{expected_width}x{expected_height}" if expected_width and expected_height else "Not specified",
            file_size_kb=file_size_kb,
            format=actual_format,
            warnings=warnings,
            errors=errors
        )


def verify_image_cli(image_path: str, asset_id: str, version: int = 1) -> None:
    """Command-line interface for quality verification.

    Args:
        image_path: Path to image file
        asset_id: Asset identifier
        version: Version number
    """
    path = Path(image_path)
    if not path.exists():
        print(f"Error: Image not found: {image_path}")
        return

    print(f"\n{'='*60}")
    print(f"Quality Verification: {asset_id} v{version}")
    print(f"Image: {image_path}")
    print(f"{'='*60}\n")

    qc = QualityController()
    report = qc.verify_image(path, asset_id, version)

    # Print results
    print("TEXT ACCURACY")
    print("-" * 40)
    if report.text_accuracy:
        print(f"Status: {report.text_accuracy.status}")
        if report.text_accuracy.extracted_text:
            print("Extracted text:")
            for text in report.text_accuracy.extracted_text:
                print(f"  - {text}")
        if report.text_accuracy.misspelled:
            print(f"Misspelled words: {', '.join(report.text_accuracy.misspelled)}")
        if report.text_accuracy.error:
            print(f"Error: {report.text_accuracy.error}")
    print()

    print("CLARITY ASSESSMENT")
    print("-" * 40)
    if report.clarity:
        print(f"Score: {report.clarity.score}/100")
        if report.clarity.strengths:
            print("Strengths:")
            for s in report.clarity.strengths:
                print(f"  + {s}")
        if report.clarity.issues:
            print("Issues:")
            for i in report.clarity.issues:
                print(f"  - {i}")
        if report.clarity.error:
            print(f"Error: {report.clarity.error}")
    print()

    print("TECHNICAL VALIDATION")
    print("-" * 40)
    if report.technical:
        print(f"Status: {report.technical.status}")
        print(f"Dimensions: {report.technical.dimensions} (expected: {report.technical.expected_dimensions})")
        print(f"File size: {report.technical.file_size_kb:.0f} KB")
        print(f"Format: {report.technical.format}")
        if report.technical.warnings:
            for w in report.technical.warnings:
                print(f"Warning: {w}")
        if report.technical.errors:
            for e in report.technical.errors:
                print(f"Error: {e}")
    print()

    print("OVERALL ASSESSMENT")
    print("-" * 40)
    print(f"Quality Score: {report.overall_score:.0f}/100")
    print(f"Status: {report.overall_status}")
    print()


if __name__ == "__main__":
    import sys

    if len(sys.argv) < 3:
        print("Usage: python qc_engine.py <image_path> <asset_id> [version]")
        print("Example: python qc_engine.py ./generated/hero/HERO-01.png HERO-01 1")
        sys.exit(1)

    image_path = sys.argv[1]
    asset_id = sys.argv[2]
    version = int(sys.argv[3]) if len(sys.argv) > 3 else 1

    verify_image_cli(image_path, asset_id, version)

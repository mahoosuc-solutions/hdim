"""Configuration for AI Image Generation System with Quality Control"""

import os
from pathlib import Path

# API Configuration
GOOGLE_API_KEY = os.environ.get("GOOGLE_API_KEY", "")

# Model Configuration
GEMINI_IMAGE_MODEL = "gemini-2.0-flash-exp-image-generation"
GEMINI_VISION_MODEL = "gemini-2.0-flash-exp"

# Generation Configuration
RATE_LIMIT_SECONDS = 7  # Seconds between API calls
MAX_RETRIES = 3
RETRY_DELAY_SECONDS = 10

# Web Portal Configuration
REVIEW_PORTAL_HOST = "127.0.0.1"
REVIEW_PORTAL_PORT = 5555
DEBUG_MODE = True

# Directory Configuration
BASE_DIR = Path(__file__).parent
OUTPUT_DIR = BASE_DIR / "generated"
PRODUCTION_DIR = BASE_DIR.parent / "public" / "images"
DB_PATH = BASE_DIR / "iteration_state.db"
TEMPLATES_DIR = BASE_DIR / "templates"
STATIC_DIR = BASE_DIR / "static"

# Create directories if they don't exist
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
TEMPLATES_DIR.mkdir(parents=True, exist_ok=True)
STATIC_DIR.mkdir(parents=True, exist_ok=True)

# Quality Thresholds
MIN_CLARITY_SCORE = 75  # Minimum clarity score (0-100)
MAX_FILE_SIZE_KB = 1024  # Maximum file size in KB before warning
EXPECTED_FORMAT = "PNG"

# Image Specifications by Category
IMAGE_SPECS = {
    "hero": {
        "HERO-01": {"width": 1920, "height": 1080, "name": "Main Hero - Desktop"},
        "HERO-02": {"width": 750, "height": 1334, "name": "Main Hero - Mobile"},
        "HERO-03": {"width": 1200, "height": 630, "name": "Social Share Image"},
    },
    "portraits": {
        "PORTRAIT-MARIA": {"width": 800, "height": 1000, "name": "Maria - Diabetes Patient"},
        "PORTRAIT-JAMES": {"width": 800, "height": 1000, "name": "James - Depression Survivor"},
        "PORTRAIT-SARAH": {"width": 800, "height": 1000, "name": "Sarah - Cancer Survivor"},
    },
    "trust-badges": {
        "BADGE-FHIR": {"width": 200, "height": 200, "name": "FHIR R4 Certified"},
        "BADGE-CQL": {"width": 200, "height": 200, "name": "CQL Standards"},
        "BADGE-HIPAA": {"width": 200, "height": 200, "name": "HIPAA Compliant"},
        "BADGE-TESTS": {"width": 200, "height": 200, "name": "11,000+ Tests"},
        "BADGE-UPTIME": {"width": 200, "height": 200, "name": "99.9% Uptime"},
    },
    "dashboards": {
        "DASH-QUALITY": {"width": 1400, "height": 900, "name": "Quality Dashboard"},
        "DASH-CARE-GAP": {"width": 1400, "height": 900, "name": "Care Gap Dashboard"},
        "DASH-ANALYTICS": {"width": 1400, "height": 900, "name": "Analytics Dashboard"},
    },
    "icons": {
        "ICON-STAR": {"width": 100, "height": 100, "name": "Star Rating Icon"},
        "ICON-CHECKMARK": {"width": 100, "height": 100, "name": "Checkmark Icon"},
        "ICON-ALERT": {"width": 100, "height": 100, "name": "Alert Icon"},
    },
}

# Brand Colors (for reference in prompts)
BRAND_COLORS = {
    "primary": "#0066CC",  # Deep Blue
    "accent": "#00A5B5",   # Warm Teal
    "background": "#F8FAFC",  # Light Gray
    "text_dark": "#1E293B",   # Dark Text
    "text_light": "#64748B",  # Light Text
}

# Priority Order for Asset Generation
ASSET_PRIORITY = [
    # Priority 1: Hero Images
    "HERO-01",
    "HERO-02",
    "HERO-03",
    # Priority 2: Patient Portraits
    "PORTRAIT-MARIA",
    "PORTRAIT-JAMES",
    "PORTRAIT-SARAH",
    # Priority 3: Trust Badges
    "BADGE-FHIR",
    "BADGE-CQL",
    "BADGE-HIPAA",
    "BADGE-TESTS",
    "BADGE-UPTIME",
]

# Custom Dictionary for Spell Checking
CUSTOM_DICTIONARY_WORDS = [
    "HDIM",
    "FHIR",
    "HEDIS",
    "CQL",
    "HIPAA",
    "ACO",
    "ACOs",
    "FQHC",
    "FQHCs",
    "EHR",
    "PHI",
    "QRDA",
    "HCC",
    "RAF",
    "CMS",
    "HealthData",
    "interoperability",
    "telehealth",
]

def get_asset_category(asset_id: str) -> str:
    """Get the category for an asset ID."""
    for category, assets in IMAGE_SPECS.items():
        if asset_id in assets:
            return category
    return "unknown"

def get_asset_spec(asset_id: str) -> dict:
    """Get the specification for an asset ID."""
    for category, assets in IMAGE_SPECS.items():
        if asset_id in assets:
            return assets[asset_id]
    return {}

def get_all_asset_ids() -> list:
    """Get all asset IDs across all categories."""
    ids = []
    for category, assets in IMAGE_SPECS.items():
        ids.extend(assets.keys())
    return ids

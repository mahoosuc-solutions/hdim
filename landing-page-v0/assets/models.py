"""Data models for AI Image Generation Quality Control System"""

from dataclasses import dataclass, field, asdict
from datetime import datetime
from typing import List, Optional
from pathlib import Path
import json


@dataclass
class TextAccuracyResult:
    """Result of text accuracy verification via OCR and spell-check."""
    status: str  # "PASS" or "FAIL"
    extracted_text: List[str] = field(default_factory=list)
    misspelled: List[str] = field(default_factory=list)
    error: Optional[str] = None

    def to_dict(self) -> dict:
        return asdict(self)

    def to_json(self) -> str:
        return json.dumps(self.to_dict())


@dataclass
class ClarityResult:
    """Result of image clarity assessment via Vision AI."""
    score: float  # 0-100
    feedback: str = ""
    strengths: List[str] = field(default_factory=list)
    issues: List[str] = field(default_factory=list)
    error: Optional[str] = None

    def to_dict(self) -> dict:
        return asdict(self)

    def to_json(self) -> str:
        return json.dumps(self.to_dict())


@dataclass
class TechnicalValidationResult:
    """Result of technical validation (dimensions, size, format)."""
    status: str  # "PASS", "FAIL", or "WARNING"
    dimensions: str = ""  # "1920x1080"
    file_size_kb: float = 0.0
    format: str = ""
    expected_dimensions: str = ""
    warnings: List[str] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)

    def to_dict(self) -> dict:
        return asdict(self)

    def to_json(self) -> str:
        return json.dumps(self.to_dict())


@dataclass
class QualityReport:
    """Complete quality report for a generated image."""
    asset_id: str
    version: int
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())

    # Quality Results
    text_accuracy: Optional[TextAccuracyResult] = None
    clarity: Optional[ClarityResult] = None
    technical: Optional[TechnicalValidationResult] = None

    # Overall Assessment
    overall_status: str = "PENDING"  # "READY_FOR_REVIEW", "FAILED_QC", "PENDING"
    overall_score: float = 0.0  # Combined score 0-100

    def calculate_overall_score(self) -> float:
        """Calculate overall quality score from components."""
        scores = []

        # Text accuracy: Pass = 100, Fail = 0
        if self.text_accuracy:
            scores.append(100 if self.text_accuracy.status == "PASS" else 0)

        # Clarity score is already 0-100
        if self.clarity:
            scores.append(self.clarity.score)

        # Technical: Pass = 100, Warning = 75, Fail = 0
        if self.technical:
            if self.technical.status == "PASS":
                scores.append(100)
            elif self.technical.status == "WARNING":
                scores.append(75)
            else:
                scores.append(0)

        self.overall_score = sum(scores) / len(scores) if scores else 0
        return self.overall_score

    def determine_status(self) -> str:
        """Determine overall status based on quality checks."""
        # Fail if text has spelling errors
        if self.text_accuracy and self.text_accuracy.status == "FAIL":
            self.overall_status = "FAILED_QC"
            return self.overall_status

        # Fail if technical validation failed
        if self.technical and self.technical.status == "FAIL":
            self.overall_status = "FAILED_QC"
            return self.overall_status

        # Ready for review if clarity is above threshold
        from config import MIN_CLARITY_SCORE
        if self.clarity and self.clarity.score >= MIN_CLARITY_SCORE:
            self.overall_status = "READY_FOR_REVIEW"
        elif self.clarity and self.clarity.score < MIN_CLARITY_SCORE:
            self.overall_status = "FAILED_QC"
        else:
            self.overall_status = "PENDING"

        return self.overall_status

    def to_dict(self) -> dict:
        return {
            "asset_id": self.asset_id,
            "version": self.version,
            "timestamp": self.timestamp,
            "text_accuracy": self.text_accuracy.to_dict() if self.text_accuracy else None,
            "clarity": self.clarity.to_dict() if self.clarity else None,
            "technical": self.technical.to_dict() if self.technical else None,
            "overall_status": self.overall_status,
            "overall_score": self.overall_score,
        }

    def to_json(self) -> str:
        return json.dumps(self.to_dict(), indent=2)


@dataclass
class GenerationResult:
    """Result of image generation attempt."""
    success: bool
    asset_id: str = ""
    version: int = 0
    image_path: Optional[Path] = None
    qc_report: Optional[QualityReport] = None
    error: Optional[str] = None
    generation_time_ms: float = 0.0

    def to_dict(self) -> dict:
        return {
            "success": self.success,
            "asset_id": self.asset_id,
            "version": self.version,
            "image_path": str(self.image_path) if self.image_path else None,
            "qc_report": self.qc_report.to_dict() if self.qc_report else None,
            "error": self.error,
            "generation_time_ms": self.generation_time_ms,
        }


@dataclass
class AssetStatus:
    """Status information for an asset displayed in dashboard."""
    asset_id: str
    name: str
    category: str
    version: int
    quality_score: float
    clarity_score: float
    text_accuracy_status: str
    human_review_status: str  # "PENDING", "APPROVED", "REJECTED"
    is_published: bool
    filepath: str
    generated_at: str


@dataclass
class DashboardStats:
    """Aggregate statistics for dashboard."""
    total_assets: int = 0
    total_versions: int = 0
    pending_review: int = 0
    approved: int = 0
    rejected: int = 0
    published: int = 0
    avg_quality_score: float = 0.0
    avg_clarity_score: float = 0.0
    avg_iterations: float = 0.0


@dataclass
class ReviewAction:
    """Action taken during human review."""
    asset_id: str
    version: int
    action: str  # "APPROVE", "REJECT"
    feedback: Optional[str] = None
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())


@dataclass
class PublishResult:
    """Result of publishing an asset."""
    asset_id: str
    status: str  # "success", "error"
    source_path: Optional[str] = None
    dest_path: Optional[str] = None
    backup_path: Optional[str] = None
    error_message: Optional[str] = None


# Type aliases for clarity
AssetId = str
Version = int

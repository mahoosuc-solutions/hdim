"""Orchestration Layer for AI Image Generation Quality Control System.

This module manages the iteration loop and state for the QC system:
- Database persistence for iterations and quality metrics
- Prompt management and refinement
- Approval/rejection workflow
- Publishing approved assets to production
"""

import os
import json
import shutil
import sqlite3
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Any

from models import (
    QualityReport,
    AssetStatus,
    DashboardStats,
    PublishResult,
)
from config import (
    DB_PATH,
    OUTPUT_DIR,
    PRODUCTION_DIR,
    IMAGE_SPECS,
    GOOGLE_API_KEY,
    get_asset_category,
    get_asset_spec,
    get_all_asset_ids,
)


class Orchestrator:
    """Manages state and coordinates the iteration loop."""

    def __init__(self, db_path: Optional[Path] = None):
        """Initialize the orchestrator.

        Args:
            db_path: Path to SQLite database. Defaults to config value.
        """
        self.db_path = db_path or DB_PATH
        self._init_db()

    def _init_db(self) -> None:
        """Initialize the database with schema."""
        schema_path = Path(__file__).parent / "db_schema.sql"

        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row

        try:
            if schema_path.exists():
                with open(schema_path, 'r') as f:
                    conn.executescript(f.read())
            else:
                # Inline minimal schema if file not found
                conn.executescript("""
                    CREATE TABLE IF NOT EXISTS assets (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        asset_id TEXT NOT NULL,
                        version INTEGER NOT NULL,
                        filepath TEXT NOT NULL,
                        generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        prompt TEXT,
                        quality_score REAL,
                        text_accuracy_status TEXT,
                        text_accuracy_details TEXT,
                        clarity_score REAL,
                        clarity_feedback TEXT,
                        technical_status TEXT,
                        technical_details TEXT,
                        human_review_status TEXT DEFAULT 'PENDING',
                        human_feedback TEXT,
                        reviewed_at TIMESTAMP,
                        is_published BOOLEAN DEFAULT 0,
                        published_at TIMESTAMP,
                        UNIQUE(asset_id, version)
                    );

                    CREATE TABLE IF NOT EXISTS prompts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        asset_id TEXT NOT NULL UNIQUE,
                        base_prompt TEXT NOT NULL,
                        current_iteration_prompt TEXT,
                        iteration_count INTEGER DEFAULT 1,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );

                    CREATE INDEX IF NOT EXISTS idx_assets_asset_id ON assets(asset_id);
                    CREATE INDEX IF NOT EXISTS idx_assets_status ON assets(human_review_status);
                """)
            conn.commit()
        finally:
            conn.close()

    def _get_conn(self) -> sqlite3.Connection:
        """Get a database connection with row factory."""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    # ==================== Iteration Management ====================

    def save_iteration(
        self,
        asset_id: str,
        version: int,
        filepath: Path,
        prompt: str,
        qc_report: QualityReport,
    ) -> int:
        """Save a new generation iteration with quality metrics.

        Args:
            asset_id: Asset identifier
            version: Version number
            filepath: Path to generated image
            prompt: Prompt used for generation
            qc_report: Quality report from verification

        Returns:
            Database row ID
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                INSERT INTO assets (
                    asset_id, version, filepath, prompt,
                    quality_score, text_accuracy_status, text_accuracy_details,
                    clarity_score, clarity_feedback,
                    technical_status, technical_details,
                    human_review_status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING')
            """, (
                asset_id,
                version,
                str(filepath),
                prompt,
                qc_report.overall_score,
                qc_report.text_accuracy.status if qc_report.text_accuracy else None,
                qc_report.text_accuracy.to_json() if qc_report.text_accuracy else None,
                qc_report.clarity.score if qc_report.clarity else None,
                qc_report.clarity.feedback if qc_report.clarity else None,
                qc_report.technical.status if qc_report.technical else None,
                qc_report.technical.to_json() if qc_report.technical else None,
            ))
            conn.commit()
            return cursor.lastrowid
        finally:
            conn.close()

    def get_latest_iteration(self, asset_id: str) -> Optional[Dict[str, Any]]:
        """Get the most recent version of an asset.

        Args:
            asset_id: Asset identifier

        Returns:
            Dictionary with iteration data or None
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT * FROM assets
                WHERE asset_id = ?
                ORDER BY version DESC
                LIMIT 1
            """, (asset_id,))

            row = cursor.fetchone()
            return dict(row) if row else None
        finally:
            conn.close()

    def get_previous_iteration(self, asset_id: str) -> Optional[Dict[str, Any]]:
        """Get the second-most recent version of an asset.

        Args:
            asset_id: Asset identifier

        Returns:
            Dictionary with iteration data or None
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT * FROM assets
                WHERE asset_id = ?
                ORDER BY version DESC
                LIMIT 1 OFFSET 1
            """, (asset_id,))

            row = cursor.fetchone()
            return dict(row) if row else None
        finally:
            conn.close()

    def get_version_history(self, asset_id: str) -> List[Dict[str, Any]]:
        """Get all versions of an asset.

        Args:
            asset_id: Asset identifier

        Returns:
            List of iteration dictionaries
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT * FROM assets
                WHERE asset_id = ?
                ORDER BY version DESC
            """, (asset_id,))

            return [dict(row) for row in cursor.fetchall()]
        finally:
            conn.close()

    def get_next_version(self, asset_id: str) -> int:
        """Get the next version number for an asset.

        Args:
            asset_id: Asset identifier

        Returns:
            Next version number (1 if first iteration)
        """
        latest = self.get_latest_iteration(asset_id)
        return (latest['version'] + 1) if latest else 1

    # ==================== Prompt Management ====================

    def get_prompt(self, asset_id: str, version: int = 0) -> Optional[str]:
        """Get the prompt for an asset.

        Args:
            asset_id: Asset identifier
            version: Version number (0 = current iteration prompt)

        Returns:
            Prompt string or None
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT base_prompt, current_iteration_prompt
                FROM prompts
                WHERE asset_id = ?
            """, (asset_id,))

            row = cursor.fetchone()
            if not row:
                return None

            # Return current iteration prompt if available, else base prompt
            return row['current_iteration_prompt'] or row['base_prompt']
        finally:
            conn.close()

    def set_base_prompt(self, asset_id: str, prompt: str) -> None:
        """Set the base prompt for an asset.

        Args:
            asset_id: Asset identifier
            prompt: Base prompt text
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                INSERT INTO prompts (asset_id, base_prompt, current_iteration_prompt)
                VALUES (?, ?, ?)
                ON CONFLICT(asset_id) DO UPDATE SET
                    base_prompt = excluded.base_prompt,
                    updated_at = CURRENT_TIMESTAMP
            """, (asset_id, prompt, prompt))
            conn.commit()
        finally:
            conn.close()

    def create_refined_prompt(self, asset_id: str, feedback: str) -> int:
        """Generate a refined prompt based on human feedback.

        Uses Gemini to evolve the prompt based on feedback, then saves it.

        Args:
            asset_id: Asset identifier
            feedback: Human feedback on the rejected image

        Returns:
            New version number
        """
        # Get current prompt
        current_prompt = self.get_prompt(asset_id)
        if not current_prompt:
            raise ValueError(f"No prompt found for asset {asset_id}")

        # Use Gemini to refine the prompt
        refined_prompt = self._refine_prompt_with_ai(current_prompt, feedback)

        # Save refined prompt
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                UPDATE prompts
                SET current_iteration_prompt = ?,
                    iteration_count = iteration_count + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE asset_id = ?
            """, (refined_prompt, asset_id))
            conn.commit()
        finally:
            conn.close()

        return self.get_next_version(asset_id)

    def _refine_prompt_with_ai(self, current_prompt: str, feedback: str) -> str:
        """Use Gemini to refine a prompt based on feedback.

        Args:
            current_prompt: The current generation prompt
            feedback: Human feedback on what to improve

        Returns:
            Refined prompt string
        """
        try:
            import google.generativeai as genai
            genai.configure(api_key=GOOGLE_API_KEY or os.environ.get("GOOGLE_API_KEY", ""))
            model = genai.GenerativeModel('gemini-2.0-flash-exp')

            refinement_prompt = f"""You are helping refine an image generation prompt based on human feedback.

CURRENT PROMPT:
{current_prompt}

HUMAN FEEDBACK ON GENERATED IMAGE:
{feedback}

INSTRUCTIONS:
1. Analyze the feedback to understand what needs improvement
2. Modify the prompt to address the specific issues mentioned
3. Preserve the original style, brand colors (Deep Blue #0066CC, Warm Teal #00A5B5), and core requirements
4. Keep the same overall concept and composition goals
5. Add specific details to improve clarity on the issues mentioned

Return ONLY the revised prompt text. Do not include any explanations or commentary."""

            response = model.generate_content(refinement_prompt)
            return response.text.strip()

        except Exception as e:
            # Fallback: append feedback as additional instruction
            return f"{current_prompt}\n\nADDITIONAL REQUIREMENTS (based on feedback):\n{feedback}"

    # ==================== Review Workflow ====================

    def approve(self, asset_id: str, version: int) -> None:
        """Mark an asset version as approved.

        Args:
            asset_id: Asset identifier
            version: Version number to approve
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                UPDATE assets
                SET human_review_status = 'APPROVED',
                    reviewed_at = CURRENT_TIMESTAMP
                WHERE asset_id = ? AND version = ?
            """, (asset_id, version))
            conn.commit()
        finally:
            conn.close()

    def reject(self, asset_id: str, version: int, feedback: str) -> None:
        """Mark an asset version as rejected with feedback.

        Args:
            asset_id: Asset identifier
            version: Version number to reject
            feedback: Rejection reason/feedback
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                UPDATE assets
                SET human_review_status = 'REJECTED',
                    human_feedback = ?,
                    reviewed_at = CURRENT_TIMESTAMP
                WHERE asset_id = ? AND version = ?
            """, (feedback, asset_id, version))
            conn.commit()
        finally:
            conn.close()

    # ==================== Publishing ====================

    def get_ready_to_publish(self) -> List[Dict[str, Any]]:
        """Get all approved assets that haven't been published.

        Returns:
            List of asset dictionaries ready for publishing
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT * FROM assets
                WHERE human_review_status = 'APPROVED'
                AND is_published = 0
                ORDER BY reviewed_at DESC
            """)

            return [dict(row) for row in cursor.fetchall()]
        finally:
            conn.close()

    def batch_publish(self, asset_ids: List[str]) -> List[PublishResult]:
        """Publish approved assets to production.

        Args:
            asset_ids: List of asset IDs to publish

        Returns:
            List of PublishResult for each asset
        """
        results = []

        for asset_id in asset_ids:
            result = self._publish_single(asset_id)
            results.append(result)

        return results

    def _publish_single(self, asset_id: str) -> PublishResult:
        """Publish a single asset to production.

        Args:
            asset_id: Asset identifier

        Returns:
            PublishResult with status and paths
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            # Get approved version
            cursor.execute("""
                SELECT filepath FROM assets
                WHERE asset_id = ?
                AND human_review_status = 'APPROVED'
                ORDER BY version DESC
                LIMIT 1
            """, (asset_id,))

            row = cursor.fetchone()
            if not row:
                return PublishResult(
                    asset_id=asset_id,
                    status="error",
                    error_message="No approved version found"
                )

            source_path = Path(row['filepath'])
            if not source_path.exists():
                return PublishResult(
                    asset_id=asset_id,
                    status="error",
                    error_message=f"Source file not found: {source_path}"
                )

            # Determine destination
            category = get_asset_category(asset_id)
            dest_dir = PRODUCTION_DIR / category
            dest_dir.mkdir(parents=True, exist_ok=True)

            # Create filename based on asset ID
            dest_filename = f"{asset_id.lower().replace('-', '_')}.png"
            dest_path = dest_dir / dest_filename

            # Backup existing file if present
            backup_path = None
            if dest_path.exists():
                backup_path = dest_path.with_suffix('.png.backup')
                shutil.copy2(dest_path, backup_path)

            # Copy new file
            shutil.copy2(source_path, dest_path)

            # Update database
            cursor.execute("""
                UPDATE assets
                SET is_published = 1,
                    published_at = CURRENT_TIMESTAMP
                WHERE asset_id = ? AND filepath = ?
            """, (asset_id, str(source_path)))
            conn.commit()

            return PublishResult(
                asset_id=asset_id,
                status="success",
                source_path=str(source_path),
                dest_path=str(dest_path),
                backup_path=str(backup_path) if backup_path else None
            )

        except Exception as e:
            return PublishResult(
                asset_id=asset_id,
                status="error",
                error_message=str(e)
            )
        finally:
            conn.close()

    # ==================== Dashboard Data ====================

    def get_all_assets_status(self) -> Dict[str, List[AssetStatus]]:
        """Get status for all assets grouped by category.

        Returns:
            Dictionary mapping category to list of AssetStatus
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        assets_by_category = {}

        try:
            # Get latest version for each asset
            for category, assets in IMAGE_SPECS.items():
                category_assets = []

                for asset_id, spec in assets.items():
                    cursor.execute("""
                        SELECT * FROM assets
                        WHERE asset_id = ?
                        ORDER BY version DESC
                        LIMIT 1
                    """, (asset_id,))

                    row = cursor.fetchone()

                    if row:
                        status = AssetStatus(
                            asset_id=asset_id,
                            name=spec.get('name', asset_id),
                            category=category,
                            version=row['version'],
                            quality_score=row['quality_score'] or 0,
                            clarity_score=row['clarity_score'] or 0,
                            text_accuracy_status=row['text_accuracy_status'] or '',
                            human_review_status=row['human_review_status'] or 'PENDING',
                            is_published=bool(row['is_published']),
                            filepath=row['filepath'],
                            generated_at=row['generated_at'],
                        )
                    else:
                        # Asset not yet generated
                        status = AssetStatus(
                            asset_id=asset_id,
                            name=spec.get('name', asset_id),
                            category=category,
                            version=0,
                            quality_score=0,
                            clarity_score=0,
                            text_accuracy_status='',
                            human_review_status='',
                            is_published=False,
                            filepath='',
                            generated_at='',
                        )

                    category_assets.append(status)

                assets_by_category[category] = category_assets

            return assets_by_category

        finally:
            conn.close()

    def get_stats(self) -> DashboardStats:
        """Get aggregate statistics for dashboard.

        Returns:
            DashboardStats with counts and averages
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            # Get counts by status
            cursor.execute("""
                SELECT
                    COUNT(DISTINCT asset_id) as total_assets,
                    COUNT(*) as total_versions,
                    SUM(CASE WHEN human_review_status = 'PENDING' THEN 1 ELSE 0 END) as pending,
                    SUM(CASE WHEN human_review_status = 'APPROVED' THEN 1 ELSE 0 END) as approved,
                    SUM(CASE WHEN human_review_status = 'REJECTED' THEN 1 ELSE 0 END) as rejected,
                    SUM(CASE WHEN is_published = 1 THEN 1 ELSE 0 END) as published,
                    AVG(quality_score) as avg_quality,
                    AVG(clarity_score) as avg_clarity
                FROM assets
            """)

            row = cursor.fetchone()

            # Get average iterations per asset
            cursor.execute("""
                SELECT AVG(version) as avg_iterations
                FROM (
                    SELECT asset_id, MAX(version) as version
                    FROM assets
                    GROUP BY asset_id
                )
            """)
            avg_row = cursor.fetchone()

            return DashboardStats(
                total_assets=row['total_assets'] or 0,
                total_versions=row['total_versions'] or 0,
                pending_review=row['pending'] or 0,
                approved=row['approved'] or 0,
                rejected=row['rejected'] or 0,
                published=row['published'] or 0,
                avg_quality_score=row['avg_quality'] or 0,
                avg_clarity_score=row['avg_clarity'] or 0,
                avg_iterations=avg_row['avg_iterations'] or 0,
            )

        finally:
            conn.close()

    def get_pending_review(self) -> List[Dict[str, Any]]:
        """Get assets pending human review.

        Returns:
            List of asset dictionaries awaiting review
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT a.*,
                       (SELECT name FROM (SELECT asset_id, name FROM (
                           SELECT 'HERO-01' as asset_id, 'Main Hero - Desktop' as name
                           UNION SELECT 'HERO-02', 'Main Hero - Mobile'
                           UNION SELECT 'HERO-03', 'Social Share Image'
                           -- Add more as needed
                       )) WHERE asset_id = a.asset_id) as name
                FROM assets a
                WHERE a.human_review_status = 'PENDING'
                ORDER BY a.generated_at DESC
            """)

            results = []
            for row in cursor.fetchall():
                data = dict(row)
                # Add name from config if not in query
                if not data.get('name'):
                    spec = get_asset_spec(data['asset_id'])
                    data['name'] = spec.get('name', data['asset_id'])
                results.append(data)

            return results

        finally:
            conn.close()

    def get_qc_report(self, asset_id: str, version: int) -> Optional[QualityReport]:
        """Reconstruct QualityReport from database.

        Args:
            asset_id: Asset identifier
            version: Version number

        Returns:
            QualityReport object or None
        """
        conn = self._get_conn()
        cursor = conn.cursor()

        try:
            cursor.execute("""
                SELECT * FROM assets
                WHERE asset_id = ? AND version = ?
            """, (asset_id, version))

            row = cursor.fetchone()
            if not row:
                return None

            from models import TextAccuracyResult, ClarityResult, TechnicalValidationResult

            # Reconstruct text accuracy
            text_accuracy = None
            if row['text_accuracy_details']:
                ta_data = json.loads(row['text_accuracy_details'])
                text_accuracy = TextAccuracyResult(**ta_data)
            elif row['text_accuracy_status']:
                text_accuracy = TextAccuracyResult(status=row['text_accuracy_status'])

            # Reconstruct clarity
            clarity = None
            if row['clarity_score'] is not None:
                clarity = ClarityResult(
                    score=row['clarity_score'],
                    feedback=row['clarity_feedback'] or ''
                )

            # Reconstruct technical
            technical = None
            if row['technical_details']:
                tech_data = json.loads(row['technical_details'])
                technical = TechnicalValidationResult(**tech_data)
            elif row['technical_status']:
                technical = TechnicalValidationResult(status=row['technical_status'])

            return QualityReport(
                asset_id=asset_id,
                version=version,
                text_accuracy=text_accuracy,
                clarity=clarity,
                technical=technical,
                overall_score=row['quality_score'] or 0,
                overall_status='READY_FOR_REVIEW' if row['human_review_status'] == 'PENDING' else row['human_review_status'],
            )

        finally:
            conn.close()


if __name__ == "__main__":
    # Quick test
    orch = Orchestrator()
    stats = orch.get_stats()
    print(f"Total assets: {stats.total_assets}")
    print(f"Pending review: {stats.pending_review}")
    print(f"Published: {stats.published}")

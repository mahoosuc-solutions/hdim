#!/usr/bin/env python3
"""Web Review Portal for AI Image Generation Quality Control.

A Flask-based local web application for reviewing and approving
generated visual assets.

Usage:
    python review_portal.py

Then open: http://localhost:5555
"""

import os
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent))

from flask import (
    Flask,
    render_template,
    request,
    jsonify,
    send_from_directory,
    abort,
)
from flask_cors import CORS

from config import (
    REVIEW_PORTAL_HOST,
    REVIEW_PORTAL_PORT,
    DEBUG_MODE,
    OUTPUT_DIR,
    PRODUCTION_DIR,
    TEMPLATES_DIR,
    STATIC_DIR,
    get_asset_spec,
)
from orchestrator import Orchestrator
from models import DashboardStats


# Initialize Flask app
app = Flask(
    __name__,
    template_folder=str(TEMPLATES_DIR),
    static_folder=str(STATIC_DIR),
)
CORS(app)

# Initialize orchestrator
orchestrator = Orchestrator()


# ==================== Routes ====================

@app.route('/')
def dashboard():
    """Main dashboard showing all assets."""
    assets_by_category = orchestrator.get_all_assets_status()
    stats = orchestrator.get_stats()

    return render_template(
        'dashboard.html',
        assets_by_category=assets_by_category,
        stats=stats,
    )


@app.route('/review/<asset_id>')
def review_asset(asset_id: str):
    """Review individual asset."""
    current = orchestrator.get_latest_iteration(asset_id)
    if not current:
        # Asset not yet generated - show placeholder
        spec = get_asset_spec(asset_id)
        current = {
            'asset_id': asset_id,
            'name': spec.get('name', asset_id),
            'version': 0,
            'filepath': '',
            'human_review_status': '',
            'generated_at': '',
        }

    previous = orchestrator.get_previous_iteration(asset_id)

    # Add name from spec
    spec = get_asset_spec(asset_id)
    current['name'] = spec.get('name', asset_id)
    if previous:
        previous['name'] = spec.get('name', asset_id)

    # Get QC report
    qc_report = None
    if current.get('version', 0) > 0:
        qc_report = orchestrator.get_qc_report(asset_id, current['version'])
        if qc_report:
            qc_report = qc_report.to_dict()

    # Get current prompt
    current_prompt = orchestrator.get_prompt(asset_id) or ''

    # Get version history
    version_history = orchestrator.get_version_history(asset_id)

    return render_template(
        'review.html',
        asset_id=asset_id,
        current=current,
        previous=previous,
        qc_report=qc_report,
        current_prompt=current_prompt,
        version_history=version_history,
    )


@app.route('/batch')
def batch_review():
    """Batch review multiple assets."""
    pending_assets = orchestrator.get_pending_review()

    return render_template(
        'batch.html',
        pending_assets=pending_assets,
    )


@app.route('/publish')
def publish_page():
    """Publishing page for approved assets."""
    ready_assets = orchestrator.get_ready_to_publish()

    # Add names from specs
    for asset in ready_assets:
        spec = get_asset_spec(asset['asset_id'])
        asset['name'] = spec.get('name', asset['asset_id'])

    # Get recently published
    # For now, we'll just show the production directory
    published_assets = []

    return render_template(
        'publish.html',
        ready_assets=ready_assets,
        published_assets=published_assets,
        production_dir=str(PRODUCTION_DIR),
    )


# ==================== API Endpoints ====================

@app.route('/api/approve', methods=['POST'])
def approve_asset():
    """Approve an asset version."""
    data = request.get_json()
    asset_id = data.get('asset_id')
    version = data.get('version')

    if not asset_id or version is None:
        return jsonify({'error': 'Missing asset_id or version'}), 400

    try:
        orchestrator.approve(asset_id, version)
        return jsonify({'status': 'approved', 'asset_id': asset_id, 'version': version})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/reject', methods=['POST'])
def reject_and_regenerate():
    """Reject asset and trigger regeneration."""
    data = request.get_json()
    asset_id = data.get('asset_id')
    version = data.get('version')
    feedback = data.get('feedback', '')

    if not asset_id or version is None:
        return jsonify({'error': 'Missing asset_id or version'}), 400

    if not feedback.strip():
        return jsonify({'error': 'Feedback required for rejection'}), 400

    try:
        # Mark as rejected
        orchestrator.reject(asset_id, version, feedback)

        # Create refined prompt for next version
        new_version = orchestrator.create_refined_prompt(asset_id, feedback)

        return jsonify({
            'status': 'rejected',
            'asset_id': asset_id,
            'version': version,
            'new_version': new_version,
            'message': 'Asset rejected. Run generation script to create new version.'
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/regenerate', methods=['POST'])
def regenerate_asset():
    """Regenerate asset with custom prompt."""
    data = request.get_json()
    asset_id = data.get('asset_id')
    prompt = data.get('prompt', '')

    if not asset_id:
        return jsonify({'error': 'Missing asset_id'}), 400

    try:
        # Update the prompt
        if prompt.strip():
            orchestrator.set_base_prompt(asset_id, prompt)

        # Get next version number
        new_version = orchestrator.get_next_version(asset_id)

        return jsonify({
            'status': 'ready',
            'asset_id': asset_id,
            'new_version': new_version,
            'message': 'Prompt updated. Run generation script to create new version.'
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/publish', methods=['POST'])
def publish_assets():
    """Publish approved assets to production."""
    data = request.get_json()
    asset_ids = data.get('asset_ids', [])

    if not asset_ids:
        return jsonify({'error': 'No assets specified'}), 400

    try:
        results = orchestrator.batch_publish(asset_ids)

        # Convert to JSON-serializable format
        json_results = []
        for r in results:
            json_results.append({
                'asset_id': r.asset_id,
                'status': r.status,
                'source_path': r.source_path,
                'dest_path': r.dest_path,
                'backup_path': r.backup_path,
                'error_message': r.error_message,
            })

        return jsonify(json_results)
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/stats')
def get_stats():
    """Get dashboard statistics."""
    try:
        stats = orchestrator.get_stats()
        return jsonify({
            'total_assets': stats.total_assets,
            'total_versions': stats.total_versions,
            'pending_review': stats.pending_review,
            'approved': stats.approved,
            'rejected': stats.rejected,
            'published': stats.published,
            'avg_quality_score': stats.avg_quality_score,
            'avg_clarity_score': stats.avg_clarity_score,
            'avg_iterations': stats.avg_iterations,
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500


# ==================== Image Preview ====================

@app.route('/preview/<path:filepath>')
def serve_preview(filepath: str):
    """Serve generated image previews."""
    # Handle both absolute and relative paths
    # Flask strips leading slash, so check if it looks like an absolute path
    if filepath.startswith('home/') or filepath.startswith('Users/'):
        filepath = '/' + filepath

    path = Path(filepath)

    if path.is_absolute():
        if path.exists():
            return send_from_directory(path.parent, path.name)
    else:
        # Try in output directory
        full_path = OUTPUT_DIR / filepath
        if full_path.exists():
            return send_from_directory(full_path.parent, full_path.name)

        # Try relative to assets directory
        assets_dir = Path(__file__).parent
        full_path = assets_dir / filepath
        if full_path.exists():
            return send_from_directory(full_path.parent, full_path.name)

    abort(404)


@app.route('/static/<path:filename>')
def serve_static(filename: str):
    """Serve static files."""
    return send_from_directory(STATIC_DIR, filename)


# ==================== Error Handlers ====================

@app.errorhandler(404)
def not_found(error):
    """Handle 404 errors."""
    return jsonify({'error': 'Not found'}), 404


@app.errorhandler(500)
def server_error(error):
    """Handle 500 errors."""
    return jsonify({'error': 'Internal server error'}), 500


# ==================== Main ====================

def main():
    """Start the review portal server."""
    print(f"""
╔══════════════════════════════════════════════════════════╗
║       HDIM Visual Asset Review Portal                    ║
╠══════════════════════════════════════════════════════════╣
║  Server running at:                                      ║
║  http://{REVIEW_PORTAL_HOST}:{REVIEW_PORTAL_PORT}/                                  ║
║                                                          ║
║  Press Ctrl+C to stop                                    ║
╚══════════════════════════════════════════════════════════╝
    """)

    app.run(
        host=REVIEW_PORTAL_HOST,
        port=REVIEW_PORTAL_PORT,
        debug=DEBUG_MODE,
    )


if __name__ == '__main__':
    main()

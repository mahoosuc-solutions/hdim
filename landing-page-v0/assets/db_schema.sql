-- Database Schema for AI Image Generation Quality Control System
-- SQLite database for tracking image iterations and quality metrics

-- Enable WAL mode for better concurrent access
PRAGMA journal_mode=WAL;

-- Assets table: Stores all generated image versions with quality metrics
CREATE TABLE IF NOT EXISTS assets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id TEXT NOT NULL,                      -- e.g., "HERO-01", "PORTRAIT-MARIA"
    version INTEGER NOT NULL,                     -- 1, 2, 3...
    filepath TEXT NOT NULL,                       -- Path to generated image
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    prompt TEXT,                                  -- The prompt used to generate this version

    -- Quality Metrics
    quality_score REAL,                           -- Overall quality score 0-100
    text_accuracy_status TEXT,                    -- "PASS", "FAIL"
    text_accuracy_details TEXT,                   -- JSON with extracted_text, misspelled
    clarity_score REAL,                           -- 0-100
    clarity_feedback TEXT,                        -- AI-generated feedback
    technical_status TEXT,                        -- "PASS", "FAIL", "WARNING"
    technical_details TEXT,                       -- JSON with dimensions, file_size, format

    -- Review Status
    human_review_status TEXT DEFAULT 'PENDING',   -- "PENDING", "APPROVED", "REJECTED"
    human_feedback TEXT,                          -- Feedback provided during rejection
    reviewed_at TIMESTAMP,                        -- When human review occurred

    -- Publishing Status
    is_published BOOLEAN DEFAULT 0,               -- Whether deployed to production
    published_at TIMESTAMP,                       -- When published

    UNIQUE(asset_id, version)
);

-- Prompts table: Tracks prompt evolution for each asset
CREATE TABLE IF NOT EXISTS prompts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id TEXT NOT NULL UNIQUE,                -- e.g., "HERO-01"
    base_prompt TEXT NOT NULL,                    -- Original prompt from VISUAL_ASSET_PROMPTS.md
    current_iteration_prompt TEXT,                -- Evolved prompt based on feedback
    iteration_count INTEGER DEFAULT 1,            -- Number of iterations
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Quality reports table: Detailed quality reports per generation
CREATE TABLE IF NOT EXISTS quality_reports (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id TEXT NOT NULL,
    version INTEGER NOT NULL,
    report_json TEXT NOT NULL,                    -- Full JSON quality report
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id, version) REFERENCES assets(asset_id, version)
);

-- Feedback table: Store human feedback for learning/analysis
CREATE TABLE IF NOT EXISTS feedback (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id TEXT NOT NULL,
    version INTEGER NOT NULL,
    feedback_type TEXT,                           -- "APPROVAL", "REJECTION", "SUGGESTION"
    feedback_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (asset_id, version) REFERENCES assets(asset_id, version)
);

-- Publishing log table: Track all publishing events
CREATE TABLE IF NOT EXISTS publishing_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    asset_id TEXT NOT NULL,
    version INTEGER NOT NULL,
    source_path TEXT NOT NULL,
    dest_path TEXT NOT NULL,
    backup_path TEXT,
    published_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    published_by TEXT DEFAULT 'system',
    FOREIGN KEY (asset_id, version) REFERENCES assets(asset_id, version)
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_assets_asset_id ON assets(asset_id);
CREATE INDEX IF NOT EXISTS idx_assets_status ON assets(human_review_status);
CREATE INDEX IF NOT EXISTS idx_assets_published ON assets(is_published);
CREATE INDEX IF NOT EXISTS idx_prompts_asset_id ON prompts(asset_id);
CREATE INDEX IF NOT EXISTS idx_quality_reports_asset ON quality_reports(asset_id, version);
CREATE INDEX IF NOT EXISTS idx_feedback_asset ON feedback(asset_id, version);

-- Views for common queries

-- View: Current status of all assets
CREATE VIEW IF NOT EXISTS v_asset_status AS
SELECT
    a.asset_id,
    a.version,
    a.quality_score,
    a.clarity_score,
    a.text_accuracy_status,
    a.human_review_status,
    a.is_published,
    a.generated_at,
    p.iteration_count
FROM assets a
LEFT JOIN prompts p ON a.asset_id = p.asset_id
WHERE a.version = (
    SELECT MAX(version) FROM assets a2 WHERE a2.asset_id = a.asset_id
);

-- View: Assets ready for review (passed QC but not yet human-reviewed)
CREATE VIEW IF NOT EXISTS v_pending_review AS
SELECT
    asset_id,
    version,
    quality_score,
    clarity_score,
    filepath,
    generated_at
FROM assets
WHERE human_review_status = 'PENDING'
AND text_accuracy_status = 'PASS'
AND clarity_score >= 75
ORDER BY generated_at DESC;

-- View: Assets ready for publishing (approved but not published)
CREATE VIEW IF NOT EXISTS v_ready_to_publish AS
SELECT
    asset_id,
    version,
    filepath,
    quality_score,
    reviewed_at
FROM assets
WHERE human_review_status = 'APPROVED'
AND is_published = 0
ORDER BY reviewed_at DESC;

-- View: Quality metrics summary
CREATE VIEW IF NOT EXISTS v_quality_summary AS
SELECT
    asset_id,
    COUNT(*) as total_iterations,
    AVG(quality_score) as avg_quality,
    AVG(clarity_score) as avg_clarity,
    SUM(CASE WHEN text_accuracy_status = 'PASS' THEN 1 ELSE 0 END) as text_pass_count,
    SUM(CASE WHEN human_review_status = 'APPROVED' THEN 1 ELSE 0 END) as approval_count
FROM assets
GROUP BY asset_id;

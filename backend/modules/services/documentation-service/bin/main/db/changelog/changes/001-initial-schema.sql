--liquibase formatted sql

--changeset documentation:001-create-clinical-documents-table
CREATE TABLE IF NOT EXISTS clinical_documents (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    patient_id VARCHAR(100) NOT NULL,
    document_type VARCHAR(100) NOT NULL,
    document_type_code VARCHAR(50),
    document_type_system VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'current',
    title VARCHAR(500),
    description TEXT,
    author_reference VARCHAR(255),
    author_name VARCHAR(255),
    custodian_reference VARCHAR(255),
    document_date TIMESTAMP,
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    encounter_reference VARCHAR(255),
    facility_reference VARCHAR(255),
    fhir_resource JSONB,
    category_codes JSONB,
    security_labels JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    version INTEGER DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_clinical_doc_tenant ON clinical_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_clinical_doc_patient ON clinical_documents(patient_id);
CREATE INDEX IF NOT EXISTS idx_clinical_doc_status ON clinical_documents(status);
CREATE INDEX IF NOT EXISTS idx_clinical_doc_type ON clinical_documents(document_type);
CREATE INDEX IF NOT EXISTS idx_clinical_doc_date ON clinical_documents(document_date);

--changeset documentation:002-create-cda-documents-table
CREATE TABLE IF NOT EXISTS cda_documents (
    id UUID PRIMARY KEY,
    clinical_document_id UUID REFERENCES clinical_documents(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    cda_type VARCHAR(50),
    template_id VARCHAR(100),
    raw_xml TEXT NOT NULL,
    parsed_data JSONB,
    rendered_html TEXT,
    validation_status VARCHAR(50) DEFAULT 'NOT_VALIDATED',
    validation_errors JSONB,
    document_id VARCHAR(100),
    set_id VARCHAR(100),
    version_number INTEGER,
    effective_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cda_doc_clinical ON cda_documents(clinical_document_id);
CREATE INDEX IF NOT EXISTS idx_cda_doc_tenant ON cda_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_cda_doc_type ON cda_documents(cda_type);
CREATE INDEX IF NOT EXISTS idx_cda_doc_validation ON cda_documents(validation_status);

--changeset documentation:003-create-document-attachments-table
CREATE TABLE IF NOT EXISTS document_attachments (
    id UUID PRIMARY KEY,
    clinical_document_id UUID NOT NULL REFERENCES clinical_documents(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255),
    file_size BIGINT,
    storage_path VARCHAR(500),
    storage_type VARCHAR(50) DEFAULT 'LOCAL',
    hash_algorithm VARCHAR(20) DEFAULT 'SHA-256',
    hash_value VARCHAR(128),
    language VARCHAR(10),
    title VARCHAR(255),
    creation_date TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachment_clinical_doc ON document_attachments(clinical_document_id);
CREATE INDEX IF NOT EXISTS idx_attachment_tenant ON document_attachments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_attachment_content_type ON document_attachments(content_type);

--changeset documentation:004-create-document-metadata-table
CREATE TABLE IF NOT EXISTS document_metadata (
    id VARCHAR(100) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    title VARCHAR(500) NOT NULL,
    portal_type VARCHAR(50),
    path VARCHAR(500) UNIQUE,
    category VARCHAR(100) NOT NULL,
    subcategory VARCHAR(100),
    tags TEXT[] NOT NULL,
    related_documents TEXT[] DEFAULT '{}',
    summary TEXT NOT NULL,
    estimated_read_time INTEGER,
    difficulty VARCHAR(20),
    last_updated DATE,
    target_audience TEXT[] NOT NULL,
    access_level VARCHAR(50) NOT NULL,
    owner VARCHAR(100) NOT NULL,
    review_cycle VARCHAR(50) NOT NULL,
    next_review_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'draft',
    version VARCHAR(50) NOT NULL,
    last_reviewed DATE,
    seo_keywords TEXT[],
    external_links JSONB,
    has_video BOOLEAN DEFAULT false,
    video_url VARCHAR(255),
    word_count INTEGER,
    created_date DATE,
    view_count INTEGER DEFAULT 0,
    avg_rating DECIMAL(3, 2),
    feedback_count INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_portal_type ON document_metadata(portal_type);
CREATE INDEX IF NOT EXISTS idx_document_category ON document_metadata(category);
CREATE INDEX IF NOT EXISTS idx_document_status ON document_metadata(status);
CREATE INDEX IF NOT EXISTS idx_document_tenant ON document_metadata(tenant_id);

--changeset documentation:005-create-document-versions-table
CREATE TABLE IF NOT EXISTS document_versions (
    id UUID PRIMARY KEY,
    document_id VARCHAR(100) NOT NULL REFERENCES document_metadata(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    version_number VARCHAR(50) NOT NULL,
    content TEXT,
    change_summary TEXT,
    changed_by VARCHAR(100),
    is_major_version BOOLEAN DEFAULT false,
    is_published BOOLEAN DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_doc_version_document ON document_versions(document_id);
CREATE INDEX IF NOT EXISTS idx_doc_version_tenant ON document_versions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_doc_version_number ON document_versions(document_id, version_number);

--changeset documentation:006-create-document-feedback-table
CREATE TABLE IF NOT EXISTS document_feedback (
    id UUID PRIMARY KEY,
    document_id VARCHAR(100) NOT NULL REFERENCES document_metadata(id) ON DELETE CASCADE,
    tenant_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(100),
    rating INTEGER CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    helpful BOOLEAN,
    feedback_type VARCHAR(50) DEFAULT 'GENERAL',
    status VARCHAR(50) DEFAULT 'PENDING',
    admin_response TEXT,
    responded_by VARCHAR(100),
    responded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_feedback_document ON document_feedback(document_id);
CREATE INDEX IF NOT EXISTS idx_feedback_tenant ON document_feedback(tenant_id);
CREATE INDEX IF NOT EXISTS idx_feedback_user ON document_feedback(user_id);
CREATE INDEX IF NOT EXISTS idx_feedback_rating ON document_feedback(rating);

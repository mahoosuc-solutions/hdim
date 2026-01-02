# Documentation Metadata Schema Specification

**Document Version**: 1.0
**Date**: December 1, 2025
**Owner**: Agent 1 - Documentation Architect
**Status**: Final Specification

---

## Purpose

This document defines the **complete metadata schema** that will be used for all 115 documentation files across the three portals. This schema enables:
- Consistent document categorization and discovery
- Advanced search and filtering
- Content governance and lifecycle management
- Analytics and usage tracking
- Quality assurance and review cycles

---

## Table of Contents

1. [Complete Schema Definition](#complete-schema-definition)
2. [Field Specifications](#field-specifications)
3. [Front Matter Format](#front-matter-format)
4. [Example Metadata](#example-metadata)
5. [Validation Rules](#validation-rules)
6. [Database Schema](#database-schema)

---

## Complete Schema Definition

### Schema Overview

```typescript
interface DocumentMetadata {
  // Core Identifiers (Required)
  id: string;                           // Unique document identifier
  title: string;                        // Display title
  portalType: PortalType;              // Which portal this belongs to
  path: string;                         // Relative path from /docs/

  // Organization (Required)
  category: string;                     // Primary category (from directory)
  subcategory?: string;                 // Optional subcategory
  tags: string[];                       // Searchable keywords (min 3, max 10)
  relatedDocuments: string[];           // Array of related document IDs

  // Content Description (Required)
  summary: string;                      // 50-150 word summary
  estimatedReadTime: number;            // Minutes (calculated: words / 200)
  difficulty?: DifficultyLevel;         // beginner | intermediate | advanced
  lastUpdated: Date;                    // Last modification date

  // Access & Governance (Required)
  targetAudience: string[];             // Roles/personas (min 1)
  accessLevel: AccessLevel;             // public | internal | restricted
  owner: string;                        // Responsible person/team
  reviewCycle: ReviewCycle;             // monthly | quarterly | semi-annual | annual
  nextReviewDate: Date;                 // Calculated from lastUpdated + reviewCycle

  // Status & Versioning (Required)
  status: DocumentStatus;               // draft | published | archived
  version: string;                      // Semantic versioning (1.0, 1.1, 2.0)
  lastReviewed: Date;                   // Date of last content review

  // SEO & Discovery (Optional)
  seoKeywords?: string[];               // Search optimization keywords
  externalLinks?: ExternalLink[];       // Related external resources
  hasVideo?: boolean;                   // Has associated video content
  videoUrl?: string;                    // URL to video if hasVideo=true

  // Content Metrics (Auto-generated)
  wordCount?: number;                   // Total word count
  createdDate?: Date;                   // Initial creation date
  viewCount?: number;                   // Number of views (tracked)
  avgRating?: number;                   // User rating (1-5 scale)
  feedbackCount?: number;               // Number of feedback submissions
}

// Enums and Types
enum PortalType {
  PRODUCT = 'product',
  USER = 'user',
  SALES = 'sales'
}

enum DifficultyLevel {
  BEGINNER = 'beginner',
  INTERMEDIATE = 'intermediate',
  ADVANCED = 'advanced'
}

enum AccessLevel {
  PUBLIC = 'public',           // Accessible to anyone
  INTERNAL = 'internal',       // Requires login
  RESTRICTED = 'restricted'    // Requires specific permissions
}

enum DocumentStatus {
  DRAFT = 'draft',
  PUBLISHED = 'published',
  ARCHIVED = 'archived'
}

enum ReviewCycle {
  MONTHLY = 'monthly',         // 30 days
  QUARTERLY = 'quarterly',     // 90 days
  SEMI_ANNUAL = 'semi-annual', // 180 days
  ANNUAL = 'annual'            // 365 days
}

interface ExternalLink {
  url: string;
  title: string;
  description?: string;
}
```

---

## Field Specifications

### Core Identifiers

#### `id` (string, required)
- **Purpose**: Unique identifier for cross-referencing
- **Format**: `{portal}-{category}-{slug}`
- **Pattern**: `^(product|user|sales)-[a-z0-9]+-[a-z0-9-]+$`
- **Example**: `product-overview-vision`, `user-physician-dashboard`, `sales-tools-demo-script`
- **Validation**: Must be unique across all 115 documents
- **Auto-generated**: Yes (from path)

#### `title` (string, required)
- **Purpose**: Human-readable document title
- **Format**: Title Case
- **Min Length**: 10 characters
- **Max Length**: 100 characters
- **Example**: "Physician Dashboard Guide", "Product Vision and Strategy"
- **Validation**: No special characters except hyphen and parentheses

#### `portalType` (enum, required)
- **Purpose**: Identify which portal this document belongs to
- **Values**: `product` | `user` | `sales`
- **Validation**: Must match directory structure
- **Usage**: Filter documents by portal

#### `path` (string, required)
- **Purpose**: File system location
- **Format**: Relative path from `/docs/`
- **Pattern**: `^(product|user|sales)/[a-z0-9-/]+\.md$`
- **Example**: `product/01-product-overview/vision-and-strategy.md`
- **Validation**: File must exist at this path

---

### Organization

#### `category` (string, required)
- **Purpose**: Primary grouping for navigation
- **Values**: Derived from parent directory name
- **Examples**: `product-overview`, `role-specific-guides`, `sales-enablement`
- **Validation**: Must match directory naming convention
- **Usage**: Navigation hierarchy

#### `subcategory` (string, optional)
- **Purpose**: Secondary grouping (for nested categories)
- **Examples**: `physician`, `care-manager`, `segments`, `use-cases`
- **Validation**: Only use if document is in nested directory
- **Usage**: Refine navigation and filtering

#### `tags` (string[], required)
- **Purpose**: Searchable keywords for discovery
- **Min Count**: 3 tags
- **Max Count**: 10 tags
- **Format**: Lowercase, hyphen-separated
- **Examples**: `["care-gaps", "quality-measures", "physician-workflow"]`
- **Validation**: No duplicates, each tag 2-30 characters
- **Usage**: Search, related content, faceted filtering

#### `relatedDocuments` (string[], required)
- **Purpose**: Link to related documentation
- **Format**: Array of document IDs
- **Min Count**: 0
- **Max Count**: 8
- **Example**: `["user-physician-search", "user-physician-alerts"]`
- **Validation**: All IDs must exist
- **Usage**: "Related Articles" section

---

### Content Description

#### `summary` (string, required)
- **Purpose**: Brief overview for search results and previews
- **Min Length**: 50 words
- **Max Length**: 150 words
- **Format**: Plain text, 2-3 sentences
- **Example**: "This guide explains the physician dashboard, including patient population overview, care gap summary, quality metric trends, and clinical alerts. Learn how to navigate key features and interpret data visualizations."
- **Validation**: Complete sentences, no markdown
- **Usage**: Search results, meta description, preview cards

#### `estimatedReadTime` (number, required)
- **Purpose**: Help users plan time for reading
- **Format**: Integer (minutes)
- **Calculation**: `Math.ceil(wordCount / 200)`
- **Range**: 1-60 minutes
- **Example**: 8 (for 1,600-word document)
- **Validation**: Must be positive integer
- **Usage**: Display on document page

#### `difficulty` (enum, optional)
- **Purpose**: Indicate complexity level for users
- **Values**: `beginner` | `intermediate` | `advanced`
- **Default**: `intermediate` if not specified
- **Usage**: Filter content by skill level
- **Guidelines**:
  - **Beginner**: No prior knowledge required, step-by-step instructions
  - **Intermediate**: Assumes basic system familiarity
  - **Advanced**: Technical depth, assumes expert knowledge

#### `lastUpdated` (Date, required)
- **Purpose**: Track content freshness
- **Format**: ISO 8601 (`YYYY-MM-DD`)
- **Example**: `2025-12-01`
- **Validation**: Cannot be future date
- **Auto-updated**: Yes (on file modification)
- **Usage**: Display "Last updated" badge

---

### Access & Governance

#### `targetAudience` (string[], required)
- **Purpose**: Identify intended readers
- **Format**: Array of role names or personas
- **Min Count**: 1
- **Max Count**: 5
- **Allowed Values**: `["physician", "care-manager", "medical-assistant", "administrator", "executive", "sales-rep", "sales-engineer", "partner", "cio", "cmo", "cfo", "developer"]`
- **Example**: `["physician", "care-manager"]`
- **Validation**: Values must match predefined list
- **Usage**: Role-based filtering

#### `accessLevel` (enum, required)
- **Purpose**: Control document visibility
- **Values**:
  - `public`: Available to anyone (including unauthenticated users)
  - `internal`: Requires authentication (all logged-in users)
  - `restricted`: Requires specific role permissions
- **Default**: `internal` for user docs, `public` for product docs, `restricted` for sales docs
- **Validation**: Must align with portal security policies
- **Usage**: Access control enforcement

#### `owner` (string, required)
- **Purpose**: Identify responsible party for content
- **Format**: Team name or individual name
- **Examples**: `"Product Marketing"`, `"Customer Success"`, `"Sales Operations"`
- **Validation**: Must match organization structure
- **Usage**: Content review assignments

#### `reviewCycle` (enum, required)
- **Purpose**: Define content refresh schedule
- **Values**: `monthly` | `quarterly` | `semi-annual` | `annual`
- **Defaults by Portal**:
  - Product: `quarterly`
  - User: `semi-annual`
  - Sales: `monthly`
- **Validation**: Must be one of allowed values
- **Usage**: Generate review reminders

#### `nextReviewDate` (Date, required)
- **Purpose**: Schedule next content review
- **Format**: ISO 8601 (`YYYY-MM-DD`)
- **Calculation**: `lastReviewed + reviewCycle`
- **Example**: `2026-03-01` (if lastReviewed 2025-12-01, quarterly cycle)
- **Validation**: Must be future date
- **Auto-calculated**: Yes
- **Usage**: Review workflow triggers

---

### Status & Versioning

#### `status` (enum, required)
- **Purpose**: Track document lifecycle state
- **Values**:
  - `draft`: Work in progress, not visible to end users
  - `published`: Live and visible to appropriate audiences
  - `archived`: Deprecated, hidden but retained
- **Default**: `draft` for new documents
- **Validation**: Status transitions must follow workflow
- **Usage**: Filter visible documents

#### `version` (string, required)
- **Purpose**: Track document revisions
- **Format**: Semantic versioning (`major.minor`)
- **Pattern**: `^\d+\.\d+$`
- **Examples**: `1.0`, `1.1`, `2.0`
- **Rules**:
  - Start at `1.0` for new documents
  - Increment minor (1.1, 1.2) for small updates
  - Increment major (2.0, 3.0) for significant rewrites
- **Validation**: Must follow semantic versioning
- **Usage**: Version history tracking

#### `lastReviewed` (Date, required)
- **Purpose**: Track when content was last verified
- **Format**: ISO 8601 (`YYYY-MM-DD`)
- **Example**: `2025-11-15`
- **Validation**: Cannot be future date
- **Usage**: Calculate nextReviewDate

---

### SEO & Discovery (Optional)

#### `seoKeywords` (string[], optional)
- **Purpose**: Improve search engine discoverability
- **Format**: Array of keyword phrases
- **Max Count**: 10
- **Examples**: `["quality measure reporting", "HEDIS diabetes care", "physician workflow optimization"]`
- **Usage**: Meta keywords, search ranking

#### `externalLinks` (ExternalLink[], optional)
- **Purpose**: Reference related external resources
- **Format**: Array of link objects
- **Max Count**: 5
- **Example**:
  ```typescript
  [
    {
      url: "https://www.cms.gov/medicare/quality",
      title: "CMS Quality Measures",
      description: "Official CMS quality measure documentation"
    }
  ]
  ```
- **Validation**: URLs must be valid HTTPS
- **Usage**: "External Resources" section

#### `hasVideo` (boolean, optional)
- **Purpose**: Indicate video content availability
- **Default**: `false`
- **Usage**: Display video icon in search results

#### `videoUrl` (string, optional)
- **Purpose**: Link to associated video content
- **Format**: Valid HTTPS URL
- **Required if**: `hasVideo === true`
- **Example**: `https://www.youtube.com/watch?v=abc123`
- **Validation**: Valid URL format
- **Usage**: Embed video player

---

### Content Metrics (Auto-generated)

#### `wordCount` (number, optional)
- **Purpose**: Track document length
- **Format**: Integer
- **Calculation**: Automated during build
- **Usage**: Calculate estimatedReadTime, analytics

#### `createdDate` (Date, optional)
- **Purpose**: Track original creation
- **Format**: ISO 8601 (`YYYY-MM-DD`)
- **Auto-set**: On first commit
- **Immutable**: Never changes
- **Usage**: Historical tracking

#### `viewCount` (number, optional)
- **Purpose**: Track popularity
- **Format**: Integer
- **Default**: 0
- **Updated**: Real-time via analytics
- **Usage**: "Most popular" features

#### `avgRating` (number, optional)
- **Purpose**: Track user satisfaction
- **Format**: Decimal (1.0 - 5.0)
- **Default**: null (no ratings yet)
- **Calculation**: Average of all user ratings
- **Usage**: Display star rating

#### `feedbackCount` (number, optional)
- **Purpose**: Track engagement
- **Format**: Integer
- **Default**: 0
- **Updated**: When users submit feedback
- **Usage**: Identify high-engagement content

---

## Front Matter Format

### YAML Front Matter

All documents MUST include YAML front matter at the top of the file:

```yaml
---
# Core Identifiers
id: "product-overview-vision"
title: "Product Vision and Strategy"
portalType: "product"
path: "product/01-product-overview/vision-and-strategy.md"

# Organization
category: "product-overview"
subcategory: null
tags:
  - "product-vision"
  - "strategy"
  - "roadmap"
  - "capabilities"
  - "differentiation"
relatedDocuments:
  - "product-overview-capabilities"
  - "product-overview-value-proposition"
  - "product-architecture-system"

# Content Description
summary: "Comprehensive overview of the HealthData in Motion product vision, strategic direction, and long-term roadmap. Explains core differentiators, target market positioning, and key capabilities that drive clinical and operational value."
estimatedReadTime: 10
difficulty: "beginner"
lastUpdated: "2025-12-01"

# Access & Governance
targetAudience:
  - "executive"
  - "cio"
  - "cmo"
  - "evaluator"
owner: "Product Marketing"
reviewCycle: "quarterly"
nextReviewDate: "2026-03-01"
accessLevel: "public"

# Status & Versioning
status: "published"
version: "1.0"
lastReviewed: "2025-12-01"

# SEO & Discovery
seoKeywords:
  - "healthcare quality software"
  - "value-based care platform"
  - "HEDIS quality measures"
  - "FHIR-native architecture"
externalLinks:
  - url: "https://www.cms.gov/medicare/quality"
    title: "CMS Quality Programs"
    description: "Official CMS quality initiative documentation"
hasVideo: false
videoUrl: null

# Content Metrics (auto-generated, do not edit)
wordCount: 2000
createdDate: "2025-11-20"
viewCount: 0
avgRating: null
feedbackCount: 0
---

# Product Vision and Strategy

[Document content begins here...]
```

---

## Example Metadata

### Example 1: Product Documentation

```yaml
---
id: "product-architecture-system"
title: "System Architecture Overview"
portalType: "product"
path: "product/02-architecture/system-architecture.md"

category: "architecture"
subcategory: null
tags:
  - "architecture"
  - "microservices"
  - "modular-monolith"
  - "scalability"
  - "cloud-native"
  - "kubernetes"
relatedDocuments:
  - "product-architecture-integration"
  - "product-architecture-security"
  - "product-implementation-deployment"

summary: "Detailed technical architecture of the HealthData in Motion platform, including modular monolith design, microservices integration, data flow patterns, and scalability approach. Covers Spring Boot services, PostgreSQL database architecture, and containerization strategy."
estimatedReadTime: 15
difficulty: "advanced"
lastUpdated: "2025-12-01"

targetAudience:
  - "cio"
  - "developer"
  - "architect"
owner: "Engineering"
reviewCycle: "quarterly"
nextReviewDate: "2026-03-01"
accessLevel: "public"

status: "published"
version: "1.0"
lastReviewed: "2025-12-01"

seoKeywords:
  - "healthcare system architecture"
  - "FHIR microservices"
  - "modular monolith"
  - "Spring Boot architecture"
externalLinks:
  - url: "https://www.hl7.org/fhir/"
    title: "FHIR R4 Specification"
    description: "Official HL7 FHIR standard documentation"
hasVideo: true
videoUrl: "https://www.youtube.com/watch?v=example123"

wordCount: 3000
createdDate: "2025-11-15"
viewCount: 142
avgRating: 4.5
feedbackCount: 8
---
```

### Example 2: User Documentation

```yaml
---
id: "user-physician-dashboard"
title: "Physician Dashboard Guide"
portalType: "user"
path: "users/02-role-specific-guides/physician/physician-dashboard.md"

category: "role-specific-guides"
subcategory: "physician"
tags:
  - "physician"
  - "dashboard"
  - "population-health"
  - "care-gaps"
  - "quality-metrics"
relatedDocuments:
  - "user-physician-search"
  - "user-physician-care-gap-identification"
  - "user-feature-dashboard"

summary: "Step-by-step guide to navigating the physician dashboard, including patient population overview, care gap summary, quality metric trends, clinical alerts, and key performance indicators. Learn how to interpret visualizations and take action on insights."
estimatedReadTime: 8
difficulty: "beginner"
lastUpdated: "2025-12-01"

targetAudience:
  - "physician"
owner: "Customer Success"
reviewCycle: "semi-annual"
nextReviewDate: "2026-06-01"
accessLevel: "internal"

status: "published"
version: "1.2"
lastReviewed: "2025-12-01"

seoKeywords:
  - "physician dashboard tutorial"
  - "care gap dashboard"
  - "quality measure tracking"
externalLinks: []
hasVideo: true
videoUrl: "https://vimeo.com/example456"

wordCount: 1600
createdDate: "2025-10-01"
viewCount: 523
avgRating: 4.8
feedbackCount: 34
---
```

### Example 3: Sales Documentation

```yaml
---
id: "sales-tools-demo-script"
title: "Product Demo Script Library"
portalType: "sales"
path: "sales/03-sales-tools/demo-script-library.md"

category: "sales-tools"
subcategory: null
tags:
  - "demo"
  - "sales-presentation"
  - "product-showcase"
  - "discovery"
  - "closing"
relatedDocuments:
  - "sales-enablement-playbook"
  - "sales-tools-discovery-questions"
  - "sales-use-case-quality-measures"

summary: "Comprehensive library of product demonstration scripts for various scenarios: executive briefing (15 min), full product demo (45 min), feature-specific demos (10 min each), and use case demonstrations (20 min each). Includes talking points, screen flows, and objection handling."
estimatedReadTime: 12
difficulty: "intermediate"
lastUpdated: "2025-12-01"

targetAudience:
  - "sales-rep"
  - "sales-engineer"
owner: "Sales Operations"
reviewCycle: "monthly"
nextReviewDate: "2026-01-01"
accessLevel: "restricted"

status: "published"
version: "2.1"
lastReviewed: "2025-12-01"

seoKeywords: null
externalLinks: []
hasVideo: false
videoUrl: null

wordCount: 2400
createdDate: "2025-09-15"
viewCount: 89
avgRating: 4.6
feedbackCount: 12
---
```

---

## Validation Rules

### Required Field Validation

**All Documents MUST Have**:
- ✅ `id` (unique, follows pattern)
- ✅ `title` (10-100 chars)
- ✅ `portalType` (product | user | sales)
- ✅ `path` (valid file path)
- ✅ `category` (from directory)
- ✅ `tags` (3-10 tags)
- ✅ `relatedDocuments` (can be empty array)
- ✅ `summary` (50-150 words)
- ✅ `estimatedReadTime` (positive integer)
- ✅ `lastUpdated` (valid date, not future)
- ✅ `targetAudience` (1-5 values from allowed list)
- ✅ `owner` (valid team/person)
- ✅ `reviewCycle` (monthly | quarterly | semi-annual | annual)
- ✅ `nextReviewDate` (future date)
- ✅ `accessLevel` (public | internal | restricted)
- ✅ `status` (draft | published | archived)
- ✅ `version` (semantic version)
- ✅ `lastReviewed` (valid date)

### Business Rule Validation

**Portal-Specific Defaults**:
```javascript
if (portalType === 'product') {
  accessLevel = accessLevel || 'public';
  reviewCycle = reviewCycle || 'quarterly';
}

if (portalType === 'user') {
  accessLevel = accessLevel || 'internal';
  reviewCycle = reviewCycle || 'semi-annual';
}

if (portalType === 'sales') {
  accessLevel = accessLevel || 'restricted';
  reviewCycle = reviewCycle || 'monthly';
}
```

**Status Transitions**:
- `draft` → `published` (requires approval)
- `published` → `archived` (allowed)
- `archived` → `published` (requires re-approval)
- Cannot skip states (draft → archived not allowed)

**Version Incrementing**:
- Minor content updates: increment minor version (1.0 → 1.1)
- Major rewrites: increment major version (1.9 → 2.0)
- Archived documents cannot have version changes

### Data Quality Validation

**Automated Checks**:
```javascript
// ID uniqueness
validateUniqueId(metadata.id);

// Path exists
validateFileExists(metadata.path);

// Related documents exist
metadata.relatedDocuments.forEach(id => {
  validateDocumentExists(id);
});

// Date logic
validateDate(metadata.lastUpdated <= Date.now());
validateDate(metadata.nextReviewDate > Date.now());
validateDate(metadata.createdDate <= metadata.lastUpdated);

// Word count matches
validateWordCount(metadata.wordCount === countWords(fileContent));

// Estimated read time calculation
validateReadTime(metadata.estimatedReadTime === Math.ceil(metadata.wordCount / 200));

// Target audience values
metadata.targetAudience.forEach(audience => {
  validateAudience(audience, ALLOWED_AUDIENCES);
});

// Tags format
metadata.tags.forEach(tag => {
  validateTagFormat(tag); // lowercase, hyphen-separated, 2-30 chars
});

// External links valid
metadata.externalLinks?.forEach(link => {
  validateURL(link.url, { protocol: 'https' });
});

// Video URL required if hasVideo
if (metadata.hasVideo && !metadata.videoUrl) {
  throw new Error('videoUrl required when hasVideo is true');
}
```

---

## Database Schema

### PostgreSQL Schema

```sql
CREATE TABLE document_metadata (
  -- Core Identifiers
  id VARCHAR(100) PRIMARY KEY,
  title VARCHAR(100) NOT NULL,
  portal_type VARCHAR(10) NOT NULL CHECK (portal_type IN ('product', 'user', 'sales')),
  path VARCHAR(255) NOT NULL UNIQUE,

  -- Organization
  category VARCHAR(50) NOT NULL,
  subcategory VARCHAR(50),
  tags TEXT[] NOT NULL,
  related_documents TEXT[] NOT NULL DEFAULT '{}',

  -- Content Description
  summary TEXT NOT NULL,
  estimated_read_time INTEGER NOT NULL CHECK (estimated_read_time > 0),
  difficulty VARCHAR(20) CHECK (difficulty IN ('beginner', 'intermediate', 'advanced')),
  last_updated DATE NOT NULL,

  -- Access & Governance
  target_audience TEXT[] NOT NULL,
  access_level VARCHAR(20) NOT NULL CHECK (access_level IN ('public', 'internal', 'restricted')),
  owner VARCHAR(100) NOT NULL,
  review_cycle VARCHAR(20) NOT NULL CHECK (review_cycle IN ('monthly', 'quarterly', 'semi-annual', 'annual')),
  next_review_date DATE NOT NULL,

  -- Status & Versioning
  status VARCHAR(20) NOT NULL CHECK (status IN ('draft', 'published', 'archived')),
  version VARCHAR(10) NOT NULL,
  last_reviewed DATE NOT NULL,

  -- SEO & Discovery
  seo_keywords TEXT[],
  external_links JSONB,
  has_video BOOLEAN DEFAULT FALSE,
  video_url VARCHAR(255),

  -- Content Metrics
  word_count INTEGER,
  created_date DATE,
  view_count INTEGER DEFAULT 0,
  avg_rating DECIMAL(2, 1) CHECK (avg_rating >= 1.0 AND avg_rating <= 5.0),
  feedback_count INTEGER DEFAULT 0,

  -- Constraints
  CONSTRAINT valid_dates CHECK (last_updated <= CURRENT_DATE AND next_review_date > CURRENT_DATE),
  CONSTRAINT video_url_required CHECK (NOT has_video OR video_url IS NOT NULL),
  CONSTRAINT tags_count CHECK (array_length(tags, 1) >= 3 AND array_length(tags, 1) <= 10),
  CONSTRAINT audience_count CHECK (array_length(target_audience, 1) >= 1 AND array_length(target_audience, 1) <= 5)
);

-- Indexes for performance
CREATE INDEX idx_portal_type ON document_metadata(portal_type);
CREATE INDEX idx_category ON document_metadata(category);
CREATE INDEX idx_status ON document_metadata(status);
CREATE INDEX idx_tags ON document_metadata USING GIN(tags);
CREATE INDEX idx_target_audience ON document_metadata USING GIN(target_audience);
CREATE INDEX idx_next_review_date ON document_metadata(next_review_date);
CREATE INDEX idx_view_count ON document_metadata(view_count DESC);
CREATE INDEX idx_avg_rating ON document_metadata(avg_rating DESC NULLS LAST);

-- Full-text search index
CREATE INDEX idx_fulltext ON document_metadata USING GIN(
  to_tsvector('english', title || ' ' || summary)
);
```

### External Links Table (Normalized)

```sql
CREATE TABLE document_external_links (
  id SERIAL PRIMARY KEY,
  document_id VARCHAR(100) REFERENCES document_metadata(id) ON DELETE CASCADE,
  url VARCHAR(500) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT,
  created_at TIMESTAMP DEFAULT NOW(),

  CONSTRAINT valid_https CHECK (url LIKE 'https://%')
);

CREATE INDEX idx_external_links_document ON document_external_links(document_id);
```

### Ratings Table (for tracking individual ratings)

```sql
CREATE TABLE document_ratings (
  id SERIAL PRIMARY KEY,
  document_id VARCHAR(100) REFERENCES document_metadata(id) ON DELETE CASCADE,
  user_id VARCHAR(100),
  rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
  comment TEXT,
  created_at TIMESTAMP DEFAULT NOW(),

  UNIQUE(document_id, user_id)
);

CREATE INDEX idx_ratings_document ON document_ratings(document_id);

-- Trigger to update avg_rating in document_metadata
CREATE OR REPLACE FUNCTION update_avg_rating()
RETURNS TRIGGER AS $$
BEGIN
  UPDATE document_metadata
  SET avg_rating = (
    SELECT AVG(rating)::DECIMAL(2,1)
    FROM document_ratings
    WHERE document_id = NEW.document_id
  ),
  feedback_count = (
    SELECT COUNT(*)
    FROM document_ratings
    WHERE document_id = NEW.document_id
  )
  WHERE id = NEW.document_id;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_avg_rating
AFTER INSERT OR UPDATE ON document_ratings
FOR EACH ROW
EXECUTE FUNCTION update_avg_rating();
```

---

## Metadata Extraction & Sync

### Build-Time Extraction

```javascript
// metadata-extractor.js
const fs = require('fs');
const matter = require('gray-matter');
const glob = require('glob');

function extractMetadata() {
  const files = glob.sync('docs/**/*.md');
  const metadata = [];

  files.forEach(filePath => {
    const content = fs.readFileSync(filePath, 'utf8');
    const { data, content: markdownContent } = matter(content);

    // Validate required fields
    validateRequiredFields(data);

    // Auto-calculate fields
    data.wordCount = countWords(markdownContent);
    data.estimatedReadTime = Math.ceil(data.wordCount / 200);

    // Validate business rules
    validateBusinessRules(data);

    metadata.push(data);
  });

  // Write to database or JSON
  saveMetadata(metadata);
}
```

### Sync Strategy

1. **Build Time**: Extract metadata from all files, validate, store in database
2. **Run Time**: Query database for fast searches and filtering
3. **Update Time**: Re-extract and sync when files change (CI/CD hook)
4. **Manual Trigger**: Admin can force metadata re-sync

---

## Document Control

**Version**: 1.0
**Status**: Final Specification
**Approved By**: Agent 1 - Documentation Architect
**Date**: December 1, 2025

**Change Log**:
| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-12-01 | Initial metadata schema specification |

---

**Next Steps**:
1. Review and approve metadata schema
2. Implement database tables
3. Create metadata extraction script
4. Add front matter to all template files
5. Hand off to Agent 5 for portal integration

/**
 * CQL Library Model - Matches backend CqlLibrary entity
 */
export interface CqlLibrary {
  id: string; // UUID
  tenantId: string;
  name: string;
  libraryName: string; // Same as name
  version: string; // Semantic versioning: "1.0.0"
  status: LibraryStatus;
  cqlContent?: string; // CQL source code
  elmJson?: string; // Compiled ELM (JSON)
  elmXml?: string; // Compiled ELM (XML)
  description?: string;
  publisher?: string;
  fhirLibraryId?: string; // UUID
  active: boolean;
  createdAt: string; // ISO date string
  updatedAt: string; // ISO date string
  createdBy: string;
}

export type LibraryStatus = 'DRAFT' | 'ACTIVE' | 'RETIRED';

/**
 * CQL Library Request DTO - For creating/updating libraries
 */
export interface CqlLibraryRequest {
  name: string; // Required, 1-255 chars
  version: string; // Required, format: "\\d+\\.\\d+\\.\\d+"
  cqlContent: string; // Required
  status?: LibraryStatus; // Optional
  description?: string; // Optional, max 4000 chars
  publisher?: string; // Optional, max 255 chars
}

/**
 * Simplified measure info for dropdowns and lists
 */
export interface MeasureInfo {
  id: string;
  name: string;
  version: string;
  description?: string;
  category?: string; // HEDIS, CMS, CUSTOM, PREVENTIVE, CHRONIC_DISEASE, etc.
  displayName: string; // e.g., "HEDIS-CDC v1.0.0 - Diabetes: HbA1c Control"
}

/**
 * HEDIS Measure Info - from /evaluate/measures endpoint
 * Represents a registered Java measure implementation
 */
export interface HedisMeasureInfo {
  measureId: string; // e.g., "CDC", "CBP", "BCS"
  measureName: string; // e.g., "Comprehensive Diabetes Care"
  version: string; // e.g., "2024"
  implementationClass: string; // e.g., "CDCMeasure"
}

/**
 * Response from /evaluate/measures endpoint
 */
export interface HedisMeasuresResponse {
  count: number;
  measures: HedisMeasureInfo[];
}

/**
 * Measure category for filtering
 */
export type MeasureCategory =
  | 'PREVENTIVE'
  | 'CHRONIC_DISEASE'
  | 'BEHAVIORAL_HEALTH'
  | 'MEDICATION'
  | 'WOMENS_HEALTH'
  | 'CHILD_ADOLESCENT'
  | 'SDOH'
  | 'UTILIZATION'
  | 'CARE_COORDINATION'
  | 'OVERUSE'
  | 'CUSTOM';

/**
 * Local Measure Metadata - from /local-measures endpoint
 * Represents locally calculated measure implementations
 */
export interface LocalMeasureMetadata {
  measureId: string; // e.g., "CDC", "CBP", "BCS"
  measureName: string; // e.g., "Comprehensive Diabetes Care"
  version: string; // e.g., "1.0.0"
  description?: string;
  category?: MeasureCategory;
}

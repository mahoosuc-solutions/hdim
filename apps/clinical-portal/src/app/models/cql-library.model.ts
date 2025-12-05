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
  category?: string; // HEDIS, CMS, CUSTOM
  displayName: string; // e.g., "HEDIS-CDC v1.0.0 - Diabetes: HbA1c Control"
}

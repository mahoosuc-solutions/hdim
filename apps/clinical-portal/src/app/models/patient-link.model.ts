/**
 * Patient Master Index (MPI) Models
 *
 * Supports patient deduplication and master patient record management
 */

/**
 * Link Type for Patient Record Relationships
 */
export enum PatientLinkType {
  /** This patient record is replaced by another master record */
  REPLACED_BY = 'replaced-by',

  /** This patient record replaces another record */
  REPLACES = 'replaces',

  /** This patient record refers to the same patient as another record */
  SEE_ALSO = 'seealso',

  /** This patient record is the master/golden record */
  MASTER = 'master'
}

/**
 * Link between patient records for deduplication
 */
export interface PatientLink {
  /** Patient ID that is linked */
  targetPatientId: string;

  /** Type of relationship */
  type: PatientLinkType;

  /** Confidence score (0-1) for automated matching */
  confidenceScore?: number;

  /** Whether link was manually verified */
  verified?: boolean;

  /** Timestamp when link was created */
  createdAt?: string;

  /** User who created the link */
  createdBy?: string;
}

/**
 * Extended Patient Summary with MPI information
 */
export interface PatientSummaryWithLinks {
  id: string;
  mrn?: string;
  mrnAssigningAuthority?: string;
  fullName: string;
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string;
  age?: number;
  gender?: string;
  status: 'Active' | 'Inactive';
  lastEvaluationDate?: string;

  // MPI Extensions
  /** Whether this is a master record */
  isMaster: boolean;

  /** ID of the master record if this is a duplicate */
  masterPatientId?: string;

  /** Links to other patient records */
  links?: PatientLink[];

  /** IDs of duplicate records if this is a master */
  duplicateIds?: string[];

  /** Count of linked duplicate records */
  duplicateCount?: number;

  /** Match score for potential duplicates (0-100) */
  matchScore?: number;

  /** Whether this record is marked as a potential duplicate */
  isPotentialDuplicate?: boolean;
}

/**
 * Potential duplicate match
 */
export interface PotentialDuplicateMatch {
  /** First patient */
  patient1: PatientSummaryWithLinks;

  /** Second patient */
  patient2: PatientSummaryWithLinks;

  /** Match score (0-100) */
  matchScore: number;

  /** Matching fields */
  matchingFields: {
    name?: boolean;
    dateOfBirth?: boolean;
    gender?: boolean;
    mrn?: boolean;
    address?: boolean;
    phone?: boolean;
  };

  /** Suggested action */
  suggestedAction: 'merge' | 'link' | 'review' | 'ignore';
}

/**
 * Patient merge request
 */
export interface PatientMergeRequest {
  /** Master patient ID (survivor) */
  masterPatientId: string;

  /** Duplicate patient ID(s) to merge */
  duplicatePatientIds: string[];

  /** Strategy for merging */
  mergeStrategy: 'keep-master' | 'keep-all' | 'manual';

  /** User comments */
  comment?: string;
}

/**
 * Patient merge result
 */
export interface PatientMergeResult {
  /** Merged master patient ID */
  masterPatientId: string;

  /** IDs of patients that were merged */
  mergedPatientIds: string[];

  /** Whether merge was successful */
  success: boolean;

  /** Error message if failed */
  error?: string;

  /** Timestamp of merge */
  mergedAt?: string;
}

/**
 * Patient deduplication statistics
 */
export interface DeduplicationStatistics {
  /** Total number of patients */
  totalPatients: number;

  /** Number of master records */
  masterRecords: number;

  /** Number of duplicate records */
  duplicateRecords: number;

  /** Number of unlinked records */
  unlinkedRecords: number;

  /** Number of potential duplicates requiring review */
  potentialDuplicates: number;

  /** Average duplicate count per master */
  averageDuplicatesPerMaster: number;
}

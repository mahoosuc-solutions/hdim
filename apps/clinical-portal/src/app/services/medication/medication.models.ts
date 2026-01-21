/**
 * Medication Management Domain Models
 *
 * Interfaces and enums for medication operations including:
 * - Medication inventory and catalog management
 * - Medication orders and fulfillment
 * - Adverse events and allergies
 * - Pharmacy coordination
 *
 * Aligns with backend medication-service data structures.
 */

// ==================== Medication Catalog Models ====================

/**
 * Medication route of administration
 */
export enum MedicationRoute {
  ORAL = 'ORAL',
  INTRAVENOUS = 'INTRAVENOUS',
  INTRAMUSCULAR = 'INTRAMUSCULAR',
  SUBCUTANEOUS = 'SUBCUTANEOUS',
  TOPICAL = 'TOPICAL',
  INHALATION = 'INHALATION',
  TRANSDERMAL = 'TRANSDERMAL',
  RECTAL = 'RECTAL',
  NASAL = 'NASAL',
  OPHTHALMIC = 'OPHTHALMIC',
  OTIC = 'OTIC',
}

/**
 * Medication dosage unit
 */
export enum DosageUnit {
  MG = 'MG',
  G = 'G',
  MCG = 'MCG',
  ML = 'ML',
  UNIT = 'UNIT',
  MEQ = 'MEQ',
  MMOL = 'MMOL',
  KCAL = 'KCAL',
}

/**
 * Medication frequency
 */
export enum MedicationFrequency {
  ONCE_DAILY = 'ONCE_DAILY',
  TWICE_DAILY = 'TWICE_DAILY',
  THREE_TIMES_DAILY = 'THREE_TIMES_DAILY',
  FOUR_TIMES_DAILY = 'FOUR_TIMES_DAILY',
  EVERY_SIX_HOURS = 'EVERY_SIX_HOURS',
  EVERY_EIGHT_HOURS = 'EVERY_EIGHT_HOURS',
  EVERY_TWELVE_HOURS = 'EVERY_TWELVE_HOURS',
  AS_NEEDED = 'AS_NEEDED',
  ONCE_WEEKLY = 'ONCE_WEEKLY',
  TWICE_WEEKLY = 'TWICE_WEEKLY',
  ONCE_MONTHLY = 'ONCE_MONTHLY',
}

/**
 * Medication form/formulation
 */
export enum MedicationForm {
  TABLET = 'TABLET',
  CAPSULE = 'CAPSULE',
  LIQUID = 'LIQUID',
  INJECTION = 'INJECTION',
  INHALER = 'INHALER',
  PATCH = 'PATCH',
  POWDER = 'POWDER',
  CREAM = 'CREAM',
  OINTMENT = 'OINTMENT',
  GEL = 'GEL',
}

/**
 * Medication catalog entry
 */
export interface Medication {
  id?: string;
  tenantId?: string;
  name: string;
  genericName?: string;
  therapeuticClass: string;
  form: MedicationForm;
  strength: string;
  strengthUnit: DosageUnit;
  ndc?: string; // National Drug Code
  rxNorm?: string; // RxNorm identifier
  manufacturer?: string;
  activeIngredients?: string[];
  contraindications?: string[];
  sideEffects?: string[];
  drugInteractions?: string[];
  maxDailyDose?: number;
  maxDailyDoseUnit?: DosageUnit;
  isControlledSubstance: boolean;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

/**
 * Medication order/prescription
 */
export interface MedicationOrder {
  id?: string;
  patientId: string;
  tenantId?: string;
  prescriberId: string;
  medicationId: string;
  medication?: Medication;
  dosage: number;
  dosageUnit: DosageUnit;
  route: MedicationRoute;
  frequency: MedicationFrequency;
  startDate: Date | string;
  endDate?: Date | string;
  quantity?: number;
  refills?: number;
  refillsRemaining?: number;
  prescriptionStatus: PrescriptionStatus;
  priorityLevel: PriorityLevel;
  indication?: string;
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

/**
 * Prescription status
 */
export enum PrescriptionStatus {
  DRAFT = 'DRAFT',
  PENDING_REVIEW = 'PENDING_REVIEW',
  APPROVED = 'APPROVED',
  SENT_TO_PHARMACY = 'SENT_TO_PHARMACY',
  FILLED = 'FILLED',
  PARTIALLY_FILLED = 'PARTIALLY_FILLED',
  NOT_FILLED = 'NOT_FILLED',
  SUSPENDED = 'SUSPENDED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
}

/**
 * Priority level for medication order
 */
export enum PriorityLevel {
  ROUTINE = 'ROUTINE',
  URGENT = 'URGENT',
  STAT = 'STAT',
}

// ==================== Medication Fulfillment Models ====================

/**
 * Pharmacy fulfillment status
 */
export enum FulfillmentStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  READY_FOR_PICKUP = 'READY_FOR_PICKUP',
  READY_FOR_DELIVERY = 'READY_FOR_DELIVERY',
  DISPATCHED = 'DISPATCHED',
  DELIVERED = 'DELIVERED',
  PICKED_UP = 'PICKED_UP',
  NOT_IN_STOCK = 'NOT_IN_STOCK',
  UNABLE_TO_FILL = 'UNABLE_TO_FILL',
  CANCELLED = 'CANCELLED',
}

/**
 * Pharmacy fulfillment order
 */
export interface PharmacyFulfillment {
  id?: string;
  prescriptionId: string;
  tenantId?: string;
  pharmacyId: string;
  pharmacyName?: string;
  fulfillmentStatus: FulfillmentStatus;
  quantityRequested: number;
  quantityFulfilled?: number;
  estimatedReadyDate?: Date | string;
  actualReadyDate?: Date | string;
  deliveryMethod?: 'PICKUP' | 'DELIVERY' | 'MAIL';
  deliveryAddress?: string;
  trackingNumber?: string;
  pharmacistNotes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Adverse Event Models ====================

/**
 * Adverse event severity
 */
export enum AdverseEventSeverity {
  MILD = 'MILD',
  MODERATE = 'MODERATE',
  SEVERE = 'SEVERE',
  LIFE_THREATENING = 'LIFE_THREATENING',
}

/**
 * Adverse event type
 */
export enum AdverseEventType {
  ALLERGY = 'ALLERGY',
  SIDE_EFFECT = 'SIDE_EFFECT',
  DRUG_INTERACTION = 'DRUG_INTERACTION',
  CONTRAINDICATION = 'CONTRAINDICATION',
  INTOLERANCE = 'INTOLERANCE',
}

/**
 * Patient medication adverse event/allergy
 */
export interface MedicationAdverseEvent {
  id?: string;
  patientId: string;
  tenantId?: string;
  medicationId?: string;
  medicationName: string;
  eventType: AdverseEventType;
  severity: AdverseEventSeverity;
  reactionDescription: string;
  onsetDate: Date | string;
  resolvedDate?: Date | string;
  reportingClinician?: string;
  verificationStatus: 'UNVERIFIED' | 'VERIFIED' | 'REFUTED';
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Medication Administration Models ====================

/**
 * Medication administration status
 */
export enum AdministrationStatus {
  SCHEDULED = 'SCHEDULED',
  ADMINISTERING = 'ADMINISTERING',
  ADMINISTERED = 'ADMINISTERED',
  NOT_ADMINISTERED = 'NOT_ADMINISTERED',
  REFUSED = 'REFUSED',
  HELD = 'HELD',
}

/**
 * Medication administration record
 */
export interface MedicationAdministration {
  id?: string;
  orderId: string;
  patientId: string;
  tenantId?: string;
  administeredBy?: string;
  status: AdministrationStatus;
  scheduledTime: Date | string;
  administeredTime?: Date | string;
  dosageAdministered?: number;
  dosageUnit?: DosageUnit;
  route?: MedicationRoute;
  site?: string; // For injections
  lotNumber?: string;
  expirationDate?: Date | string;
  notes?: string;
  reasonNotAdministered?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Medication Interaction Models ====================

/**
 * Drug interaction severity
 */
export enum InteractionSeverity {
  MINOR = 'MINOR',
  MODERATE = 'MODERATE',
  MAJOR = 'MAJOR',
}

/**
 * Drug interaction check result
 */
export interface DrugInteraction {
  id?: string;
  medicationId1: string;
  medicationId2: string;
  tenantId?: string;
  severity: InteractionSeverity;
  description: string;
  management?: string;
  evidence?: string;
  createdAt?: Date | string;
}

/**
 * Medication interaction check request/result
 */
export interface MedicationInteractionCheck {
  patientId: string;
  tenantId?: string;
  currentMedications: string[]; // Medication IDs
  newMedicationId: string;
  interactions: DrugInteraction[];
  hasSignificantInteractions: boolean;
  recommendations?: string;
}

// ==================== Medication Metrics Models ====================

/**
 * Medication compliance/adherence metrics
 */
export interface MedicationAdherenceMetrics {
  patientId: string;
  totalOrders: number;
  filledOrders: number;
  adherenceRate: number; // percentage
  missedDoses?: number;
  refillsOnTime: number;
  refillsLate: number;
  refillsEarly: number;
  lastFillDate?: Date;
  nextExpectedFillDate?: Date;
}

/**
 * Medication therapy management metrics
 */
export interface MedicationTherapyMetrics {
  patientId: string;
  totalActiveMedications: number;
  highRiskMedications: number;
  potentialDrugInteractions: number;
  medicationsWithDuplicateTherapy: number;
  medicationsAwaitingReview: number;
  lastReviewDate?: Date;
}

// ==================== Pharmacy Coordination Models ====================

/**
 * Pharmacy information
 */
export interface Pharmacy {
  id?: string;
  tenantId?: string;
  name: string;
  type: 'RETAIL' | 'MAIL_ORDER' | 'SPECIALTY' | 'CLINIC';
  npi?: string;
  phoneNumber?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  hours?: string;
  accepts24HourOrders: boolean;
  acceptsDelivery: boolean;
  isPreferred: boolean;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

/**
 * Pharmacy coordination request
 */
export interface PharmacyCoordination {
  id?: string;
  prescriptionId: string;
  patientId: string;
  tenantId?: string;
  coordinatorId: string;
  pharmacyId: string;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'ESCALATED';
  requestType: 'PRIOR_AUTH' | 'QUANTITY_OVERRIDE' | 'FORMULARY_EXCEPTION' | 'REFILL_AUTHORIZATION';
  requestDetails?: string;
  pharmacyResponse?: string;
  resolvedAt?: Date | string;
  notes?: string;
  createdAt?: Date | string;
  updatedAt?: Date | string;
}

// ==================== Unified Medication Context ====================

/**
 * Aggregated medication context for clinical dashboard
 */
export interface MedicationContext {
  patientId: string;
  patientName: string;
  clinicianId: string;

  // Active Medications
  activeMedications: {
    orders: MedicationOrder[];
    totalCount: number;
    highRiskCount: number;
  };

  // Pending Orders
  pendingOrders: {
    orders: MedicationOrder[];
    totalCount: number;
    awaitingPharmacy: number;
  };

  // Fulfillment Status
  fulfillments: {
    pending: PharmacyFulfillment[];
    readyForPickup: PharmacyFulfillment[];
    delivered: PharmacyFulfillment[];
  };

  // Adverse Events
  adverseEvents: {
    allergies: MedicationAdverseEvent[];
    sideEffects: MedicationAdverseEvent[];
    interactions: MedicationAdverseEvent[];
  };

  // Administration
  administration: {
    scheduledToday: MedicationAdministration[];
    administeredToday: MedicationAdministration[];
    missedToday: MedicationAdministration[];
  };

  // Metrics
  adherenceMetrics: MedicationAdherenceMetrics;
  therapyMetrics: MedicationTherapyMetrics;

  lastUpdated: Date;
}

/**
 * Learning barriers that may affect medication education
 */
export interface EducationBarriers {
  healthLiteracy?: boolean;
  language?: boolean;
  cognitive?: boolean;
  emotional?: boolean;
  visual?: boolean;
  hearing?: boolean;
}

/**
 * Pagination wrapper for list responses
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

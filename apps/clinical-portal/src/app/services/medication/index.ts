/**
 * Medication Services Module - Public API
 *
 * Exports all medication services and domain models
 * for easy consumption by components and other services.
 *
 * Usage:
 * import { MedicationService } from '@app/services/medication';
 * import { Medication, MedicationOrder, ... } from '@app/services/medication';
 */

// Service exports
export { MedicationService, MEDICATION_BASE_URL } from './medication.service';

// Enum exports (values)
export {
  // Medication Catalog
  MedicationRoute,
  DosageUnit,
  MedicationFrequency,
  MedicationForm,
  // Prescription
  PrescriptionStatus,
  PriorityLevel,
  // Fulfillment
  FulfillmentStatus,
  // Adverse Events
  AdverseEventSeverity,
  AdverseEventType,
  // Administration
  AdministrationStatus,
  // Interactions
  InteractionSeverity,
} from './medication.models';

// Type exports (interfaces)
export type {
  Medication,
  MedicationOrder,
  PharmacyFulfillment,
  MedicationAdverseEvent,
  MedicationAdministration,
  DrugInteraction,
  MedicationInteractionCheck,
  MedicationAdherenceMetrics,
  MedicationTherapyMetrics,
  Pharmacy,
  PharmacyCoordination,
  MedicationContext,
  EducationBarriers,
  PaginatedResponse,
} from './medication.models';

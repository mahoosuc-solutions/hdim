import { createAction, props } from '@ngrx/store';
import { Patient } from '../../models/patient.model';

/**
 * Patient Actions - NgRx actions for patient state management
 *
 * Action Types:
 * - Load patients list
 * - Load single patient
 * - Create new patient
 * - Update existing patient
 * - Delete patient
 * - Search patients
 */

// Load Patients
export const loadPatients = createAction(
  '[Patient] Load Patients',
  props<{ count?: number }>()
);

export const loadPatientsSuccess = createAction(
  '[Patient] Load Patients Success',
  props<{ patients: Patient[] }>()
);

export const loadPatientsFailure = createAction(
  '[Patient] Load Patients Failure',
  props<{ error: string }>()
);

// Load Single Patient
export const loadPatient = createAction(
  '[Patient] Load Patient',
  props<{ id: string }>()
);

export const loadPatientSuccess = createAction(
  '[Patient] Load Patient Success',
  props<{ patient: Patient }>()
);

export const loadPatientFailure = createAction(
  '[Patient] Load Patient Failure',
  props<{ error: string }>()
);

// Create Patient
export const createPatient = createAction(
  '[Patient] Create Patient',
  props<{ patient: Patient }>()
);

export const createPatientSuccess = createAction(
  '[Patient] Create Patient Success',
  props<{ patient: Patient }>()
);

export const createPatientFailure = createAction(
  '[Patient] Create Patient Failure',
  props<{ error: string }>()
);

// Update Patient
export const updatePatient = createAction(
  '[Patient] Update Patient',
  props<{ id: string; patient: Patient }>()
);

export const updatePatientSuccess = createAction(
  '[Patient] Update Patient Success',
  props<{ patient: Patient }>()
);

export const updatePatientFailure = createAction(
  '[Patient] Update Patient Failure',
  props<{ error: string }>()
);

// Delete Patient
export const deletePatient = createAction(
  '[Patient] Delete Patient',
  props<{ id: string }>()
);

export const deletePatientSuccess = createAction(
  '[Patient] Delete Patient Success',
  props<{ id: string }>()
);

export const deletePatientFailure = createAction(
  '[Patient] Delete Patient Failure',
  props<{ error: string }>()
);

// Search Patients
export const searchPatients = createAction(
  '[Patient] Search Patients',
  props<{ params: SearchParams }>()
);

export const searchPatientsSuccess = createAction(
  '[Patient] Search Patients Success',
  props<{ patients: Patient[] }>()
);

export const searchPatientsFailure = createAction(
  '[Patient] Search Patients Failure',
  props<{ error: string }>()
);

// Select Patient
export const selectPatient = createAction(
  '[Patient] Select Patient',
  props<{ patient: Patient | null }>()
);

// Clear Selected Patient
export const clearSelectedPatient = createAction(
  '[Patient] Clear Selected Patient'
);

// Clear Patients
export const clearPatients = createAction('[Patient] Clear Patients');

export interface SearchParams {
  name?: string;
  identifier?: string;
  birthdate?: string;
  gender?: string;
}

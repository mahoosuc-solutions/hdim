import { createFeatureSelector, createSelector } from '@ngrx/store';
import { PatientState } from '../reducers/patient.reducer';

/**
 * Patient Selectors - Memoized selectors for patient state
 */

// Feature selector
export const selectPatientState = createFeatureSelector<PatientState>('patient');

// Select all patients
export const selectAllPatients = createSelector(
  selectPatientState,
  (state: PatientState) => state.patients
);

// Select current patient
export const selectCurrentPatient = createSelector(
  selectPatientState,
  (state: PatientState) => state.currentPatient
);

// Select selected patient
export const selectSelectedPatient = createSelector(
  selectPatientState,
  (state: PatientState) => state.selectedPatient
);

// Select search results
export const selectSearchResults = createSelector(
  selectPatientState,
  (state: PatientState) => state.searchResults
);

// Select loading state
export const selectLoading = createSelector(
  selectPatientState,
  (state: PatientState) => state.loading
);

// Select error
export const selectError = createSelector(
  selectPatientState,
  (state: PatientState) => state.error
);

// Select loading patient state
export const selectLoadingPatient = createSelector(
  selectPatientState,
  (state: PatientState) => state.loadingPatient
);

// Select patient error
export const selectPatientError = createSelector(
  selectPatientState,
  (state: PatientState) => state.patientError
);

// Select patient by ID
export const selectPatientById = (id: string) =>
  createSelector(selectAllPatients, (patients) => patients.find((p) => p.id === id));

// Select patients count
export const selectPatientsCount = createSelector(
  selectAllPatients,
  (patients) => patients.length
);

// Select active patients (assuming there's an 'active' field)
export const selectActivePatients = createSelector(selectAllPatients, (patients) =>
  patients.filter((p) => p.active !== false)
);

// Select whether there are any patients
export const selectHasPatients = createSelector(
  selectAllPatients,
  (patients) => patients.length > 0
);

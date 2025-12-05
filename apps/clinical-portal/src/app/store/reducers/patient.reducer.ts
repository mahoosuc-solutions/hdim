import { createReducer, on } from '@ngrx/store';
import { Patient } from '../../models/patient.model';
import * as PatientActions from '../actions/patient.actions';

/**
 * Patient State Interface
 */
export interface PatientState {
  patients: Patient[];
  currentPatient: Patient | null;
  selectedPatient: Patient | null;
  searchResults: Patient[];
  loading: boolean;
  error: string | null;
  loadingPatient: boolean;
  patientError: string | null;
}

/**
 * Initial State
 */
export const initialState: PatientState = {
  patients: [],
  currentPatient: null,
  selectedPatient: null,
  searchResults: [],
  loading: false,
  error: null,
  loadingPatient: false,
  patientError: null,
};

/**
 * Patient Reducer
 */
export const patientReducer = createReducer(
  initialState,

  // Load Patients
  on(PatientActions.loadPatients, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(PatientActions.loadPatientsSuccess, (state, { patients }) => ({
    ...state,
    patients,
    loading: false,
    error: null,
  })),

  on(PatientActions.loadPatientsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Load Single Patient
  on(PatientActions.loadPatient, (state) => ({
    ...state,
    loadingPatient: true,
    patientError: null,
  })),

  on(PatientActions.loadPatientSuccess, (state, { patient }) => ({
    ...state,
    currentPatient: patient,
    loadingPatient: false,
    patientError: null,
  })),

  on(PatientActions.loadPatientFailure, (state, { error }) => ({
    ...state,
    loadingPatient: false,
    patientError: error,
  })),

  // Create Patient
  on(PatientActions.createPatient, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(PatientActions.createPatientSuccess, (state, { patient }) => ({
    ...state,
    patients: [...state.patients, patient],
    currentPatient: patient,
    loading: false,
    error: null,
  })),

  on(PatientActions.createPatientFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Update Patient
  on(PatientActions.updatePatient, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(PatientActions.updatePatientSuccess, (state, { patient }) => ({
    ...state,
    patients: state.patients.map((p) => (p.id === patient.id ? patient : p)),
    currentPatient: state.currentPatient?.id === patient.id ? patient : state.currentPatient,
    selectedPatient: state.selectedPatient?.id === patient.id ? patient : state.selectedPatient,
    loading: false,
    error: null,
  })),

  on(PatientActions.updatePatientFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Delete Patient
  on(PatientActions.deletePatient, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(PatientActions.deletePatientSuccess, (state, { id }) => ({
    ...state,
    patients: state.patients.filter((p) => p.id !== id),
    currentPatient: state.currentPatient?.id === id ? null : state.currentPatient,
    selectedPatient: state.selectedPatient?.id === id ? null : state.selectedPatient,
    loading: false,
    error: null,
  })),

  on(PatientActions.deletePatientFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Search Patients
  on(PatientActions.searchPatients, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(PatientActions.searchPatientsSuccess, (state, { patients }) => ({
    ...state,
    searchResults: patients,
    loading: false,
    error: null,
  })),

  on(PatientActions.searchPatientsFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Select Patient
  on(PatientActions.selectPatient, (state, { patient }) => ({
    ...state,
    selectedPatient: patient,
  })),

  // Clear Selected Patient
  on(PatientActions.clearSelectedPatient, (state) => ({
    ...state,
    selectedPatient: null,
  })),

  // Clear Patients
  on(PatientActions.clearPatients, () => initialState)
);

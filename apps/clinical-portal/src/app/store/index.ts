import { ActionReducerMap } from '@ngrx/store';
import { PatientState, patientReducer } from './reducers/patient.reducer';
import {
  CareRecommendationState,
  careRecommendationReducer,
} from './reducers/care-recommendation.reducer';

/**
 * Application State Interface
 * Add additional feature states here as they are created
 */
export interface AppState {
  patient: PatientState;
  careRecommendation: CareRecommendationState;
  // Add more feature states here:
  // measure: MeasureState;
  // careGap: CareGapState;
  // fhir: FhirState;
  // auth: AuthState;
}

/**
 * Root Reducer Map
 * Maps feature keys to their reducers
 */
export const reducers: ActionReducerMap<AppState> = {
  patient: patientReducer,
  careRecommendation: careRecommendationReducer,
  // Add more reducers here:
  // measure: measureReducer,
  // careGap: careGapReducer,
  // fhir: fhirReducer,
  // auth: authReducer,
};

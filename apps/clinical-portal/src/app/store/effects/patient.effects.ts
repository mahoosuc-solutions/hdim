import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, catchError, switchMap, tap } from 'rxjs/operators';
import { PatientService } from '../../services/patient.service';
import { NotificationService } from '../../services/notification.service';
import { Router } from '@angular/router';
import * as PatientActions from '../actions/patient.actions';

/**
 * Patient Effects - Handle side effects for patient actions
 * Uses inject() to ensure dependencies are available before class field initializers
 */
@Injectable()
export class PatientEffects {
  private readonly actions$ = inject(Actions);
  private readonly patientService = inject(PatientService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  /**
   * Load patients effect
   */
  loadPatients$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PatientActions.loadPatients),
      switchMap(({ count }) =>
        this.patientService.getPatients(count).pipe(
          map((patients) => PatientActions.loadPatientsSuccess({ patients })),
          catchError((error) => {
            this.notificationService.error('Failed to load patients');
            return of(PatientActions.loadPatientsFailure({ error: error.message }));
          })
        )
      )
    )
  );

  /**
   * Load single patient effect
   */
  loadPatient$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PatientActions.loadPatient),
      switchMap(({ id }) =>
        this.patientService.getPatient(id).pipe(
          map((patient) => PatientActions.loadPatientSuccess({ patient })),
          catchError((error) => {
            this.notificationService.error(`Failed to load patient: ${error.message}`);
            return of(PatientActions.loadPatientFailure({ error: error.message }));
          })
        )
      )
    )
  );

  /**
   * Create patient effect
   */
  createPatient$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PatientActions.createPatient),
      switchMap(({ patient }) =>
        this.patientService.createPatient(patient).pipe(
          map((createdPatient) => {
            this.notificationService.success('Patient created successfully');
            return PatientActions.createPatientSuccess({ patient: createdPatient });
          }),
          catchError((error) => {
            this.notificationService.error(`Failed to create patient: ${error.message}`);
            return of(PatientActions.createPatientFailure({ error: error.message }));
          })
        )
      )
    )
  );

  /**
   * Update patient effect
   */
  updatePatient$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PatientActions.updatePatient),
      switchMap(({ id, patient }) =>
        this.patientService.updatePatient(id, patient).pipe(
          map((updatedPatient) => {
            this.notificationService.success('Patient updated successfully');
            return PatientActions.updatePatientSuccess({ patient: updatedPatient });
          }),
          catchError((error) => {
            this.notificationService.error(`Failed to update patient: ${error.message}`);
            return of(PatientActions.updatePatientFailure({ error: error.message }));
          })
        )
      )
    )
  );

  /**
   * Delete patient effect
   */
  deletePatient$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PatientActions.deletePatient),
      switchMap(({ id }) =>
        this.patientService.deletePatient(id).pipe(
          map(() => {
            this.notificationService.success('Patient deleted successfully');
            return PatientActions.deletePatientSuccess({ id });
          }),
          catchError((error) => {
            this.notificationService.error(`Failed to delete patient: ${error.message}`);
            return of(PatientActions.deletePatientFailure({ error: error.message }));
          })
        )
      )
    )
  );

  /**
   * Search patients effect
   */
  searchPatients$ = createEffect(() =>
    this.actions$.pipe(
      ofType(PatientActions.searchPatients),
      switchMap(({ params }) =>
        this.patientService.searchPatients(params).pipe(
          map((patients) => PatientActions.searchPatientsSuccess({ patients })),
          catchError((error) => {
            this.notificationService.error('Failed to search patients');
            return of(PatientActions.searchPatientsFailure({ error: error.message }));
          })
        )
      )
    )
  );

  /**
   * Navigate after successful patient creation
   */
  createPatientSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PatientActions.createPatientSuccess),
        tap(({ patient }) => {
          if (patient.id) {
            this.router.navigate(['/patients', patient.id]);
          }
        })
      ),
    { dispatch: false }
  );

  /**
   * Navigate after successful patient deletion
   */
  deletePatientSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(PatientActions.deletePatientSuccess),
        tap(() => {
          this.router.navigate(['/patients']);
        })
      ),
    { dispatch: false }
  );
}

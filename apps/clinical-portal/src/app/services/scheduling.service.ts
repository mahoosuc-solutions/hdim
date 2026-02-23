import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, map, Observable, of, switchMap, take, forkJoin } from 'rxjs';
import { API_CONFIG, FHIR_ENDPOINTS } from '../config/api.config';
import { PatientService } from './patient.service';
import { Patient } from '../models/patient.model';
import { AuthService } from './auth.service';
import { getScheduleSourceForTenant, ScheduleSource } from '../config/scheduling.config';

interface FhirBundle<T> {
  entry?: Array<{ resource: T }>;
}

interface FhirReference {
  reference?: string;
  display?: string;
}

interface FhirCoding {
  system?: string;
  code?: string;
  display?: string;
}

interface FhirCodeableConcept {
  text?: string;
  coding?: FhirCoding[];
}

interface FhirAppointmentParticipant {
  actor?: FhirReference;
  status?: string;
}

interface FhirAppointment {
  id?: string;
  status?: string;
  description?: string;
  start?: string;
  end?: string;
  appointmentType?: FhirCodeableConcept;
  serviceType?: FhirCodeableConcept[];
  participant?: FhirAppointmentParticipant[];
}

interface FhirEncounter {
  id?: string;
  status?: string;
  period?: { start?: string; end?: string };
  type?: FhirCodeableConcept[];
  class?: FhirCoding;
  subject?: FhirReference;
  participant?: Array<{
    individual?: FhirReference;
  }>;
}

interface FhirTask {
  id?: string;
  status?: string;
  priority?: string;
  code?: FhirCodeableConcept;
  description?: string;
  authoredOn?: string;
  executionPeriod?: { start?: string; end?: string };
  for?: FhirReference;
  owner?: FhirReference;
}

export interface ScheduleAppointment {
  id: string;
  patientId: string;
  patientName: string;
  patientMRN: string;
  start: Date;
  end: Date;
  type: string;
  status: string;
  practitionerName?: string;
  practitionerId?: string;
  locationName?: string;
}

export interface ScheduleTask {
  id: string;
  patientId: string;
  patientName: string;
  patientMRN: string;
  type: string;
  status: string;
  priority: 'high' | 'normal' | 'low';
  scheduledStart?: Date;
  ownerName?: string;
  ownerId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class SchedulingService {
  private readonly baseUrl = API_CONFIG.FHIR_SERVER_URL;

  constructor(
    private http: HttpClient,
    private patientService: PatientService,
    private authService: AuthService
  ) {}

  getAppointmentsForDate(date: Date): Observable<ScheduleAppointment[]> {
    return this.getScheduleSource().pipe(
      switchMap((source) => {
        if (source === 'encounter') {
          return this.getEncountersForDate(date);
        }
        if (source === 'hybrid') {
          return forkJoin([
            this.getAppointmentsForDateInternal(date),
            this.getEncountersForDate(date)
          ]).pipe(
            map(([appointments, encounters]) => [...appointments, ...encounters])
          );
        }
        return this.getAppointmentsForDateInternal(date);
      }),
      catchError(() => of([]))
    );
  }

  getTasksForDate(date: Date): Observable<ScheduleTask[]> {
    return this.getScheduleSource().pipe(
      switchMap((source) => {
        if (source === 'encounter') {
          return of([]);
        }
        return this.getTasksForDateInternal(date);
      }),
      catchError(() => of([]))
    );
  }

  private getAppointmentsForDateInternal(date: Date): Observable<ScheduleAppointment[]> {
    const { start, end } = this.getDayBounds(date);
    const url = `${this.baseUrl}${FHIR_ENDPOINTS.APPOINTMENT}` +
      `?date=ge${encodeURIComponent(start.toISOString())}` +
      `&date=le${encodeURIComponent(end.toISOString())}` +
      `&_count=200`;

    return this.http.get<FhirBundle<FhirAppointment>>(url).pipe(
      map((bundle) => this.extractResources(bundle)),
      switchMap((appointments) => this.enrichAppointments(appointments)),
      catchError(() => of([]))
    );
  }

  private getTasksForDateInternal(date: Date): Observable<ScheduleTask[]> {
    const { start, end } = this.getDayBounds(date);
    const url = `${this.baseUrl}${FHIR_ENDPOINTS.TASK}` +
      `?authored-on=ge${encodeURIComponent(start.toISOString())}` +
      `&authored-on=le${encodeURIComponent(end.toISOString())}` +
      `&_count=200`;

    return this.http.get<FhirBundle<FhirTask>>(url).pipe(
      map((bundle) => this.extractResources(bundle)),
      switchMap((tasks) => this.enrichTasks(tasks)),
      catchError(() => of([]))
    );
  }

  private getEncountersForDate(date: Date): Observable<ScheduleAppointment[]> {
    const { start, end } = this.getDayBounds(date);
    const url = `${this.baseUrl}${FHIR_ENDPOINTS.ENCOUNTER}` +
      `?date=ge${encodeURIComponent(start.toISOString())}` +
      `&date=le${encodeURIComponent(end.toISOString())}` +
      `&_count=200`;

    return this.http.get<FhirBundle<FhirEncounter>>(url).pipe(
      map((bundle) => this.extractResources(bundle)),
      switchMap((encounters) => this.enrichEncounters(encounters)),
      catchError(() => of([]))
    );
  }

  private enrichEncounters(encounters: FhirEncounter[]): Observable<ScheduleAppointment[]> {
    if (encounters.length === 0) {
      return of([]);
    }

    return this.patientService.getPatientsCached(200).pipe(
      map((patients) => {
        const patientMap = this.buildPatientMap(patients);
        return encounters.map((encounter) => {
          const patientId = this.getReferenceId(encounter.subject?.reference);
          const patient = patientId ? patientMap.get(patientId) : undefined;
          const start = encounter.period?.start ? new Date(encounter.period.start) : new Date();
          const end = encounter.period?.end ? new Date(encounter.period.end) : new Date(start.getTime() + 30 * 60000);
          const practitioner = encounter.participant?.[0];
          return {
            id: encounter.id || `enc-${start.getTime()}`,
            patientId: patientId || 'unknown',
            patientName: this.formatPatientName(patient) || 'Unknown Patient',
            patientMRN: this.formatPatientMrn(patient) || 'MRN-NA',
            start,
            end,
            type: this.getEncounterType(encounter),
            status: encounter.status || 'planned',
            practitionerName: (practitioner as any)?.individual?.display || undefined,
            practitionerId: this.getReferenceId((practitioner as any)?.individual?.reference) || undefined,
          };
        });
      })
    );
  }

  private getEncounterType(encounter: FhirEncounter): string {
    return (
      encounter.type?.[0]?.text ||
      encounter.type?.[0]?.coding?.[0]?.display ||
      encounter.class?.display ||
      'Encounter'
    );
  }

  private enrichAppointments(appointments: FhirAppointment[]): Observable<ScheduleAppointment[]> {
    if (appointments.length === 0) {
      return of([]);
    }

    return this.patientService.getPatientsCached(200).pipe(
      map((patients) => {
        const patientMap = this.buildPatientMap(patients);
        return appointments.map((appointment) => {
          const patientId = this.getPatientIdFromAppointment(appointment);
          const patient = patientId ? patientMap.get(patientId) : undefined;
          const start = appointment.start ? new Date(appointment.start) : new Date();
          const end = appointment.end ? new Date(appointment.end) : new Date(start.getTime() + 30 * 60000);
          const practitionerParticipant = appointment.participant?.find((p) =>
            p.actor?.reference?.startsWith('PractitionerRole/')
          );
          const locationParticipant = appointment.participant?.find((p) =>
            p.actor?.reference?.startsWith('Location/')
          );
          return {
            id: appointment.id || `appt-${start.getTime()}`,
            patientId: patientId || 'unknown',
            patientName: this.formatPatientName(patient) || 'Unknown Patient',
            patientMRN: this.formatPatientMrn(patient) || 'MRN-NA',
            start,
            end,
            type: this.getAppointmentType(appointment),
            status: appointment.status || 'booked',
            practitionerName: practitionerParticipant?.actor?.display || undefined,
            practitionerId: this.getReferenceId(practitionerParticipant?.actor?.reference) || undefined,
            locationName: locationParticipant?.actor?.display || undefined,
          };
        });
      })
    );
  }

  private enrichTasks(tasks: FhirTask[]): Observable<ScheduleTask[]> {
    if (tasks.length === 0) {
      return of([]);
    }

    return this.patientService.getPatientsCached(200).pipe(
      map((patients) => {
        const patientMap = this.buildPatientMap(patients);
        return tasks.map((task) => {
          const patientId = this.getReferenceId(task.for?.reference);
          const patient = patientId ? patientMap.get(patientId) : undefined;
          const scheduledStart = task.executionPeriod?.start || task.authoredOn;
          return {
            id: task.id || `task-${scheduledStart || 'unknown'}`,
            patientId: patientId || 'unknown',
            patientName: this.formatPatientName(patient) || 'Unknown Patient',
            patientMRN: this.formatPatientMrn(patient) || 'MRN-NA',
            type: this.getTaskType(task),
            status: task.status || 'requested',
            priority: this.mapTaskPriority(task.priority),
            scheduledStart: scheduledStart ? new Date(scheduledStart) : undefined,
            ownerName: task.owner?.display || undefined,
            ownerId: this.getReferenceId(task.owner?.reference) || undefined,
          };
        });
      })
    );
  }

  private extractResources<T>(bundle: FhirBundle<T>): T[] {
    if (!bundle.entry || bundle.entry.length === 0) {
      return [];
    }
    return bundle.entry.map((entry) => entry.resource);
  }

  private buildPatientMap(patients: Patient[]): Map<string, Patient> {
    const map = new Map<string, Patient>();
    patients.forEach((patient) => {
      if (patient.id) {
        map.set(patient.id, patient);
      }
    });
    return map;
  }

  private getPatientIdFromAppointment(appointment: FhirAppointment): string | null {
    const patientParticipant = appointment.participant?.find((participant) =>
      participant.actor?.reference?.startsWith('Patient/')
    );
    return this.getReferenceId(patientParticipant?.actor?.reference);
  }

  private getReferenceId(reference?: string): string | null {
    if (!reference) return null;
    const parts = reference.split('/');
    return parts.length > 1 ? parts[1] : null;
  }

  private getAppointmentType(appointment: FhirAppointment): string {
    return (
      appointment.appointmentType?.text ||
      appointment.appointmentType?.coding?.[0]?.display ||
      appointment.serviceType?.[0]?.text ||
      appointment.serviceType?.[0]?.coding?.[0]?.display ||
      appointment.description ||
      'Appointment'
    );
  }

  private getTaskType(task: FhirTask): string {
    return (
      task.code?.text ||
      task.code?.coding?.[0]?.display ||
      task.description ||
      'Task'
    );
  }

  private mapTaskPriority(priority?: string): 'high' | 'normal' | 'low' {
    switch ((priority || '').toLowerCase()) {
      case 'stat':
      case 'urgent':
      case 'asap':
        return 'high';
      case 'routine':
        return 'normal';
      case 'low':
        return 'low';
      default:
        return 'normal';
    }
  }

  private formatPatientName(patient?: Patient): string | null {
    if (!patient?.name || patient.name.length === 0) return null;
    const name = patient.name[0];
    const family = name.family || '';
    const given = name.given?.join(' ') || '';
    const full = `${family}${family && given ? ', ' : ''}${given}`.trim();
    return full || null;
  }

  private formatPatientMrn(patient?: Patient): string | null {
    const identifier = patient?.identifier?.find((id) =>
      id.type?.coding?.some((coding) => coding.code === 'MR') ||
      id.system?.toLowerCase().includes('mrn')
    );
    return identifier?.value || patient?.identifier?.[0]?.value || null;
  }

  private getDayBounds(date: Date): { start: Date; end: Date } {
    const start = new Date(date);
    start.setHours(0, 0, 0, 0);
    const end = new Date(date);
    end.setHours(23, 59, 59, 999);
    return { start, end };
  }

  private getScheduleSource(): Observable<ScheduleSource> {
    return this.authService.currentUser$.pipe(
      take(1),
      map((user) => {
        const tenantId = user?.tenantId || user?.tenantIds?.[0] || API_CONFIG.DEFAULT_TENANT_ID;
        return getScheduleSourceForTenant(tenantId);
      })
    );
  }
}

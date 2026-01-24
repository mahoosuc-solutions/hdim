import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpProgressEvent } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { map, catchError, delay } from 'rxjs/operators';
import { LoggerService } from './logger.service';

/**
 * Document metadata interface
 */
export interface PatientDocument {
  id: string;
  patientId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  uploadDate: Date;
  uploadedBy: string;
  description?: string;
  ocrStatus?: 'pending' | 'processing' | 'completed' | 'failed';
  ocrText?: string;
}

/**
 * Upload progress interface
 */
export interface UploadProgress {
  fileName: string;
  progress: number; // 0-100
  status: 'pending' | 'uploading' | 'completed' | 'failed';
  error?: string;
}

/**
 * Document Service
 *
 * Handles patient document upload, download, and management.
 *
 * Features:
 * - Multi-file upload with progress tracking
 * - File validation (type, size)
 * - Document list retrieval
 * - Download/view functionality
 * - OCR processing status tracking
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - All uploads logged with user context
 * - PHI filtering in logs
 *
 * Sprint 3 - Issue #244: Document Upload Drag-Drop
 *
 * TODO: Replace mock implementation with actual backend API when available
 * Backend endpoint: POST /api/v1/documents/upload
 * Backend endpoint: GET /api/v1/documents/patient/{patientId}
 * Backend endpoint: GET /api/v1/documents/{documentId}/download
 * Backend endpoint: DELETE /api/v1/documents/{documentId}
 */
@Injectable({
  providedIn: 'root',
})
export class DocumentService {
  private readonly baseUrl = '/documents'; // TODO: Replace with actual API endpoint
  private readonly maxFileSize = 10 * 1024 * 1024; // 10MB
  private readonly allowedTypes = [
    'application/pdf',
    'image/png',
    'image/jpeg',
    'image/jpg',
    'image/tiff',
  ];

  private logger!: ReturnType<LoggerService['withContext']>;

  // Mock storage (remove when backend available)
  private mockDocuments: Map<string, PatientDocument> = new Map();

  constructor(
    private http: HttpClient,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('DocumentService');
  }

  /**
   * Upload document for patient
   * Returns Observable of upload progress
   */
  uploadDocument(
    patientId: string,
    file: File,
    description?: string
  ): Observable<UploadProgress> {
    // Validate file
    const validation = this.validateFile(file);
    if (!validation.valid) {
      this.logger.error('File validation failed', new Error(validation.error || 'Unknown error'));
      return throwError(() => new Error(validation.error));
    }

    this.logger.info('Uploading document', {
      patientId,
      fileName: file.name,
      fileSize: file.size,
      fileType: file.type,
    });

    // TODO: Replace with actual backend API call
    // const formData = new FormData();
    // formData.append('file', file);
    // formData.append('patientId', patientId);
    // if (description) formData.append('description', description);
    //
    // return this.http.post<any>(`${this.baseUrl}/upload`, formData, {
    //   reportProgress: true,
    //   observe: 'events'
    // }).pipe(
    //   map(event => this.mapProgressEvent(event, file.name)),
    //   catchError(error => this.handleError(error, file.name))
    // );

    // Mock implementation - simulate upload progress
    return this.mockUpload(patientId, file, description);
  }

  /**
   * Get all documents for patient
   */
  getPatientDocuments(patientId: string): Observable<PatientDocument[]> {
    this.logger.info('Fetching patient documents', { patientId });

    // TODO: Replace with actual backend API call
    // return this.http.get<PatientDocument[]>(`${this.baseUrl}/patient/${patientId}`);

    // Mock implementation
    const docs = Array.from(this.mockDocuments.values()).filter(
      (doc) => doc.patientId === patientId
    );
    return of(docs).pipe(delay(300)); // Simulate network delay
  }

  /**
   * Download document by ID
   */
  downloadDocument(documentId: string): Observable<Blob> {
    this.logger.info('Downloading document', { documentId });

    // TODO: Replace with actual backend API call
    // return this.http.get(`${this.baseUrl}/${documentId}/download`, {
    //   responseType: 'blob'
    // });

    // Mock implementation
    return throwError(() => new Error('Download not implemented in mock mode'));
  }

  /**
   * Delete document by ID
   */
  deleteDocument(documentId: string): Observable<void> {
    this.logger.info('Deleting document', { documentId });

    // TODO: Replace with actual backend API call
    // return this.http.delete<void>(`${this.baseUrl}/${documentId}`);

    // Mock implementation
    this.mockDocuments.delete(documentId);
    return of(void 0).pipe(delay(200));
  }

  /**
   * Validate file before upload
   */
  validateFile(file: File): { valid: boolean; error?: string } {
    // Check file type
    if (!this.allowedTypes.includes(file.type)) {
      return {
        valid: false,
        error: `File type not allowed. Allowed types: PDF, PNG, JPG, JPEG, TIFF`,
      };
    }

    // Check file size
    if (file.size > this.maxFileSize) {
      return {
        valid: false,
        error: `File size exceeds maximum of ${this.maxFileSize / (1024 * 1024)}MB`,
      };
    }

    return { valid: true };
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  /**
   * Map HTTP progress event to UploadProgress
   */
  private mapProgressEvent(event: HttpEvent<any>, fileName: string): UploadProgress {
    if (event.type === HttpEventType.UploadProgress) {
      const progressEvent = event as HttpProgressEvent;
      const progress = progressEvent.total
        ? Math.round((100 * progressEvent.loaded) / progressEvent.total)
        : 0;

      return {
        fileName,
        progress,
        status: progress < 100 ? 'uploading' : 'completed',
      };
    }

    if (event.type === HttpEventType.Response) {
      return {
        fileName,
        progress: 100,
        status: 'completed',
      };
    }

    return {
      fileName,
      progress: 0,
      status: 'pending',
    };
  }

  /**
   * Handle upload error
   */
  private handleError(error: any, fileName: string): Observable<UploadProgress> {
    this.logger.error('Upload failed', error);
    return of({
      fileName,
      progress: 0,
      status: 'failed',
      error: error.message || 'Upload failed',
    });
  }

  /**
   * Mock upload implementation
   * Simulates upload progress over 2 seconds
   */
  private mockUpload(
    patientId: string,
    file: File,
    description?: string
  ): Observable<UploadProgress> {
    return new Observable((observer) => {
      let progress = 0;
      const interval = setInterval(() => {
        progress += 10;

        observer.next({
          fileName: file.name,
          progress,
          status: progress < 100 ? 'uploading' : 'completed',
        });

        if (progress >= 100) {
          clearInterval(interval);

          // Add to mock storage
          const document: PatientDocument = {
            id: this.generateId(),
            patientId,
            fileName: file.name,
            fileType: file.type,
            fileSize: file.size,
            uploadDate: new Date(),
            uploadedBy: 'current-user', // TODO: Get from auth context
            description,
            ocrStatus: 'pending',
          };
          this.mockDocuments.set(document.id, document);

          observer.complete();
        }
      }, 200); // Update every 200ms (10 updates over 2 seconds)

      // Cleanup on unsubscribe
      return () => clearInterval(interval);
    });
  }

  /**
   * Generate unique ID for mock documents
   */
  private generateId(): string {
    return `doc-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
  }
}

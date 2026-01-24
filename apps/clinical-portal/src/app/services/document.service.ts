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
 * Handles patient document upload, download, and management with OCR processing.
 *
 * Features:
 * - Multi-file upload with progress tracking
 * - File validation (type, size - max 10MB)
 * - Document list retrieval with OCR status
 * - Download/view functionality
 * - Async OCR processing (Tesseract LSTM)
 * - Full-text search on OCR extracted text
 * - OCR reprocessing capability
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - All uploads logged with user context
 * - PHI filtering in logs
 *
 * Backend API Endpoints (documentation-service):
 * - POST /api/documents/clinical/{id}/upload - Upload file with OCR
 * - GET /api/documents/clinical/patient/{patientId} - List documents
 * - GET /api/documents/clinical/attachments/{id} - Download attachment
 * - DELETE /api/documents/clinical/{id} - Delete document
 * - GET /api/documents/clinical/search-ocr - Search OCR text
 * - GET /api/documents/clinical/attachments/{id}/ocr-status - OCR status
 * - POST /api/documents/clinical/attachments/{id}/reprocess-ocr - Retry OCR
 *
 * Sprint 3 - Issue #244: Document Upload Drag-Drop
 * Issue #245 - Part 3: OCR Integration with Frontend
 */
@Injectable({
  providedIn: 'root',
})
export class DocumentService {
  private readonly baseUrl = '/api/documents/clinical'; // Clinical Document Service API
  private readonly maxFileSize = 10 * 1024 * 1024; // 10MB
  private readonly allowedTypes = [
    'application/pdf',
    'image/png',
    'image/jpeg',
    'image/jpg',
    'image/tiff',
  ];

  private logger!: ReturnType<LoggerService['withContext']>;

  constructor(
    private http: HttpClient,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('DocumentService');
  }

  /**
   * Upload document for patient
   * Returns Observable of upload progress
   *
   * Backend: POST /api/documents/clinical/{id}/upload
   * - Uploads file with multipart/form-data
   * - Triggers async OCR processing
   * - Returns document attachment with PENDING status
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

    // Create document for patient first
    // TODO: Get clinical document ID from patient context
    // For now, use placeholder - this needs to be retrieved from patient detail component
    const documentId = 'placeholder-doc-id';

    const formData = new FormData();
    formData.append('file', file);
    if (description) formData.append('title', description);

    return this.http.post<any>(`${this.baseUrl}/${documentId}/upload`, formData, {
      reportProgress: true,
      observe: 'events'
    }).pipe(
      map(event => this.mapProgressEvent(event, file.name)),
      catchError(error => this.handleError(error, file.name))
    );
  }

  /**
   * Get all documents for patient
   *
   * Backend: GET /api/documents/clinical/patient/{patientId}
   * - Returns list of clinical documents with attachments
   * - Includes OCR status for each attachment
   */
  getPatientDocuments(patientId: string): Observable<PatientDocument[]> {
    this.logger.info('Fetching patient documents', { patientId });

    return this.http.get<any[]>(`${this.baseUrl}/patient/${patientId}`).pipe(
      map(docs => docs.map(doc => this.mapDocumentResponse(doc))),
      catchError(error => {
        this.logger.error('Failed to fetch patient documents', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Map backend document response to PatientDocument interface
   */
  private mapDocumentResponse(doc: any): PatientDocument {
    return {
      id: doc.id,
      patientId: doc.patientId,
      fileName: doc.attachments?.[0]?.fileName || 'Unnamed Document',
      fileType: doc.attachments?.[0]?.contentType || 'unknown',
      fileSize: doc.attachments?.[0]?.fileSize || 0,
      uploadDate: new Date(doc.createdAt || doc.documentDate),
      uploadedBy: doc.authorName || 'Unknown',
      description: doc.description || doc.title,
      ocrStatus: doc.attachments?.[0]?.ocrStatus?.toLowerCase() as any,
      ocrText: doc.attachments?.[0]?.ocrText,
    };
  }

  /**
   * Download document by ID
   *
   * Backend: GET /api/documents/clinical/attachments/{attachmentId}
   * - Returns file blob for download
   */
  downloadDocument(documentId: string): Observable<Blob> {
    this.logger.info('Downloading document', { documentId });

    return this.http.get(`${this.baseUrl}/attachments/${documentId}`, {
      responseType: 'blob'
    }).pipe(
      catchError(error => {
        this.logger.error('Download failed', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Delete document by ID
   *
   * Backend: DELETE /api/documents/clinical/{id}
   * - Deletes clinical document and all attachments
   */
  deleteDocument(documentId: string): Observable<void> {
    this.logger.info('Deleting document', { documentId });

    return this.http.delete<void>(`${this.baseUrl}/${documentId}`).pipe(
      catchError(error => {
        this.logger.error('Delete failed', error);
        return throwError(() => error);
      })
    );
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
   * Search OCR text across all documents
   *
   * Backend: GET /api/documents/clinical/search-ocr?query={query}
   * - Full-text search on OCR extracted text
   * - Returns paginated list of attachments
   */
  searchOcrText(query: string, page: number = 0, size: number = 20): Observable<PatientDocument[]> {
    this.logger.info('Searching OCR text', { query, page, size });

    return this.http.get<any>(`${this.baseUrl}/search-ocr`, {
      params: { query, page: page.toString(), size: size.toString() }
    }).pipe(
      map(response => response.content.map((attachment: any) => ({
        id: attachment.id,
        patientId: attachment.clinicalDocumentId, // Use document ID as patient context
        fileName: attachment.fileName,
        fileType: attachment.contentType,
        fileSize: attachment.fileSize,
        uploadDate: new Date(attachment.createdAt),
        uploadedBy: 'Unknown', // Not available in attachment response
        description: attachment.title,
        ocrStatus: attachment.ocrStatus?.toLowerCase() as any,
        ocrText: attachment.ocrText,
      }))),
      catchError(error => {
        this.logger.error('OCR text search failed', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Get OCR processing status for attachment
   *
   * Backend: GET /api/documents/clinical/attachments/{attachmentId}/ocr-status
   * - Returns OCR processing status and metadata
   */
  getOcrStatus(attachmentId: string): Observable<any> {
    this.logger.info('Fetching OCR status', { attachmentId });

    return this.http.get<any>(`${this.baseUrl}/attachments/${attachmentId}/ocr-status`).pipe(
      catchError(error => {
        this.logger.error('Failed to fetch OCR status', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Trigger OCR reprocessing for attachment
   *
   * Backend: POST /api/documents/clinical/attachments/{attachmentId}/reprocess-ocr
   * - Resets OCR status to PENDING
   * - Triggers async OCR processing
   */
  reprocessOcr(attachmentId: string): Observable<void> {
    this.logger.info('Triggering OCR reprocessing', { attachmentId });

    return this.http.post<void>(`${this.baseUrl}/attachments/${attachmentId}/reprocess-ocr`, {}).pipe(
      catchError(error => {
        this.logger.error('OCR reprocessing failed', error);
        return throwError(() => error);
      })
    );
  }
}

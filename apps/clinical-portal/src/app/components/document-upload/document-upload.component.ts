import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { Subject, takeUntil, finalize } from 'rxjs';
import { DocumentService, PatientDocument, UploadProgress } from '../../services/document.service';
import { LoggerService } from '../../services/logger.service';

/**
 * Upload queue item
 */
interface UploadQueueItem {
  file: File;
  progress: UploadProgress;
}

/**
 * Document Upload Component
 *
 * Drag-and-drop document upload interface with progress tracking.
 *
 * Features:
 * - Drag-drop zone with visual feedback
 * - File input fallback (click to browse)
 * - Multi-file upload queue
 * - Real-time progress bars
 * - File validation (type, size)
 * - Upload retry on failure
 * - Document list table
 * - Download/delete actions
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - All uploads logged with patient context
 * - PHI filtering in logs
 *
 * Sprint 3 - Issue #244: Document Upload Drag-Drop
 */
@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
    MatChipsModule,
  ],
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.scss'],
})
export class DocumentUploadComponent implements OnInit, OnDestroy {
  @Input() patientId!: string;

  documents: PatientDocument[] = [];
  uploadQueue: UploadQueueItem[] = [];
  isDragOver = false;

  // Table columns
  displayedColumns: string[] = ['fileName', 'fileType', 'fileSize', 'uploadDate', 'ocrStatus', 'actions'];

  private destroy$ = new Subject<void>();
  private logger!: ReturnType<LoggerService['withContext']>;

  constructor(
    private documentService: DocumentService,
    private loggerService: LoggerService
  ) {
    this.logger = this.loggerService.withContext('DocumentUploadComponent');
  }

  ngOnInit(): void {
    if (!this.patientId) {
      this.logger.error('Patient ID is required for document upload', new Error('Missing patientId'));
      return;
    }

    this.logger.info('Document upload initialized', { patientId: this.patientId });
    this.loadDocuments();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load existing documents for patient
   */
  private loadDocuments(): void {
    this.documentService
      .getPatientDocuments(this.patientId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (documents: PatientDocument[]) => {
          this.documents = documents;
          this.logger.info('Documents loaded', { count: documents.length });
        },
        error: (error: Error) => {
          this.logger.error('Failed to load documents', error);
        },
      });
  }

  /**
   * Handle drag over event
   */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  /**
   * Handle drag leave event
   */
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  /**
   * Handle file drop
   */
  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (files) {
      this.handleFiles(Array.from(files));
    }
  }

  /**
   * Handle file input change
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.handleFiles(Array.from(input.files));
      input.value = ''; // Reset input
    }
  }

  /**
   * Process selected files
   */
  private handleFiles(files: File[]): void {
    this.logger.info('Files selected for upload', { count: files.length });

    files.forEach((file) => {
      // Validate file
      const validation = this.documentService.validateFile(file);
      if (!validation.valid) {
        this.logger.error('File validation failed', new Error(validation.error || 'Unknown error'));
        // Add to queue with failed status
        this.uploadQueue.push({
          file,
          progress: {
            fileName: file.name,
            progress: 0,
            status: 'failed',
            error: validation.error,
          },
        });
        return;
      }

      // Add to upload queue
      const queueItem: UploadQueueItem = {
        file,
        progress: {
          fileName: file.name,
          progress: 0,
          status: 'pending',
        },
      };
      this.uploadQueue.push(queueItem);

      // Start upload
      this.uploadFile(queueItem);
    });
  }

  /**
   * Upload single file
   */
  private uploadFile(queueItem: UploadQueueItem): void {
    this.documentService
      .uploadDocument(this.patientId, queueItem.file)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          // Reload documents after upload completes
          if (queueItem.progress.status === 'completed') {
            this.loadDocuments();
          }
        })
      )
      .subscribe({
        next: (progress: UploadProgress) => {
          queueItem.progress = progress;
        },
        error: (error: Error) => {
          this.logger.error('Upload failed', error);
          queueItem.progress = {
            fileName: queueItem.file.name,
            progress: 0,
            status: 'failed',
            error: error.message,
          };
        },
      });
  }

  /**
   * Retry failed upload
   */
  retryUpload(queueItem: UploadQueueItem): void {
    this.logger.info('Retrying upload', { fileName: queueItem.file.name });
    queueItem.progress.status = 'pending';
    queueItem.progress.progress = 0;
    queueItem.progress.error = undefined;
    this.uploadFile(queueItem);
  }

  /**
   * Cancel pending upload
   */
  cancelUpload(queueItem: UploadQueueItem): void {
    this.logger.info('Cancelling upload', { fileName: queueItem.file.name });
    const index = this.uploadQueue.indexOf(queueItem);
    if (index > -1) {
      this.uploadQueue.splice(index, 1);
    }
  }

  /**
   * Clear completed uploads from queue
   */
  clearCompleted(): void {
    this.uploadQueue = this.uploadQueue.filter((item) => item.progress.status !== 'completed');
  }

  /**
   * Download document
   */
  downloadDocument(doc: PatientDocument): void {
    this.logger.info('Downloading document', { documentId: doc.id });

    this.documentService
      .downloadDocument(doc.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          // Create download link
          const url = window.URL.createObjectURL(blob);
          const link = window.document.createElement('a');
          link.href = url;
          link.download = doc.fileName;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: (error: Error) => {
          this.logger.error('Download failed', error);
        },
      });
  }

  /**
   * Delete document
   */
  deleteDocument(doc: PatientDocument): void {
    if (!confirm(`Delete ${doc.fileName}?`)) return;

    this.logger.info('Deleting document', { documentId: doc.id });

    this.documentService
      .deleteDocument(doc.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loadDocuments();
        },
        error: (error: Error) => {
          this.logger.error('Delete failed', error);
        },
      });
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }

  /**
   * Get OCR status color
   */
  getOcrStatusColor(status?: string): string {
    switch (status) {
      case 'completed':
        return 'success';
      case 'processing':
        return 'primary';
      case 'failed':
        return 'warn';
      default:
        return '';
    }
  }

  /**
   * Get upload queue status counts
   */
  getQueueCounts(): { pending: number; uploading: number; completed: number; failed: number } {
    return {
      pending: this.uploadQueue.filter((item) => item.progress.status === 'pending').length,
      uploading: this.uploadQueue.filter((item) => item.progress.status === 'uploading').length,
      completed: this.uploadQueue.filter((item) => item.progress.status === 'completed').length,
      failed: this.uploadQueue.filter((item) => item.progress.status === 'failed').length,
    };
  }
}

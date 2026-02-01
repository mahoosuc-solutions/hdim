import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentUploadService, AttachmentUploadResponse, OcrStatus } from '../../services/document-upload.service';
import { validateFileSize, validateFileType, formatFileSize } from '../../utils/file-validation';
import { LoggerService } from '../../services/logger.service';

export interface OcrCompletionEvent {
  attachmentId: string;
  ocrStatus: OcrStatus;
  ocrText?: string;
  errorMessage?: string;
}

/**
 * Document Upload Component
 *
 * File upload interface with OCR processing integration.
 *
 * Features:
 * - File validation (type, size)
 * - Upload progress indicator
 * - OCR status polling with visual feedback
 * - Retry failed OCR processing
 * - HIPAA-compliant logging (LoggerService, no console.log)
 * - Accessible with ARIA labels and screen reader support
 *
 * Phase 1 - Issue #249: OCR Clinical Workflow Integration
 */
@Component({
  selector: 'app-document-upload',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatChipsModule,
    MatListModule,
    MatTooltipModule
  ],
  templateUrl: './document-upload.component.html',
  styleUrls: ['./document-upload.component.scss']
})
export class DocumentUploadComponent {
  @Input() documentId!: string;
  @Input() patientId!: string;
  @Input() allowedFileTypes: string[] = ['pdf', 'png', 'jpg', 'jpeg', 'tiff'];
  @Input() maxFileSize: number = 10 * 1024 * 1024; // 10 MB

  @Output() uploadSuccess = new EventEmitter<AttachmentUploadResponse>();
  @Output() uploadError = new EventEmitter<string>();
  @Output() ocrComplete = new EventEmitter<OcrCompletionEvent>();

  selectedFile: File | null = null;
  uploading = false;
  errorMessage = '';
  ocrStatus: OcrStatus | null = null;
  uploadedFiles: AttachmentUploadResponse[] = [];


  constructor(
    private uploadService: DocumentUploadService,
    private logger: LoggerService
  ) {
  }

  get acceptedFileTypes(): string {
    return this.allowedFileTypes.map(t => '.' + t).join(',');
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    this.errorMessage = '';

    // Validate file size
    if (!validateFileSize(file, this.maxFileSize)) {
      this.errorMessage = `File size exceeds 10 MB limit (${formatFileSize(file.size)})`;
      this.uploadError.emit(this.errorMessage);
      return;
    }

    // Validate file type
    if (!validateFileType(file)) {
      this.errorMessage = `Unsupported file type: ${file.type}. Please upload PDF or image files.`;
      this.uploadError.emit(this.errorMessage);
      return;
    }

    this.selectedFile = file;
    this.uploadFile(file);
  }

  private uploadFile(file: File): void {
    this.uploading = true;
    this.logger.info('Uploading file', this.documentId);

    this.uploadService.uploadDocument(this.documentId, file).subscribe({
      next: (response) => {
        this.uploading = false;
        this.uploadSuccess.emit(response);
        this.uploadedFiles.push(response);
        this.logger.info('File uploaded successfully', response.attachmentId);

        // Start OCR status polling
        this.startOcrPolling(response.attachmentId);
      },
      error: (error) => {
        this.uploading = false;
        this.errorMessage = `Upload failed: ${error.message || 'Unknown error'}`;
        this.uploadError.emit(this.errorMessage);
        this.logger.error('File upload failed', error);
      }
    });
  }

  private startOcrPolling(attachmentId: string): void {
    this.logger.info('Starting OCR status polling', attachmentId);

    this.uploadService.pollOcrStatus(attachmentId).subscribe({
      next: (status) => {
        this.ocrStatus = status;
        this.logger.info('OCR status update', { attachmentId, status });

        if (status === 'COMPLETED' || status === 'FAILED') {
          this.ocrComplete.emit({
            attachmentId,
            ocrStatus: status
          });
        }
      },
      error: (error) => {
        this.logger.error('OCR status polling failed', error);
      }
    });
  }

  retryOcr(attachmentId: string): void {
    this.logger.info('Retrying OCR processing', attachmentId);

    this.uploadService.retryOcr(attachmentId).subscribe({
      next: () => {
        this.logger.info('OCR retry initiated', attachmentId);
        this.startOcrPolling(attachmentId);
      },
      error: (error) => {
        this.logger.error('OCR retry failed', error);
      }
    });
  }

  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType === 'application/pdf') return 'picture_as_pdf';
    return 'insert_drive_file';
  }

  getOcrStatusColor(status: OcrStatus): string {
    switch (status) {
      case 'PENDING': return 'warn';
      case 'PROCESSING': return 'accent';
      case 'COMPLETED': return 'primary';
      case 'FAILED': return 'warn';
      default: return '';
    }
  }

  getOcrStatusLabel(status: OcrStatus): string {
    switch (status) {
      case 'PENDING': return 'OCR Queued';
      case 'PROCESSING': return 'Processing OCR...';
      case 'COMPLETED': return 'OCR Complete';
      case 'FAILED': return 'OCR Failed';
      default: return '';
    }
  }
}

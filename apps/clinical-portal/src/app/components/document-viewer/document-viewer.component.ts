import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subject, takeUntil } from 'rxjs';
import { DocumentService, PatientDocument } from '../../services/document.service';
import { LoggerService } from '../../services/logger.service';

/**
 * Document Viewer Component
 *
 * Displays document with OCR text and search/highlight capabilities.
 *
 * Features:
 * - PDF/image preview (if browser supports)
 * - OCR text display with search
 * - Highlight search terms in OCR text
 * - OCR status indicator
 * - Reprocess button for failed OCR
 * - Download original file
 *
 * HIPAA Compliance:
 * - Uses LoggerService for audit logging
 * - All document access logged
 * - PHI filtering in logs
 *
 * Issue #245 - Part 4: Document Viewer with Search/Highlight
 */
@Component({
  selector: 'app-document-viewer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './document-viewer.component.html',
  styleUrls: ['./document-viewer.component.scss'],
})
export class DocumentViewerComponent implements OnInit, OnDestroy {
  @Input() document!: PatientDocument;

  searchQuery = '';
  highlightedOcrText = '';
  isLoadingOcrStatus = false;
  ocrStatusDetails: any = null;
  previewUrl: SafeResourceUrl | null = null;

  private destroy$ = new Subject<void>();
  private logger!: ReturnType<LoggerService['withContext']>;

  constructor(
    private documentService: DocumentService,
    private loggerService: LoggerService,
    private sanitizer: DomSanitizer
  ) {
    this.logger = this.loggerService.withContext('DocumentViewerComponent');
  }

  ngOnInit(): void {
    if (!this.document) {
      this.logger.error('Document is required for viewer', new Error('Missing document'));
      return;
    }

    this.logger.info('Document viewer initialized', { documentId: this.document.id });
    this.loadOcrStatus();
    this.updateHighlightedText();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load OCR processing status
   */
  private loadOcrStatus(): void {
    if (!this.document.id) return;

    this.isLoadingOcrStatus = true;
    this.documentService
      .getOcrStatus(this.document.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (status: any) => {
          this.ocrStatusDetails = status;
          this.isLoadingOcrStatus = false;
          this.logger.info('OCR status loaded', { status: status.status });
        },
        error: (error: Error) => {
          this.logger.error('Failed to load OCR status', error);
          this.isLoadingOcrStatus = false;
        },
      });
  }

  /**
   * Update highlighted OCR text based on search query
   */
  updateHighlightedText(): void {
    if (!this.document.ocrText) {
      this.highlightedOcrText = '';
      return;
    }

    if (!this.searchQuery.trim()) {
      this.highlightedOcrText = this.escapeHtml(this.document.ocrText);
      return;
    }

    // Case-insensitive search and highlight
    const regex = new RegExp(`(${this.escapeRegex(this.searchQuery)})`, 'gi');
    this.highlightedOcrText = this.escapeHtml(this.document.ocrText).replace(
      regex,
      '<mark class="highlight">$1</mark>'
    );

    this.logger.info('Text highlighted', { query: this.searchQuery });
  }

  /**
   * Clear search query
   */
  clearSearch(): void {
    this.searchQuery = '';
    this.updateHighlightedText();
  }

  /**
   * Trigger OCR reprocessing
   */
  reprocessOcr(): void {
    if (!this.document.id) return;

    this.logger.info('Triggering OCR reprocessing', { documentId: this.document.id });

    this.documentService
      .reprocessOcr(this.document.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.logger.info('OCR reprocessing started');
          // Refresh OCR status
          setTimeout(() => this.loadOcrStatus(), 2000);
        },
        error: (error: Error) => {
          this.logger.error('OCR reprocessing failed', error);
        },
      });
  }

  /**
   * Download original document
   */
  downloadDocument(): void {
    if (!this.document.id) return;

    this.logger.info('Downloading document', { documentId: this.document.id });

    this.documentService
      .downloadDocument(this.document.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob: Blob) => {
          // Create download link
          const url = window.URL.createObjectURL(blob);
          const link = window.document.createElement('a');
          link.href = url;
          link.download = this.document.fileName;
          link.click();
          window.URL.revokeObjectURL(url);
        },
        error: (error: Error) => {
          this.logger.error('Download failed', error);
        },
      });
  }

  /**
   * Get OCR status color
   */
  getOcrStatusColor(): string {
    switch (this.document.ocrStatus) {
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
   * Check if OCR is in progress
   */
  isOcrProcessing(): boolean {
    return this.document.ocrStatus === 'processing' || this.document.ocrStatus === 'pending';
  }

  /**
   * Check if OCR failed
   */
  isOcrFailed(): boolean {
    return this.document.ocrStatus === 'failed';
  }

  /**
   * Check if OCR completed
   */
  isOcrCompleted(): boolean {
    return this.document.ocrStatus === 'completed';
  }

  /**
   * Get match count for search query
   */
  getMatchCount(): number {
    if (!this.searchQuery.trim() || !this.document.ocrText) return 0;

    const regex = new RegExp(this.escapeRegex(this.searchQuery), 'gi');
    const matches = this.document.ocrText.match(regex);
    return matches ? matches.length : 0;
  }

  /**
   * Escape HTML special characters
   */
  private escapeHtml(text: string): string {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * Escape regex special characters
   */
  private escapeRegex(text: string): string {
    return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes: number): string {
    return this.documentService.formatFileSize(bytes);
  }
}

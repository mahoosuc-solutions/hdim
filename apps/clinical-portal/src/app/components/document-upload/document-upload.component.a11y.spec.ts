/**
 * Accessibility Tests for DocumentUploadComponent
 *
 * Tests WCAG 2.1 Level A and AA compliance for the document upload component.
 * Validates file upload controls, status announcements, error messaging, and
 * screen reader support for OCR progress.
 *
 * HIPAA Compliance: Ensures accessible PHI handling for clinical documents.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentUploadComponent } from './document-upload.component';
import { DocumentUploadService } from '../../services/document-upload.service';
import { LoggerService } from '../../services/logger.service';
import {
  testAccessibility,
  testKeyboardAccessibility,
  testAriaAttributes,
} from '../../../testing/accessibility.helper';
import { of } from 'rxjs';

describe('DocumentUploadComponent - Accessibility', () => {
  let component: DocumentUploadComponent;
  let fixture: ComponentFixture<DocumentUploadComponent>;
  let uploadService: jest.Mocked<DocumentUploadService>;
  let loggerService: jest.Mocked<LoggerService>;

  beforeEach(async () => {
    const uploadServiceMock = {
      uploadDocument: jest.fn(),
      pollOcrStatus: jest.fn(),
      retryOcr: jest.fn(),
    } as any;

    const loggerServiceMock = {
      withContext: jest.fn().mockReturnValue({
        info: jest.fn(),
        error: jest.fn(),
        debug: jest.fn(),
        warn: jest.fn(),
      }),
    } as any;

    await TestBed.configureTestingModule({
      imports: [
        DocumentUploadComponent,
        MatIconModule,
        MatButtonModule,
        MatProgressBarModule,
        MatChipsModule,
        MatListModule,
        MatTooltipModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: DocumentUploadService, useValue: uploadServiceMock },
        { provide: LoggerService, useValue: loggerServiceMock },
      ],
    }).compileComponents();

    uploadService = TestBed.inject(
      DocumentUploadService
    ) as jest.Mocked<DocumentUploadService>;
    loggerService = TestBed.inject(
      LoggerService
    ) as jest.Mocked<LoggerService>;
    fixture = TestBed.createComponent(DocumentUploadComponent);
    component = fixture.componentInstance;
    component.documentId = 'doc-123';
    component.patientId = 'patient-456';
    fixture.detectChanges();
  });

  describe('WCAG 2.1 Level A Compliance', () => {
    it('should have no Level A accessibility violations', async () => {
      // Add uploaded files to populate the DOM with accessible content
      component.uploadedFiles = [
        {
          attachmentId: 'attach-123',
          fileName: 'test-document.pdf',
          mimeType: 'application/pdf',
          fileSize: 4096,
          uploadDate: '2026-01-24T12:00:00Z',
          ocrStatus: 'COMPLETED',
        },
      ];
      fixture.detectChanges();

      // Disable region rule for component-level testing (components don't have full page structure)
      const results = await testAccessibility(fixture, {
        rules: {
          region: { enabled: false },
        },
      });
      expect(results).toHaveNoViolations();
    });

    it('should have accessible upload button', () => {
      const uploadButton = fixture.nativeElement.querySelector(
        'button[aria-label="Upload clinical document"]'
      );

      expect(uploadButton).toBeTruthy();
      expect(uploadButton.getAttribute('aria-label')).toBe(
        'Upload clinical document'
      );
      expect(uploadButton.textContent).toContain('Upload Document');
    });

    it('should have accessible file input', () => {
      const fileInput = fixture.nativeElement.querySelector(
        'input[type="file"]'
      );

      expect(fileInput).toBeTruthy();
      expect(fileInput.getAttribute('aria-label')).toBe(
        'Choose file to upload'
      );
      expect(fileInput.hasAttribute('accept')).toBe(true);
    });
  });

  describe('WCAG 2.1 Level AA Compliance', () => {
    it('should support keyboard navigation', async () => {
      // Add uploaded files to populate the DOM with keyboard-accessible elements
      component.uploadedFiles = [
        {
          attachmentId: 'attach-123',
          fileName: 'test-document.pdf',
          mimeType: 'application/pdf',
          fileSize: 4096,
          uploadDate: '2026-01-24T12:00:00Z',
          ocrStatus: 'FAILED',
        },
      ];
      fixture.detectChanges();

      const results = await testKeyboardAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have valid ARIA attributes', async () => {
      const results = await testAriaAttributes(fixture);
      expect(results).toHaveNoViolations();
    });
  });

  describe('Screen Reader Support', () => {
    it('should announce upload progress', () => {
      component.uploading = true;
      component.selectedFile = new File(['test'], 'test.pdf', {
        type: 'application/pdf',
      });
      fixture.detectChanges();

      const progressContainer = fixture.nativeElement.querySelector('.upload-progress');
      expect(progressContainer.getAttribute('role')).toBe('status');
      expect(progressContainer.getAttribute('aria-live')).toBe('polite');

      const srText = fixture.nativeElement.querySelector('.sr-only');
      expect(srText.textContent).toContain('Uploading test.pdf');
    });

    it('should announce OCR status updates', () => {
      component.ocrStatus = 'PROCESSING';
      fixture.detectChanges();

      const statusContainer = fixture.nativeElement.querySelector('.ocr-status');
      expect(statusContainer.getAttribute('role')).toBe('status');
      expect(statusContainer.getAttribute('aria-live')).toBe('polite');
    });

    it('should announce errors with role="alert"', () => {
      component.errorMessage = 'File size exceeds 10 MB';
      fixture.detectChanges();

      const errorContainer = fixture.nativeElement.querySelector('.error-message');
      expect(errorContainer.getAttribute('role')).toBe('alert');
      expect(errorContainer.textContent).toContain('File size exceeds 10 MB');
    });
  });

  describe('Retry Button Accessibility', () => {
    it('should have accessible retry button for failed OCR', () => {
      component.uploadedFiles = [
        {
          attachmentId: 'attach-123',
          fileName: 'test.pdf',
          mimeType: 'application/pdf',
          fileSize: 12,
          uploadDate: '2026-01-24T12:00:00Z',
          ocrStatus: 'FAILED',
        },
      ];
      fixture.detectChanges();

      const retryButton = fixture.nativeElement.querySelector(
        'button[aria-label*="Retry OCR"]'
      );

      expect(retryButton).toBeTruthy();
      expect(retryButton.getAttribute('aria-label')).toBe(
        'Retry OCR for test.pdf'
      );
    });

    it('should hide decorative icon from screen readers', () => {
      component.uploadedFiles = [
        {
          attachmentId: 'attach-123',
          fileName: 'test.pdf',
          mimeType: 'application/pdf',
          fileSize: 12,
          uploadDate: '2026-01-24T12:00:00Z',
          ocrStatus: 'FAILED',
        },
      ];
      fixture.detectChanges();

      const retryButton = fixture.nativeElement.querySelector(
        'button[aria-label*="Retry OCR"]'
      );
      const icon = retryButton.querySelector('mat-icon');

      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });

  describe('File Icons Accessibility', () => {
    it('should hide decorative icons from screen readers', () => {
      component.uploadedFiles = [
        {
          attachmentId: 'attach-123',
          fileName: 'test.pdf',
          mimeType: 'application/pdf',
          fileSize: 12,
          uploadDate: '2026-01-24T12:00:00Z',
          ocrStatus: 'COMPLETED',
        },
      ];
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('mat-icon[matListItemIcon]');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });
});

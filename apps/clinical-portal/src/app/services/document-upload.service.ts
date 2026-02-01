import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { switchMap, map, distinctUntilChanged, takeWhile, catchError, retry } from 'rxjs/operators';
import { AuthService } from './auth.service';

export type OcrStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AttachmentUploadResponse {
  attachmentId: string;
  fileName: string;
  mimeType: string;
  fileSize: number;
  uploadDate: string;
  ocrStatus: OcrStatus;
}

export interface OcrStatusResponse {
  attachmentId: string;
  ocrStatus: OcrStatus;
  ocrText?: string;
  ocrProcessingDate?: string;
  errorMessage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DocumentUploadService {
  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  uploadDocument(
    documentId: string,
    file: File
  ): Observable<AttachmentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    const tenantId = this.authService.getTenantId();

    return this.http.post<AttachmentUploadResponse>(
      `/api/documents/clinical/${documentId}/upload`,
      formData,
      { headers: { 'X-Tenant-ID': tenantId || '' } }
    );
  }

  pollOcrStatus(attachmentId: string): Observable<OcrStatus> {
    const tenantId = this.authService.getTenantId();

    return timer(0, 2000).pipe(
      switchMap(() =>
        this.http.get<OcrStatusResponse>(
          `/api/documents/clinical/attachments/${attachmentId}/ocr-status`,
          { headers: { 'X-Tenant-ID': tenantId || '' } }
        ).pipe(
          retry({ count: 2, delay: 1000 }),
          catchError((error) => {
            throw error;
          })
        )
      ),
      map(response => response.ocrStatus),
      distinctUntilChanged(),
      takeWhile(status => status === 'PENDING' || status === 'PROCESSING', true)
    );
  }

  retryOcr(attachmentId: string): Observable<void> {
    const tenantId = this.authService.getTenantId();

    return this.http.post<void>(
      `/api/documents/clinical/attachments/${attachmentId}/reprocess-ocr`,
      {},
      { headers: { 'X-Tenant-ID': tenantId || '' } }
    );
  }
}

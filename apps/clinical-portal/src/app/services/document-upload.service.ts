import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval } from 'rxjs';
import { switchMap, map, distinctUntilChanged, takeWhile } from 'rxjs/operators';

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
  private readonly tenantId = 'default-tenant'; // TODO: Get from AuthService

  constructor(private http: HttpClient) {}

  uploadDocument(
    documentId: string,
    file: File
  ): Observable<AttachmentUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<AttachmentUploadResponse>(
      `/api/documents/clinical/${documentId}/upload`,
      formData,
      { headers: { 'X-Tenant-ID': this.tenantId } }
    );
  }

  pollOcrStatus(attachmentId: string): Observable<OcrStatus> {
    return interval(2000).pipe(
      switchMap(() =>
        this.http.get<OcrStatusResponse>(
          `/api/documents/clinical/attachments/${attachmentId}/ocr-status`,
          { headers: { 'X-Tenant-ID': this.tenantId } }
        )
      ),
      map(response => response.ocrStatus),
      distinctUntilChanged(),
      takeWhile(status => status === 'PENDING' || status === 'PROCESSING', true)
    );
  }

  retryOcr(attachmentId: string): Observable<void> {
    return this.http.post<void>(
      `/api/documents/clinical/attachments/${attachmentId}/reprocess-ocr`,
      {},
      { headers: { 'X-Tenant-ID': this.tenantId } }
    );
  }
}

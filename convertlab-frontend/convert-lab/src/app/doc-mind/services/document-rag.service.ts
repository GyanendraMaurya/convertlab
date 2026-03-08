import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { IngestResponse, QueryResponse, UploadResponse } from '../models/docmind.models';

export interface ApiResponse<T> {
  data: T;
  success: boolean;
  error?: any;
}

@Injectable({ providedIn: 'root' })
export class DocumentRagService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;


  /**
   * Step 2 — ingest: OCR + chunking + embedding + vector store
   */
  ingestDocument(fileId: string): Observable<ApiResponse<IngestResponse>> {
    return this.http.post<ApiResponse<IngestResponse>>(
      `${this.apiUrl}/documents/ingest`,
      { fileId },
      // { withCredentials: true }
    );
  }

  /**
   * Step 3 — query the indexed document
   */
  queryDocument(fileId: string, query: string): Observable<ApiResponse<QueryResponse>> {
    return this.http.post<ApiResponse<QueryResponse>>(
      `${this.apiUrl}/documents/query`,
      { fileId, query },
      // { withCredentials: true }
    );
  }
}

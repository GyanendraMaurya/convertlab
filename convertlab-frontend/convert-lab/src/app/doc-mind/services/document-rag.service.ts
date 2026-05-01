import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { IngestResponse, QueryResponse, UploadResponse } from '../models/docmind.models';
import { SUPPRESS_ERROR } from '../../interceptors/http-context';

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
      { context: new HttpContext().set(SUPPRESS_ERROR, true) }
    );
  }

  /**
   * Step 3 — query the indexed document
   */
  queryDocument(fileId: string, query: string): Observable<ApiResponse<QueryResponse>> {
    return this.http.post<ApiResponse<QueryResponse>>(
      `${this.apiUrl}/documents/query`,
      { fileId, query },
      { context: new HttpContext().set(SUPPRESS_ERROR, true) }
    );
  }
}

import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { ApiResponse, HttpService } from './http.service';
import { Observable } from 'rxjs';
import { PdfUploadResponse } from '../models/extract-pdf.model';

@Injectable({
  providedIn: 'root',
})
export class FileUploadService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);

  uploadPdf(file: File): Observable<ApiResponse<PdfUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.httpService.post<ApiResponse<PdfUploadResponse>>(`${this.apiUrl}/pdf/upload`, formData);
  }
}

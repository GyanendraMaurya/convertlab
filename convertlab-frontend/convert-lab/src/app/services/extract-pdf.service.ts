import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpService } from './http.service';
import { PdfExtractRequest } from '../models/extract-pdf.model';
import { HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { IS_BLOB_REQUEST, SUPPRESS_ERROR } from '../interceptors/http-context';

@Injectable({
  providedIn: 'root',
})
export class ExtractPdfService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);

  extractPdf(request: PdfExtractRequest): Observable<HttpResponse<Blob>> {
    return this.httpService.post<HttpResponse<Blob>>(`${this.apiUrl}/pdf/extract`,
      request,
      { responseType: 'blob', observe: 'response', context: new HttpContext().set(IS_BLOB_REQUEST, true) });
  }
}

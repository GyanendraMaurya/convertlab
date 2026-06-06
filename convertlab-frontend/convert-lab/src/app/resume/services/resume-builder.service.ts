import { HttpContext, HttpResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { IS_BLOB_REQUEST } from '../../interceptors/http-context';
import { ApiResponse, HttpService } from '../../services/http.service';
import { ResumeRequest, ResumeTemplate } from '../models/buildermodel';

@Injectable({ providedIn: 'root' })
export class ResumeBuilderService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);

  getTemplates(): Observable<ApiResponse<ResumeTemplate[]>> {
    return this.httpService.get<ApiResponse<ResumeTemplate[]>>(`${this.apiUrl}/resumes/templates`);
  }

  previewResume(request: ResumeRequest, templateId: string): Observable<string> {
    return this.httpService.post<string>(
      `${this.apiUrl}/resumes/preview/${templateId}`,
      request,
      { responseType: 'text' }
    );
  }

  downloadResume(request: ResumeRequest, templateId: string): Observable<HttpResponse<Blob>> {
    return this.httpService.post<HttpResponse<Blob>>(
      `${this.apiUrl}/resumes/download/${templateId}`,
      request,
      {
        responseType: 'blob',
        observe: 'response',
        context: new HttpContext().set(IS_BLOB_REQUEST, true)
      }
    );
  }
}

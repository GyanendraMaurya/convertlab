import { inject, Injectable } from '@angular/core';
import { CompressImageRequest } from '../models/compress-image.model';
import { HttpContext, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { HttpService } from './http.service';
import { IS_BLOB_REQUEST } from '../interceptors/http-context';

@Injectable({
  providedIn: 'root',
})
export class ImageService {

  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);

  compressImages(request: CompressImageRequest): Observable<HttpResponse<Blob>> {
    return this.httpService.post<HttpResponse<Blob>>(
      `${this.apiUrl}/image/compress`,
      request,
      {
        responseType: 'blob',
        observe: 'response',
        context: new HttpContext().set(IS_BLOB_REQUEST, true)
      }
    );
  }

}

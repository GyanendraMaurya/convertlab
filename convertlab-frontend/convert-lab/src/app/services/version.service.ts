import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { HttpService, ApiResponse } from './http.service';
import { Observable } from 'rxjs';

export interface VersionInfo {
  version: string;
  buildTime: string;
  serverTime?: string;
}

@Injectable({
  providedIn: 'root',
})
export class VersionService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);

  /**
   * Get frontend version from environment
   */
  getFrontendVersion(): VersionInfo {
    return {
      version: environment.version,
      buildTime: environment.buildTime
    };
  }

  /**
   * Get backend version from API
   */
  getBackendVersion(): Observable<ApiResponse<VersionInfo>> {
    return this.httpService.get<ApiResponse<VersionInfo>>(`${this.apiUrl}/version`);
  }
}

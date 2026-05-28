import { HttpContext } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { SUPPRESS_ERROR } from '../interceptors/http-context';
import { ApiResponse, HttpService } from './http.service';

export type FeatureCode = 'SHOW_CONTACT_PAGE';

export interface FeatureFlag {
  code: string;
  title: string;
  enabled: boolean;
  exposeToFrontend: boolean;
}

export interface FeatureFlagUpdate {
  code: string;
  enabled: boolean;
}

const DEFAULT_PUBLIC_FEATURES: Record<FeatureCode, boolean> = {
  SHOW_CONTACT_PAGE: true,
};

@Injectable({
  providedIn: 'root',
})
export class FeatureFlagService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);
  private readonly flags = signal<Record<string, boolean>>({ ...DEFAULT_PUBLIC_FEATURES });

  readonly showContactPage = computed(() => this.isEnabled('SHOW_CONTACT_PAGE'));

  async loadPublicFeatures(): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.httpService.get<ApiResponse<FeatureFlag[]>>(
          `${this.apiUrl}/features/public`,
          { context: new HttpContext().set(SUPPRESS_ERROR, true) },
        ),
      );

      this.setPublicFlags(response.data ?? []);
    } catch {
      this.flags.set({ ...DEFAULT_PUBLIC_FEATURES });
    }
  }

  isEnabled(code: FeatureCode | string): boolean {
    return this.flags()[code] ?? DEFAULT_PUBLIC_FEATURES[code as FeatureCode] ?? true;
  }

  getAdminFeatures(): Observable<ApiResponse<FeatureFlag[]>> {
    return this.httpService.get<ApiResponse<FeatureFlag[]>>(`${this.apiUrl}/admin/features`);
  }

  updateAdminFeatures(features: FeatureFlagUpdate[]): Observable<ApiResponse<FeatureFlag[]>> {
    return this.httpService.put<ApiResponse<FeatureFlag[]>>(
      `${this.apiUrl}/admin/features`,
      { features },
    );
  }

  applyPublicFeatures(features: FeatureFlag[]): void {
    this.setPublicFlags(features.filter(feature => feature.exposeToFrontend));
  }

  private setPublicFlags(features: FeatureFlag[]): void {
    const nextFlags: Record<string, boolean> = { ...DEFAULT_PUBLIC_FEATURES };

    for (const feature of features) {
      nextFlags[feature.code] = feature.enabled;
    }

    this.flags.set(nextFlags);
  }
}

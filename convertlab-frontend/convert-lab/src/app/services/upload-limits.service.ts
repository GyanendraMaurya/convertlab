import { HttpContext } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { SUPPRESS_ERROR } from '../interceptors/http-context';
import { ApiResponse, HttpService } from './http.service';

export interface PdfUploadLimits {
  guestMaxSizeBytes: number;
  authenticatedMaxSizeBytes: number;
  maxPages: number;
  allowedExtensions: string[];
}

export interface ImageUploadLimits {
  guestMaxSizeBytes: number;
  authenticatedMaxSizeBytes: number;
  maxDimension: number;
  allowedExtensions: string[];
}

export interface UploadLimits {
  authenticated: boolean;
  pdf: PdfUploadLimits;
  image: ImageUploadLimits;
}

export const DEFAULT_UPLOAD_LIMITS: UploadLimits = {
  authenticated: false,
  pdf: {
    guestMaxSizeBytes: 25 * 1024 * 1024,
    authenticatedMaxSizeBytes: 75 * 1024 * 1024,
    maxPages: 1000,
    allowedExtensions: ['pdf'],
  },
  image: {
    guestMaxSizeBytes: 10 * 1024 * 1024,
    authenticatedMaxSizeBytes: 50 * 1024 * 1024,
    maxDimension: 10000,
    allowedExtensions: ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'],
  },
};

@Injectable({
  providedIn: 'root',
})
export class UploadLimitsService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);
  private readonly limits = signal<UploadLimits>(DEFAULT_UPLOAD_LIMITS);

  async loadUploadLimits(): Promise<void> {
    try {
      const response = await firstValueFrom(
        this.httpService.get<ApiResponse<UploadLimits>>(
          `${this.apiUrl}/upload/limits`,
          { context: new HttpContext().set(SUPPRESS_ERROR, true) },
        ),
      );

      if (response.data) {
        this.limits.set(this.mergeWithDefaults(response.data));
      }
    } catch {
      this.limits.set(DEFAULT_UPLOAD_LIMITS);
    }
  }

  getLimits(): UploadLimits {
    return this.limits();
  }

  private mergeWithDefaults(limits: UploadLimits): UploadLimits {
    return {
      authenticated: limits.authenticated ?? DEFAULT_UPLOAD_LIMITS.authenticated,
      pdf: {
        ...DEFAULT_UPLOAD_LIMITS.pdf,
        ...limits.pdf,
      },
      image: {
        ...DEFAULT_UPLOAD_LIMITS.image,
        ...limits.image,
      },
    };
  }
}

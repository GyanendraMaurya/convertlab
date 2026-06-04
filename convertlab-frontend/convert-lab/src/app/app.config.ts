import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './interceptors/error.interceptor';
import { blobErrorInterceptor } from './interceptors/blob-error.interceptor';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { authInterceptor } from './interceptors/auth.interceptor';
import { AuthInitService } from './services/auth-init.service';
import { sessionInterceptor } from './interceptors/session.interceptor';
import { provideMarkdown } from 'ngx-markdown';
import { FeatureFlagService } from './services/feature-flag.service';
import { UploadLimitsService } from './services/upload-limits.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        authInterceptor,
        sessionInterceptor,
        blobErrorInterceptor,
        errorInterceptor
      ]),
      withFetch()),
    provideMarkdown(),
    provideClientHydration(withEventReplay()),
    provideAppInitializer(() => {
      const authService = inject(AuthInitService);
      return authService.init();
    }),
    provideAppInitializer(() => {
      const featureFlagService = inject(FeatureFlagService);
      return featureFlagService.loadPublicFeatures();
    }),
    provideAppInitializer(() => {
      const uploadLimitsService = inject(UploadLimitsService);
      return uploadLimitsService.loadUploadLimits();
    }),
  ]
};

import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from './interceptors/error.interceptor';
import { blobErrorInterceptor } from './interceptors/blob-error.interceptor';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { authInterceptor } from './interceptors/auth.interceptor';
import { AuthInitService } from './services/auth-init.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        authInterceptor,
        blobErrorInterceptor,
        errorInterceptor
      ]),
      withFetch()),
    provideClientHydration(withEventReplay()),
    provideAppInitializer(() => {
      const authService = inject(AuthInitService);
      return authService.init();
    }),
  ]
};

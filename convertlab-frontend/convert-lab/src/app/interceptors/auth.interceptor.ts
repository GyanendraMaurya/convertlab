// convertlab-frontend/convert-lab/src/app/interceptors/auth.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStateService } from '../services/auth-state.service';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authState = inject(AuthStateService);
  const authService = inject(AuthService);

  // Skip auth endpoints (except refresh)
  if (req.url.includes('/auth/login') ||
    req.url.includes('/auth/signup') ||
    req.url.includes('/auth/verify-otp')) {
    return next(req);
  }

  // Skip refresh endpoint to avoid infinite loops
  if (req.url.includes('/auth/refresh')) {
    return next(req);
  }

  const token = authState.getAccessToken();

  // If no token, try to refresh first (in case refresh token cookie exists)
  if (!token) {
    return authService.refreshToken().pipe(
      switchMap((response) => {
        // Store new tokens
        authState.setTokens(response.data);

        // Clone request with new token
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${response.data.accessToken}`
          }
        });
        return next(authReq);
      }),
      catchError((error) => {
        // Refresh failed (no refresh token cookie or expired)
        // Proceed without token - let the endpoint handle unauthorized
        return next(req);
      })
    );
  }

  // Token exists - check if it's expiring soon
  if (authState.isTokenExpiringSoon()) {
    return authService.refreshToken().pipe(
      switchMap((response) => {
        authState.setTokens(response.data);

        // Clone request with new token
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${response.data.accessToken}`
          }
        });
        return next(authReq);
      }),
      catchError((error) => {
        // Refresh failed, clear tokens and proceed with old token
        authState.clearTokens();

        // Still try with the old token
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        return next(authReq);
      })
    );
  }

  // Add token to request
  const authReq = req.clone({
    setHeaders: {
      Authorization: `Bearer ${token}`
    }
  });

  return next(authReq).pipe(
    catchError((error) => {
      // If 401, try to refresh token
      if (error.status === 401) {
        return authService.refreshToken().pipe(
          switchMap((response) => {
            authState.setTokens(response.data);

            // Retry original request with new token
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${response.data.accessToken}`
              }
            });
            return next(retryReq);
          }),
          catchError((refreshError) => {
            // Refresh failed, clear tokens
            authState.clearTokens();
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};

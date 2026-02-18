import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthStateService } from '../services/auth-state.service';
import { AuthService } from '../services/auth.service';
import { catchError, switchMap, throwError, BehaviorSubject, filter, take } from 'rxjs';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authState = inject(AuthStateService);
  const authService = inject(AuthService);

  // Skip auth endpoints
  if (
    req.url.includes('/auth/login') ||
    req.url.includes('/auth/signup') ||
    req.url.includes('/auth/verify-otp') ||
    req.url.includes('/auth/refresh')
  ) {
    return next(req);
  }

  let authReq = req;

  const token = authState.getAccessToken();

  // Attach access token if present
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error) => {

      // If not 401 → just pass error
      if (error.status !== 401) {
        return throwError(() => error);
      }

      // If refresh already in progress → wait
      if (isRefreshing) {
        return refreshTokenSubject.pipe(
          filter((newToken) => newToken !== null),
          take(1),
          switchMap((newToken) => {
            const retryReq = req.clone({
              setHeaders: {
                Authorization: `Bearer ${newToken}`
              }
            });
            return next(retryReq);
          })
        );
      }

      // Start refresh flow
      isRefreshing = true;
      refreshTokenSubject.next(null);

      return authService.refreshToken().pipe(
        switchMap((response) => {
          const newAccessToken = response.data.accessToken;

          // Save new token in memory
          authState.setTokens(response.data);

          isRefreshing = false;
          refreshTokenSubject.next(newAccessToken);

          // Retry original request
          const retryReq = req.clone({
            setHeaders: {
              Authorization: `Bearer ${newAccessToken}`
            }
          });

          return next(retryReq);
        }),
        catchError((refreshError) => {
          isRefreshing = false;

          // Clear auth state
          authState.clearTokens();

          // Optional: redirect to login
          // inject(Router).navigate(['/login']);

          return throwError(() => refreshError);
        })
      );
    })
  );
};

import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthStateService } from '../services/auth-state.service';

export const authGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (authState.isAuthenticated()) {
    return true;
  }

  router.navigate(['/login']);
  return false;
};

export const guestGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (!authState.isAuthenticated()) {
    return true;
  }

  router.navigate(['/']);
  return false;
};

export const superAdminGuard: CanActivateFn = () => {
  const authState = inject(AuthStateService);
  const router = inject(Router);

  if (!authState.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  if (authState.isSuperAdmin()) {
    return true;
  }

  router.navigate(['/']);
  return false;
};

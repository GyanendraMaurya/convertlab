import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { FeatureFlagService } from '../services/feature-flag.service';

export const contactFeatureGuard: CanActivateFn = () => {
  const featureFlags = inject(FeatureFlagService);
  const router = inject(Router);

  if (featureFlags.isEnabled('SHOW_CONTACT_PAGE')) {
    return true;
  }

  return router.parseUrl('/');
};

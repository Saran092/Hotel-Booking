import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isAuthenticated) return router.createUrlTree(['/login']);
  if (auth.mustChangePassword) return router.createUrlTree(['/change-password']);
  return true;
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (auth.isAuthenticated && !auth.mustChangePassword) return router.createUrlTree([auth.homeFor()]);
  return true;
};

export const roleGuard = (...roles: string[]): CanActivateFn => {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.isAuthenticated) return router.createUrlTree(['/login']);
    if (auth.mustChangePassword) return router.createUrlTree(['/change-password']);
    if (!roles.some(r => auth.hasRole(r))) return router.createUrlTree([auth.homeFor()]);
    return true;
  };
};

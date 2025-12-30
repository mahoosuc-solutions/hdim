import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router) {}

  canActivate(): boolean {
    // For demo purposes, always allow access
    // In production, check for valid JWT token
    const token = localStorage.getItem('auth_token');

    // Demo mode: allow without token
    if (!token) {
      // For demo, auto-set a demo token
      localStorage.setItem('auth_token', 'demo-admin-token');
      localStorage.setItem('user_role', 'ADMIN');
    }

    return true;
  }
}

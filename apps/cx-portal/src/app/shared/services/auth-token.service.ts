import { Injectable } from '@angular/core';

const STORAGE_KEY = 'cx_portal_auth_token';

@Injectable({ providedIn: 'root' })
export class AuthTokenService {
  get(): string {
    try {
      return localStorage.getItem(STORAGE_KEY) || '';
    } catch {
      return '';
    }
  }

  set(token: string): void {
    try {
      localStorage.setItem(STORAGE_KEY, token);
    } catch {
      // Ignore storage errors (private mode, blocked storage, etc).
    }
  }

  clear(): void {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // Ignore.
    }
  }

  has(): boolean {
    return !!this.get();
  }
}


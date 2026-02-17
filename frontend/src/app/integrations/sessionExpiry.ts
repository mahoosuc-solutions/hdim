export interface SessionExpiryHandlerOptions {
  storage?: Storage;
  tokenKeys?: string[];
  markerKey?: string;
  redirectUrl?: string;
  redirect?: (url: string) => void;
  onExpired?: () => void;
}

export function createSessionExpiryHandler(options: SessionExpiryHandlerOptions = {}): () => boolean {
  const storage = options.storage ?? localStorage;
  const tokenKeys = options.tokenKeys ?? ['authToken', 'refreshToken'];
  const markerKey = options.markerKey ?? 'sessionExpiredAt';
  const redirect = options.redirect ?? ((url: string) => window.location.assign(url));
  const redirectUrl = options.redirectUrl;
  const onExpired = options.onExpired;
  let handled = false;

  return () => {
    if (handled) {
      return false;
    }

    handled = true;

    for (const key of tokenKeys) {
      storage.removeItem(key);
    }

    storage.setItem(markerKey, new Date().toISOString());
    onExpired?.();

    if (redirectUrl) {
      redirect(redirectUrl);
    }

    return true;
  };
}

export interface SessionReauthCheckOptions {
  storage?: Storage;
  tokenKey?: string;
  markerKey?: string;
}

export function shouldRequireSessionReauth(options: SessionReauthCheckOptions = {}): boolean {
  const storage = options.storage ?? localStorage;
  const tokenKey = options.tokenKey ?? 'authToken';
  const markerKey = options.markerKey ?? 'sessionExpiredAt';

  const marker = storage.getItem(markerKey);
  if (!marker) {
    return false;
  }

  const token = storage.getItem(tokenKey);
  return !token;
}

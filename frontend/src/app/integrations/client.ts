export class SessionExpiredError extends Error {
  readonly status = 401;
  readonly path: string;

  constructor(path: string) {
    super(`Unauthorized - session expired (${path})`);
    this.name = 'SessionExpiredError';
    this.path = path;
  }
}

export interface ApiClientOptions {
  baseUrl?: string;
  tokenProvider?: () => string | null | undefined;
  onUnauthorized?: () => void;
  fetchImpl?: typeof fetch;
  defaultHeaders?: HeadersInit;
  shouldForceUnauthorized?: (path: string) => boolean;
}

export class ApiClient {
  private readonly baseUrl: string;
  private readonly tokenProvider?: () => string | null | undefined;
  private readonly onUnauthorized?: () => void;
  private readonly fetchImpl: typeof fetch;
  private readonly defaultHeaders: HeadersInit;
  private readonly shouldForceUnauthorized?: (path: string) => boolean;
  private unauthorizedHandled = false;

  constructor(options: ApiClientOptions = {}) {
    this.baseUrl = options.baseUrl ?? '';
    this.tokenProvider = options.tokenProvider;
    this.onUnauthorized = options.onUnauthorized;
    this.fetchImpl = options.fetchImpl ?? fetch;
    this.defaultHeaders = options.defaultHeaders ?? { Accept: 'application/json' };
    this.shouldForceUnauthorized = options.shouldForceUnauthorized;
  }

  resetUnauthorizedState(): void {
    this.unauthorizedHandled = false;
  }

  async get<T>(path: string, init: RequestInit = {}): Promise<T> {
    return this.request<T>(path, { ...init, method: init.method ?? 'GET' });
  }

  async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    if (this.shouldForceUnauthorized?.(path) === true) {
      if (!this.unauthorizedHandled) {
        this.unauthorizedHandled = true;
        this.onUnauthorized?.();
      }
      throw new SessionExpiredError(path);
    }

    const headers = new Headers(this.defaultHeaders);
    const incomingHeaders = new Headers(init.headers);

    incomingHeaders.forEach((value, key) => {
      headers.set(key, value);
    });

    const token = this.tokenProvider?.();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    const response = await this.fetchImpl(this.toUrl(path), {
      ...init,
      headers,
    });

    if (response.status === 401) {
      if (!this.unauthorizedHandled) {
        this.unauthorizedHandled = true;
        this.onUnauthorized?.();
      }
      throw new SessionExpiredError(path);
    }

    if (!response.ok) {
      throw new Error(`Request failed (${response.status} ${response.statusText})`);
    }

    const contentType = response.headers.get('content-type') ?? '';
    if (contentType.includes('application/json')) {
      return (await response.json()) as T;
    }

    const text = await response.text();
    return text as T;
  }

  private toUrl(path: string): string {
    if (!this.baseUrl) return path;
    if (path.startsWith('http://') || path.startsWith('https://')) return path;
    return `${this.baseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
  }
}

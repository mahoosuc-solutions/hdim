import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpHeaders } from '@angular/common/http';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService],
    });

    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    jest.restoreAllMocks();
  });

  it('adds default headers and logs responses', () => {
    const logSpy = jest.spyOn(console, 'log').mockImplementation(() => undefined);
    service.get('api/test', undefined, { apiVersion: 'v1' }).subscribe();

    const req = httpMock.expectOne('api/test');
    expect(req.request.headers.get('Content-Type')).toBe('application/json');
    expect(req.request.headers.get('API-Version')).toBe('v1');
    req.flush({ ok: true };

    expect(logSpy).toHaveBeenCalled();
  };

  it('supports POST, PUT, PATCH, and DELETE requests', () => {
    service.post('api/post', { name: 'test' }).subscribe();
    service.put('api/put', { name: 'test' }).subscribe();
    service.patch('api/patch', { name: 'test' }).subscribe();
    service.delete('api/delete').subscribe();

    httpMock.expectOne('api/post').flush({ ok: true };
    httpMock.expectOne('api/put').flush({ ok: true };
    httpMock.expectOne('api/patch').flush({ ok: true };
    httpMock.expectOne('api/delete').flush({ ok: true };
  };

  it('handles network errors with a friendly message', (done) => {
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    service.get('api/error', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toContain('Network error');
        expect(errorSpy).toHaveBeenCalled();
        done();
      },
    };

    const req = httpMock.expectOne('api/error');
    req.error(new ErrorEvent('NetworkError', { message: 'offline' }));
  });

  it('translates 404 errors', (done) => {
    service.get('api/missing', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe('Resource not found.');
        done();
      },
    };

    const req = httpMock.expectOne('api/missing');
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  });

  it('uses backend message for 409 conflicts', (done) => {
    service.get('api/conflict', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe('Already exists');
        done();
      },
    };

    const req = httpMock.expectOne('api/conflict');
    req.flush({ message: 'Already exists' }, { status: 409, statusText: 'Conflict' });
  });

  it('returns friendly message for service unavailable', (done) => {
    service.get('api/unavailable', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe('Service unavailable. Please try again later.');
        done();
      },
    };

    const req = httpMock.expectOne('api/unavailable');
    req.flush({}, { status: 503, statusText: 'Service Unavailable' });
  });

  it('handles status 0 connection errors', (done) => {
    service.get('api/down', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe(
          'Unable to connect to server. Please check your network connection.'
        );
        done();
      },
    };

    const req = httpMock.expectOne('api/down');
    req.flush({}, { status: 0, statusText: 'Unknown Error' });
  });

  it('translates authorization errors', (done) => {
    service.get('api/unauthorized', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe('Unauthorized. Please log in again.');
        done();
      },
    };

    const req = httpMock.expectOne('api/unauthorized');
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
  });

  it('translates access denied errors', (done) => {
    service.get('api/forbidden', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe(
          'Access denied. You do not have permission to access this resource.'
        );
        done();
      },
    };

    const req = httpMock.expectOne('api/forbidden');
    req.flush({}, { status: 403, statusText: 'Forbidden' });
  });

  it('translates validation errors', (done) => {
    service.get('api/validation', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe('Validation error. Please check your input.');
        done();
      },
    };

    const req = httpMock.expectOne('api/validation');
    req.flush({}, { status: 422, statusText: 'Unprocessable Entity' });
  });

  it('translates rate limiting errors', (done) => {
    service.get('api/rate', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe('Too many requests. Please try again later.');
        done();
      },
    };

    const req = httpMock.expectOne('api/rate');
    req.flush({}, { status: 429, statusText: 'Too Many Requests' });
  });

  it('translates server errors for 500/502/504', (done) => {
    const errors: string[] = [];
    service.get('api/server-500', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        errors.push(err.message);
      },
    };
    service.get('api/server-502', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        errors.push(err.message);
      },
    });
    service.get('api/server-504', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        errors.push(err.message);
        expect(errors).toEqual([
          'Internal server error. Please try again later.',
          'Bad gateway. The server is temporarily unavailable.',
          'Gateway timeout. The request took too long to complete.'
        ]);
        done();
      },
    });

    httpMock.expectOne('api/server-500').flush({}, { status: 500, statusText: 'Server Error' });
    httpMock.expectOne('api/server-502').flush({}, { status: 502, statusText: 'Bad Gateway' });
    httpMock.expectOne('api/server-504').flush({}, { status: 504, statusText: 'Gateway Timeout' });
  });

  it('uses default server error formatting for unknown status', (done) => {
    service.get('api/teapot', undefined, { retry: 0 }).subscribe({
      next: () => done.fail('expected error'),
      error: (err) => {
        expect(err.message).toBe('Teapot says no');
        done();
      },
    };

    const req = httpMock.expectOne('api/teapot');
    req.flush({ message: 'Teapot says no' }, { status: 418, statusText: 'Teapot' });
  });

  it('retries with exponential backoff on server errors', fakeAsync(() => {
    const responses: any[] = [];
    service.getWithRetry('api/retry', undefined, { maxRetries: 1 }).subscribe((response) => {
      responses.push(response);
    });

    const first = httpMock.expectOne('api/retry');
    first.flush({ message: 'fail' }, { status: 500, statusText: 'Server Error' });

    tick(1000);
    const retry = httpMock.expectOne('api/retry');
    retry.flush({ ok: true });

    tick();
    expect(responses).toEqual([{ ok: true }]);
  }));

  it('logs retry attempts when enabled', fakeAsync(() => {
    const logSpy = jest.spyOn(console, 'log').mockImplementation(() => undefined);
    service.getWithRetry('api/retry-log', undefined, { maxRetries: 1 }).subscribe({
      error: () => undefined,
    });

    const first = httpMock.expectOne('api/retry-log');
    first.flush({ message: 'fail' }, { status: 500, statusText: 'Server Error' });

    tick(1000);
    httpMock.expectOne('api/retry-log').flush({ ok: true });

    expect(
      logSpy.mock.calls.some((call) => String(call[0]).includes('Retry attempt'))
    ).toBe(true);
    logSpy.mockRestore();
  }));

  it('does not retry on client errors', fakeAsync(() => {
    const errors: any[] = [];
    service.getWithRetry('api/no-retry', undefined, { maxRetries: 1 }).subscribe({
      error: (err) => errors.push(err),
    });

    const req = httpMock.expectOne('api/no-retry');
    req.flush({ message: 'bad' }, { status: 400, statusText: 'Bad Request' });

    tick(1000);
    httpMock.expectNone('api/no-retry');
    expect(errors.length).toBe(1);
  }));

  it('respects custom headers in request options', () => {
    const headers = new HttpHeaders().set('Content-Type', 'application/xml');
    service.get('api/headers', undefined, { headers }).subscribe();

    const req = httpMock.expectOne('api/headers');
    expect(req.request.headers.get('Content-Type')).toBe('application/xml');
    req.flush({ ok: true });
  });
});

import { ErrorFactory, ErrorCode, ErrorSeverity } from './error.model';

describe('ErrorFactory', () => {
  it('creates network and timeout errors', () => {
    const network = ErrorFactory.createNetworkError({ detail: 'x' });
    expect(network.code).toBe(ErrorCode.NETWORK_ERROR);
    expect(network.severity).toBe(ErrorSeverity.ERROR);

    const timeout = ErrorFactory.createTimeoutError();
    expect(timeout.code).toBe(ErrorCode.TIMEOUT_ERROR);
    expect(timeout.severity).toBe(ErrorSeverity.WARNING);
  });

  it('creates not found and validation errors', () => {
    const notFound = ErrorFactory.createNotFoundError('Patient', 'p1');
    expect(notFound.message).toContain('Patient');
    expect(notFound.details?.resourceId).toBe('p1');

    const validation = ErrorFactory.createValidationError('name', 'required');
    expect(validation.code).toBe(ErrorCode.VALIDATION_ERROR);
    expect(validation.userMessage).toContain('name');
  });

  it('creates data operation errors', () => {
    const saveError = ErrorFactory.createDataSaveError('Report');
    expect(saveError.code).toBe(ErrorCode.DATA_SAVE_ERROR);

    const deleteError = ErrorFactory.createDataDeleteError('Report', 'r1');
    expect(deleteError.userMessage).toContain(ErrorCode.DATA_DELETE_ERROR);

    const exportError = ErrorFactory.createExportError('csv');
    expect(exportError.userMessage).toContain('CSV');
  });

  it('creates generic errors from unknown errors', () => {
    const err = ErrorFactory.createFromError(new Error('boom'));
    expect(err.code).toBe(ErrorCode.UNKNOWN_ERROR);
    expect(err.message).toContain('boom');

    const existing = ErrorFactory.createFromError({ code: ErrorCode.FORBIDDEN, message: 'forbidden' });
    expect(existing.code).toBe(ErrorCode.FORBIDDEN);
  });

  it('maps http statuses to typed errors', () => {
    expect(ErrorFactory.createFromHttpError(401, 'Unauthorized').code).toBe(ErrorCode.UNAUTHORIZED);
    expect(ErrorFactory.createFromHttpError(403, 'Forbidden').code).toBe(ErrorCode.FORBIDDEN);
    expect(ErrorFactory.createFromHttpError(404, 'Not Found').code).toBe(ErrorCode.RESOURCE_NOT_FOUND);
    expect(ErrorFactory.createFromHttpError(409, 'Conflict').code).toBe(ErrorCode.RESOURCE_CONFLICT);
    expect(ErrorFactory.createFromHttpError(500, 'Server').code).toBe(ErrorCode.INTERNAL_ERROR);
    expect(ErrorFactory.createFromHttpError(418, 'Teapot').code).toBe(ErrorCode.UNKNOWN_ERROR);
  });
});

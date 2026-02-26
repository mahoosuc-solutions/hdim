import { extractApiFieldErrors, getApiErrorMessage } from './api-error-parser';

describe('api-error-parser', () => {
  it('extracts first message as api error message', () => {
    const error = {
      error: {
        errors: [{ defaultMessage: 'Name is required' }],
      },
    };

    expect(getApiErrorMessage(error, 'fallback')).toBe('Name is required');
  });

  it('extracts field-level validation errors', () => {
    const error = {
      error: {
        fieldErrors: [
          { field: 'priority', defaultMessage: 'Priority must be LOW, MEDIUM, or HIGH' },
          { field: 'year', defaultMessage: 'Year must be >= 2000' },
        ],
      },
    };

    expect(extractApiFieldErrors(error)).toEqual({
      priority: 'Priority must be LOW, MEDIUM, or HIGH',
      year: 'Year must be >= 2000',
    });
  });

  it('falls back when payload is missing', () => {
    expect(getApiErrorMessage({}, 'fallback')).toBe('fallback');
    expect(extractApiFieldErrors({})).toEqual({});
  });
});

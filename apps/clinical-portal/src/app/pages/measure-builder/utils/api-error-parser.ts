export function getApiErrorMessage(error: unknown, fallback: string): string {
  const payload = (error as { error?: unknown } | undefined)?.error;
  if (!payload || typeof payload === 'string') {
    return fallback;
  }

  const typedPayload = payload as {
    errors?: Array<{ defaultMessage?: string; message?: string; error?: string }>;
    fieldErrors?: Array<{ defaultMessage?: string; message?: string; error?: string }>;
    violations?: Array<{ defaultMessage?: string; message?: string; error?: string }>;
    message?: string;
  };

  const collect = (
    items: Array<{ defaultMessage?: string; message?: string; error?: string }>
  ): string[] =>
    items
      .map((item) => item?.defaultMessage || item?.message || item?.error)
      .filter((value): value is string => !!value);

  const messages = [
    ...collect(typedPayload.errors || []),
    ...collect(typedPayload.fieldErrors || []),
    ...collect(typedPayload.violations || []),
  ];

  if (messages.length > 0) {
    return messages[0];
  }

  if (typeof typedPayload.message === 'string' && typedPayload.message.trim()) {
    return typedPayload.message;
  }

  return fallback;
}

export function extractApiFieldErrors(error: unknown): Record<string, string> {
  const payload = (error as { error?: unknown } | undefined)?.error;
  if (!payload || typeof payload === 'string') {
    return {};
  }

  const typedPayload = payload as {
    fieldErrors?: Array<{ field?: string; property?: string; path?: string; defaultMessage?: string; message?: string; error?: string }>;
    errors?: Array<{ field?: string; property?: string; path?: string; defaultMessage?: string; message?: string; error?: string }>;
    violations?: Array<{ field?: string; property?: string; path?: string; defaultMessage?: string; message?: string; error?: string }>;
  };

  const fieldErrors: Record<string, string> = {};
  const addFieldError = (field: string, message: string): void => {
    if (!field || !message || fieldErrors[field]) {
      return;
    }
    fieldErrors[field] = message;
  };

  const lists = [
    typedPayload.fieldErrors || [],
    typedPayload.errors || [],
    typedPayload.violations || [],
  ];

  for (const list of lists) {
    for (const item of list) {
      const field = item?.field || item?.property || item?.path;
      const message = item?.defaultMessage || item?.message || item?.error;
      if (typeof field === 'string' && typeof message === 'string') {
        addFieldError(field, message);
      }
    }
  }

  return fieldErrors;
}

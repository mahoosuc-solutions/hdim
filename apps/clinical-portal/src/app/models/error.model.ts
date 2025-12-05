/**
 * Error Model - Typed error handling with error codes for support reference
 */

/**
 * Error codes for different error types
 */
export enum ErrorCode {
  // Network Errors (1xxx)
  NETWORK_ERROR = 'ERR-1001',
  TIMEOUT_ERROR = 'ERR-1002',
  CONNECTION_REFUSED = 'ERR-1003',

  // Authentication/Authorization Errors (2xxx)
  UNAUTHORIZED = 'ERR-2001',
  FORBIDDEN = 'ERR-2002',
  SESSION_EXPIRED = 'ERR-2003',

  // Resource Errors (3xxx)
  RESOURCE_NOT_FOUND = 'ERR-3001',
  RESOURCE_ALREADY_EXISTS = 'ERR-3002',
  RESOURCE_CONFLICT = 'ERR-3003',

  // Validation Errors (4xxx)
  VALIDATION_ERROR = 'ERR-4001',
  INVALID_INPUT = 'ERR-4002',
  MISSING_REQUIRED_FIELD = 'ERR-4003',

  // Service Errors (5xxx)
  FHIR_SERVICE_ERROR = 'ERR-5001',
  CQL_ENGINE_ERROR = 'ERR-5002',
  QUALITY_MEASURE_ERROR = 'ERR-5003',
  EVALUATION_ERROR = 'ERR-5004',

  // Data Errors (6xxx)
  DATA_LOADING_ERROR = 'ERR-6001',
  DATA_SAVE_ERROR = 'ERR-6002',
  DATA_DELETE_ERROR = 'ERR-6003',
  EXPORT_ERROR = 'ERR-6004',
  IMPORT_ERROR = 'ERR-6005',

  // Generic Errors (9xxx)
  UNKNOWN_ERROR = 'ERR-9001',
  INTERNAL_ERROR = 'ERR-9002',
}

/**
 * Error severity levels
 */
export enum ErrorSeverity {
  INFO = 'info',
  WARNING = 'warning',
  ERROR = 'error',
  CRITICAL = 'critical',
}

/**
 * Application Error Interface
 */
export interface AppError {
  code: ErrorCode;
  message: string;
  userMessage: string;
  severity: ErrorSeverity;
  timestamp: Date;
  details?: any;
  stack?: string;
  requestId?: string;
}

/**
 * Error Factory - Creates typed errors with user-friendly messages
 */
export class ErrorFactory {
  /**
   * Create a network error
   */
  static createNetworkError(details?: any): AppError {
    return {
      code: ErrorCode.NETWORK_ERROR,
      message: 'Network request failed',
      userMessage: 'Unable to connect to the server. Please check your internet connection and try again.',
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details,
    };
  }

  /**
   * Create a timeout error
   */
  static createTimeoutError(details?: any): AppError {
    return {
      code: ErrorCode.TIMEOUT_ERROR,
      message: 'Request timed out',
      userMessage: 'The request took too long to complete. Please try again.',
      severity: ErrorSeverity.WARNING,
      timestamp: new Date(),
      details,
    };
  }

  /**
   * Create an unauthorized error
   */
  static createUnauthorizedError(details?: any): AppError {
    return {
      code: ErrorCode.UNAUTHORIZED,
      message: 'User not authenticated',
      userMessage: 'Your session has expired. Please log in again.',
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details,
    };
  }

  /**
   * Create a resource not found error
   */
  static createNotFoundError(resourceType: string, resourceId?: string): AppError {
    return {
      code: ErrorCode.RESOURCE_NOT_FOUND,
      message: `${resourceType} not found: ${resourceId || 'unknown'}`,
      userMessage: `The ${resourceType.toLowerCase()} you're looking for could not be found. It may have been deleted or moved.`,
      severity: ErrorSeverity.WARNING,
      timestamp: new Date(),
      details: { resourceType, resourceId },
    };
  }

  /**
   * Create a validation error
   */
  static createValidationError(field: string, reason: string): AppError {
    return {
      code: ErrorCode.VALIDATION_ERROR,
      message: `Validation failed for ${field}: ${reason}`,
      userMessage: `Please check your input for "${field}". ${reason}`,
      severity: ErrorSeverity.WARNING,
      timestamp: new Date(),
      details: { field, reason },
    };
  }

  /**
   * Create a FHIR service error
   */
  static createFhirServiceError(operation: string, details?: any): AppError {
    return {
      code: ErrorCode.FHIR_SERVICE_ERROR,
      message: `FHIR service error during ${operation}`,
      userMessage: 'Unable to access patient data. The FHIR server may be temporarily unavailable. Please try again in a few moments.',
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: { operation, ...details },
    };
  }

  /**
   * Create a CQL engine error
   */
  static createCqlEngineError(operation: string, details?: any): AppError {
    return {
      code: ErrorCode.CQL_ENGINE_ERROR,
      message: `CQL engine error during ${operation}`,
      userMessage: 'Unable to process quality measures. The CQL Engine service may be temporarily unavailable. Please try again later.',
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: { operation, ...details },
    };
  }

  /**
   * Create an evaluation error
   */
  static createEvaluationError(patientId: string, measureId: string, details?: any): AppError {
    return {
      code: ErrorCode.EVALUATION_ERROR,
      message: `Evaluation failed for patient ${patientId} and measure ${measureId}`,
      userMessage: 'Unable to complete the quality measure evaluation. Please verify the patient data and measure configuration.',
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: { patientId, measureId, ...details },
    };
  }

  /**
   * Create a data loading error
   */
  static createDataLoadingError(dataType: string, details?: any): AppError {
    return {
      code: ErrorCode.DATA_LOADING_ERROR,
      message: `Failed to load ${dataType}`,
      userMessage: `Unable to load ${dataType.toLowerCase()}. Please refresh the page and try again.`,
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: { dataType, ...details },
    };
  }

  /**
   * Create a data save error
   */
  static createDataSaveError(dataType: string, details?: any): AppError {
    return {
      code: ErrorCode.DATA_SAVE_ERROR,
      message: `Failed to save ${dataType}`,
      userMessage: `Unable to save ${dataType.toLowerCase()}. Please check your input and try again.`,
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: { dataType, ...details },
    };
  }

  /**
   * Create a data delete error
   */
  static createDataDeleteError(dataType: string, itemId: string, details?: any): AppError {
    return {
      code: ErrorCode.DATA_DELETE_ERROR,
      message: `Failed to delete ${dataType}: ${itemId}`,
      userMessage: `Unable to delete ${dataType.toLowerCase()}. It may be in use or you may not have permission. Error code: ${ErrorCode.DATA_DELETE_ERROR}`,
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: { dataType, itemId, ...details },
    };
  }

  /**
   * Create an export error
   */
  static createExportError(format: string, details?: any): AppError {
    return {
      code: ErrorCode.EXPORT_ERROR,
      message: `Failed to export data to ${format}`,
      userMessage: `Unable to export data to ${format.toUpperCase()}. Please try again or contact support with error code: ${ErrorCode.EXPORT_ERROR}`,
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: { format, ...details },
    };
  }

  /**
   * Create a generic error from an unknown error
   */
  static createFromError(error: any): AppError {
    if (error && typeof error === 'object' && 'code' in error) {
      return error as AppError;
    }

    return {
      code: ErrorCode.UNKNOWN_ERROR,
      message: error?.message || 'An unknown error occurred',
      userMessage: 'An unexpected error occurred. Please try again or contact support if the problem persists.',
      severity: ErrorSeverity.ERROR,
      timestamp: new Date(),
      details: error,
      stack: error?.stack,
    };
  }

  /**
   * Convert HTTP status code to appropriate error
   */
  static createFromHttpError(status: number, statusText: string, details?: any): AppError {
    switch (status) {
      case 401:
        return this.createUnauthorizedError(details);
      case 403:
        return {
          code: ErrorCode.FORBIDDEN,
          message: 'Access forbidden',
          userMessage: 'You do not have permission to perform this action.',
          severity: ErrorSeverity.ERROR,
          timestamp: new Date(),
          details,
        };
      case 404:
        return {
          code: ErrorCode.RESOURCE_NOT_FOUND,
          message: 'Resource not found',
          userMessage: 'The requested resource could not be found.',
          severity: ErrorSeverity.WARNING,
          timestamp: new Date(),
          details,
        };
      case 409:
        return {
          code: ErrorCode.RESOURCE_CONFLICT,
          message: 'Resource conflict',
          userMessage: 'A conflict occurred. The resource may have been modified by another user.',
          severity: ErrorSeverity.WARNING,
          timestamp: new Date(),
          details,
        };
      case 500:
      case 502:
      case 503:
      case 504:
        return {
          code: ErrorCode.INTERNAL_ERROR,
          message: `Server error: ${status} ${statusText}`,
          userMessage: 'A server error occurred. Please try again later or contact support if the problem persists.',
          severity: ErrorSeverity.ERROR,
          timestamp: new Date(),
          details,
        };
      default:
        return {
          code: ErrorCode.UNKNOWN_ERROR,
          message: `HTTP error: ${status} ${statusText}`,
          userMessage: 'An error occurred while processing your request. Please try again.',
          severity: ErrorSeverity.ERROR,
          timestamp: new Date(),
          details,
        };
    }
  }
}

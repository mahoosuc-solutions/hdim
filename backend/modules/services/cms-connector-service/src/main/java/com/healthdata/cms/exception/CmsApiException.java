package com.healthdata.cms.exception;

import com.healthdata.cms.model.CmsApiProvider;

/**
 * Base exception for CMS API errors
 */
public class CmsApiException extends RuntimeException {

    private final CmsApiProvider provider;
    private final String errorCode;
    private final int httpStatus;

    public CmsApiException(String message, CmsApiProvider provider) {
        super(message);
        this.provider = provider;
        this.errorCode = null;
        this.httpStatus = -1;
    }

    public CmsApiException(String message, CmsApiProvider provider, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = null;
        this.httpStatus = -1;
    }

    public CmsApiException(String message, CmsApiProvider provider, String errorCode, int httpStatus) {
        super(message);
        this.provider = provider;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public CmsApiException(String message, CmsApiProvider provider, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public CmsApiProvider getProvider() {
        return provider;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public boolean isRetriable() {
        // HTTP 5xx and timeout errors are retriable
        // HTTP 4xx errors (except 429) are not retriable
        return httpStatus >= 500 || httpStatus == 429;
    }
}

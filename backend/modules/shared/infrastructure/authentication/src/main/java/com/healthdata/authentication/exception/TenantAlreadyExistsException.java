package com.healthdata.authentication.exception;

import com.healthdata.common.exception.HdimBusinessException.DuplicateEntityException;

/**
 * Exception thrown when attempting to create a tenant that already exists.
 */
public class TenantAlreadyExistsException extends DuplicateEntityException {

    public TenantAlreadyExistsException(String tenantId) {
        super("Tenant", tenantId);
    }
}

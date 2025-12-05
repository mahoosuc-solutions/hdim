package com.healthdata.fhir.models;

import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

/**
 * Fluent builder for constructing FHIR bundles with consistent defaults.
 */
public final class FhirBundleBuilder {

    private final Bundle bundle;
    private Instant explicitTimestamp;

    private FhirBundleBuilder(Bundle.BundleType type) {
        this.bundle = new Bundle();
        this.bundle.setType(type);
        this.bundle.setId(UUID.randomUUID().toString());
    }

    /**
     * Creates a builder for transaction bundles.
     */
    public static FhirBundleBuilder transactionBundle() {
        return new FhirBundleBuilder(Bundle.BundleType.TRANSACTION);
    }

    /**
     * Allows overriding the default bundle timestamp.
     */
    public FhirBundleBuilder withTimestamp(Instant timestamp) {
        this.explicitTimestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        return this;
    }

    /**
     * Adds an entry to the bundle.
     *
     * @param resource The FHIR resource to include
     * @param method   The HTTP verb
     * @param url      The relative request URL (e.g., "Patient/123")
     */
    public FhirBundleBuilder addEntry(Resource resource, Bundle.HTTPVerb method, String url) {
        Objects.requireNonNull(resource, "resource must not be null");
        if (method == null) {
            throw new IllegalArgumentException("HTTP method must not be null");
        }
        if (StringUtils.isBlank(url)) {
            throw new IllegalArgumentException("Request url must not be blank");
        }
        Bundle.BundleEntryComponent entry = bundle.addEntry();
        entry.setResource(resource);
        entry.getRequest().setMethod(method);
        entry.getRequest().setUrl(url);
        return this;
    }

    /**
     * Builds the bundle, assigning timestamps and metadata where required.
     */
    public Bundle build() {
        Instant timestamp = explicitTimestamp != null ? explicitTimestamp : Instant.now();
        Date timestampAsDate = Date.from(timestamp);
        bundle.setTimestamp(timestampAsDate);
        bundle.getMeta().setLastUpdated(timestampAsDate);
        return bundle;
    }
}

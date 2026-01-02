package com.healthdata.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuditMetadataTest {

    @Test
    @DisplayName("forCreate should populate creator fields and timestamps")
    void forCreate_shouldPopulateInitialAuditFields() {
        Instant beforeCall = Instant.now();
        AuditMetadata metadata = AuditMetadata.forCreate("user-1", "org-9");
        Instant afterCall = Instant.now();

        assertThat(metadata.getCreatedBy()).isEqualTo("user-1");
        assertThat(metadata.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(metadata.getOrganizationId()).isEqualTo("org-9");

        assertThat(metadata.getCreatedAt()).isBetween(beforeCall, afterCall);
        assertThat(metadata.getLastModifiedAt()).isBetween(beforeCall, afterCall);

        assertThat(metadata.getIpAddress()).isNull();
        assertThat(metadata.getUserAgent()).isNull();
        assertThat(metadata.getSessionId()).isNull();
        assertThat(metadata.getAccessPurpose()).isNull();
        assertThat(metadata.getContext()).isNull();
    }

    @Test
    @DisplayName("markModified should update modifier fields and timestamp")
    void markModified_shouldUpdateModifierInformation() {
        AuditMetadata metadata = AuditMetadata.forCreate("user-1", "org-9");
        Instant originalLastModified = metadata.getLastModifiedAt();

        metadata.markModified("user-2");

        assertThat(metadata.getLastModifiedBy()).isEqualTo("user-2");
        assertThat(metadata.getLastModifiedAt()).isAfterOrEqualTo(originalLastModified);
    }
}

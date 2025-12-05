package com.healthdata.ehr.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for EhrVendorType enum.
 */
@DisplayName("EHR Vendor Type Tests")
class EhrVendorTypeTest {

    @Test
    @DisplayName("Should have all expected vendor types")
    void shouldHaveAllVendorTypes() {
        assertThat(EhrVendorType.values()).containsExactlyInAnyOrder(
                EhrVendorType.EPIC,
                EhrVendorType.CERNER,
                EhrVendorType.ATHENA,
                EhrVendorType.GENERIC
        );
    }

    @Test
    @DisplayName("Should convert from string")
    void shouldConvertFromString() {
        assertThat(EhrVendorType.valueOf("EPIC")).isEqualTo(EhrVendorType.EPIC);
        assertThat(EhrVendorType.valueOf("CERNER")).isEqualTo(EhrVendorType.CERNER);
        assertThat(EhrVendorType.valueOf("ATHENA")).isEqualTo(EhrVendorType.ATHENA);
        assertThat(EhrVendorType.valueOf("GENERIC")).isEqualTo(EhrVendorType.GENERIC);
    }
}

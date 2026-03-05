package com.healthdata.investor.entity;

import com.healthdata.security.encryption.Encrypted;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Zoho OAuth token fields are annotated for encryption at rest (B4).
 */
@Tag("unit")
class ZohoConnectionEncryptionTest {

    @Test
    void accessToken_shouldHaveEncryptedAnnotation() throws Exception {
        Field field = ZohoConnection.class.getDeclaredField("accessToken");
        Encrypted annotation = field.getAnnotation(Encrypted.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.category()).isEqualTo(Encrypted.HipaaCategory.SENSITIVE);
    }

    @Test
    void refreshToken_shouldHaveEncryptedAnnotation() throws Exception {
        Field field = ZohoConnection.class.getDeclaredField("refreshToken");
        Encrypted annotation = field.getAnnotation(Encrypted.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.category()).isEqualTo(Encrypted.HipaaCategory.SENSITIVE);
    }
}

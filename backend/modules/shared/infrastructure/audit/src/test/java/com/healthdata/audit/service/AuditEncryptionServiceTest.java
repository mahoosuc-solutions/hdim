package com.healthdata.audit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AuditEncryptionService.
 */
class AuditEncryptionServiceTest {

    private AuditEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Use temporary key for testing
        encryptionService = new AuditEncryptionService(null);
    }

    @Test
    void testEncryptDecrypt() {
        // Given
        String plaintext = "Sensitive PHI data: John Doe, SSN: 123-45-6789";

        // When
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertNotEquals(plaintext, encrypted, "Encrypted data should be different from plaintext");
        assertEquals(plaintext, decrypted, "Decrypted data should match original plaintext");
    }

    @Test
    void testEncryptionProducesUniqueOutput() {
        // Given
        String plaintext = "Test data";

        // When - encrypt the same data twice
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Then - should produce different ciphertext due to random IV
        assertNotEquals(encrypted1, encrypted2, "Each encryption should produce unique output");

        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testEncryptEmptyString() {
        // Given
        String plaintext = "";

        // When
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptUnicodeCharacters() {
        // Given
        String plaintext = "Unicode test: 你好世界 🏥 🔒";

        // When
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testDecryptInvalidData() {
        // Given
        String invalidEncryptedData = "not-valid-encrypted-data";

        // When/Then
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(invalidEncryptedData));
    }

    @Test
    void testIsConfigured() {
        // Then
        assertTrue(encryptionService.isConfigured(), "Encryption service should be configured");
    }
}

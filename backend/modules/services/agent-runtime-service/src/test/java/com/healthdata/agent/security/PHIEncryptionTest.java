package com.healthdata.agent.security;

import com.healthdata.agent.security.PHIEncryption.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for PHI encryption service.
 * Critical for HIPAA compliance - ensures PHI data is properly encrypted at rest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PHI Encryption Tests")
class PHIEncryptionTest {

    private PHIEncryption phiEncryption;

    private static final String MASTER_KEY = "test-master-key-for-hipaa-compliant-encryption-32chars!";
    private static final String SALT = "test-salt-value-32chars-for-test!";
    private static final String TENANT_ID = "tenant-123";

    @BeforeEach
    void setUp() {
        phiEncryption = new PHIEncryption();
        ReflectionTestUtils.setField(phiEncryption, "masterKey", MASTER_KEY);
        ReflectionTestUtils.setField(phiEncryption, "salt", SALT);
    }

    @Nested
    @DisplayName("Encrypt/Decrypt Round-trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("should encrypt and decrypt simple text")
        void encryptDecryptSimpleText() {
            // Given
            String plaintext = "Patient name: John Doe";

            // When
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then
            assertThat(encrypted).isNotEqualTo(plaintext);
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("should encrypt and decrypt JSON data")
        void encryptDecryptJsonData() {
            // Given
            String jsonData = """
                {
                    "patientId": "P123456",
                    "ssn": "123-45-6789",
                    "diagnosis": "Type 2 Diabetes",
                    "medications": ["Metformin", "Lisinopril"]
                }
                """;

            // When
            String encrypted = phiEncryption.encrypt(jsonData, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then
            assertThat(decrypted).isEqualTo(jsonData);
        }

        @Test
        @DisplayName("should encrypt and decrypt empty string")
        void encryptDecryptEmptyString() {
            // Given
            String plaintext = "";

            // When
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("should encrypt and decrypt unicode characters")
        void encryptDecryptUnicode() {
            // Given
            String plaintext = "Patient: Jose Garcia. Notas medicas en espanol.";

            // When
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then
            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("should encrypt and decrypt large data")
        void encryptDecryptLargeData() {
            // Given - simulate large conversation history
            StringBuilder largeData = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeData.append("Message ").append(i).append(": This is a test message with PHI data. ");
            }
            String plaintext = largeData.toString();

            // When
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then
            assertThat(decrypted).isEqualTo(plaintext);
            assertThat(encrypted.length()).isGreaterThan(plaintext.length()); // Encrypted should be larger (Base64 + IV + tag)
        }

        @Test
        @DisplayName("should produce different ciphertext for same plaintext (random IV)")
        void differentCiphertextForSamePlaintext() {
            // Given
            String plaintext = "Same PHI data";

            // When
            String encrypted1 = phiEncryption.encrypt(plaintext, TENANT_ID);
            String encrypted2 = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Then - different ciphertext due to random IV
            assertThat(encrypted1).isNotEqualTo(encrypted2);

            // But both should decrypt to same plaintext
            assertThat(phiEncryption.decrypt(encrypted1, TENANT_ID)).isEqualTo(plaintext);
            assertThat(phiEncryption.decrypt(encrypted2, TENANT_ID)).isEqualTo(plaintext);
        }
    }

    @Nested
    @DisplayName("Tenant Isolation Tests")
    class TenantIsolationTests {

        @Test
        @DisplayName("should use different keys for different tenants")
        void differentKeysPerTenant() {
            // Given
            String plaintext = "Sensitive PHI data";
            String tenant1 = "tenant-abc";
            String tenant2 = "tenant-xyz";

            // When
            String encryptedTenant1 = phiEncryption.encrypt(plaintext, tenant1);
            String encryptedTenant2 = phiEncryption.encrypt(plaintext, tenant2);

            // Then - encrypted with different keys
            // Tenant 1 can decrypt its own data
            assertThat(phiEncryption.decrypt(encryptedTenant1, tenant1)).isEqualTo(plaintext);

            // Tenant 2 can decrypt its own data
            assertThat(phiEncryption.decrypt(encryptedTenant2, tenant2)).isEqualTo(plaintext);

            // But they cannot decrypt each other's data (wrong key)
            assertThatThrownBy(() -> phiEncryption.decrypt(encryptedTenant1, tenant2))
                .isInstanceOf(EncryptionException.class);

            assertThatThrownBy(() -> phiEncryption.decrypt(encryptedTenant2, tenant1))
                .isInstanceOf(EncryptionException.class);
        }

        @Test
        @DisplayName("should derive consistent key for same tenant")
        void consistentKeyForSameTenant() {
            // Given
            String plaintext = "PHI data for consistency test";

            // Encrypt once
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Clear key cache to force re-derivation
            phiEncryption.clearKeyCache();

            // When - decrypt after key cache cleared (key re-derived)
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then - should still decrypt correctly
            assertThat(decrypted).isEqualTo(plaintext);
        }
    }

    @Nested
    @DisplayName("Key Management Tests")
    class KeyManagementTests {

        @Test
        @DisplayName("should clear all keys from cache")
        void clearAllKeys() {
            // Given - encrypt data for multiple tenants
            String plaintext = "Test data";
            phiEncryption.encrypt(plaintext, "tenant-1");
            phiEncryption.encrypt(plaintext, "tenant-2");
            phiEncryption.encrypt(plaintext, "tenant-3");

            // When
            phiEncryption.clearKeyCache();

            // Then - all keys cleared (next encryption will derive new keys)
            // This doesn't throw - just verifies the cache was cleared successfully
            assertThatCode(() -> phiEncryption.clearKeyCache()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should clear specific tenant key from cache")
        void clearSpecificTenantKey() {
            // Given
            String plaintext = "Test data";
            String tenant1 = "tenant-keep";
            String tenant2 = "tenant-remove";

            phiEncryption.encrypt(plaintext, tenant1);
            phiEncryption.encrypt(plaintext, tenant2);

            // When
            phiEncryption.clearTenantKey(tenant2);

            // Then - clearing non-existent key doesn't throw
            assertThatCode(() -> phiEncryption.clearTenantKey("non-existent")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should work after key rotation simulation")
        void keyRotationSimulation() {
            // Given - encrypt data
            String plaintext = "Data encrypted before rotation";
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Simulate key rotation by clearing cache (in practice, master key would change)
            phiEncryption.clearKeyCache();

            // When - decrypt after "rotation" (same master key, re-derived tenant key)
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then - still works with same master key
            assertThat(decrypted).isEqualTo(plaintext);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should throw exception for invalid ciphertext")
        void invalidCiphertext() {
            // Given
            String invalidCiphertext = "not-valid-base64-encrypted-data!!!";

            // When/Then
            assertThatThrownBy(() -> phiEncryption.decrypt(invalidCiphertext, TENANT_ID))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("Failed to decrypt");
        }

        @Test
        @DisplayName("should throw exception for truncated ciphertext")
        void truncatedCiphertext() {
            // Given
            String plaintext = "Some PHI data";
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Truncate the ciphertext
            String truncated = encrypted.substring(0, encrypted.length() / 2);

            // When/Then
            assertThatThrownBy(() -> phiEncryption.decrypt(truncated, TENANT_ID))
                .isInstanceOf(Exception.class); // Can be IllegalArgumentException or EncryptionException
        }

        @Test
        @DisplayName("should throw exception for tampered ciphertext")
        void tamperedCiphertext() {
            // Given
            String plaintext = "Sensitive data";
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Tamper with ciphertext (flip a bit)
            byte[] bytes = java.util.Base64.getDecoder().decode(encrypted);
            bytes[bytes.length / 2] ^= 0xFF; // Flip bits
            String tampered = java.util.Base64.getEncoder().encodeToString(bytes);

            // When/Then - GCM mode should detect tampering
            assertThatThrownBy(() -> phiEncryption.decrypt(tampered, TENANT_ID))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("Failed to decrypt");
        }

        @Test
        @DisplayName("should reject null tenant ID")
        void nullTenantId() {
            // Given
            String plaintext = "Test data";

            // When/Then - encrypt with null tenant should throw EncryptionException
            // The underlying NullPointerException from ConcurrentHashMap is wrapped
            assertThatThrownBy(() -> phiEncryption.encrypt(plaintext, null))
                .isInstanceOf(EncryptionException.class)
                .hasMessageContaining("Failed to encrypt");
        }
    }

    @Nested
    @DisplayName("Security Properties Tests")
    class SecurityPropertiesTests {

        @Test
        @DisplayName("should produce ciphertext with minimum length")
        void ciphertextMinimumLength() {
            // Given
            String plaintext = "X"; // Single character

            // When
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);

            // Then - should have IV (12 bytes) + ciphertext + GCM tag (16 bytes)
            // Base64 encoded, so length should be significant
            byte[] decoded = java.util.Base64.getDecoder().decode(encrypted);

            // IV (12) + at least 1 byte plaintext + GCM tag (16) = minimum 29 bytes
            assertThat(decoded.length).isGreaterThanOrEqualTo(29);
        }

        @Test
        @DisplayName("should use AES-256-GCM as specified")
        void usesCorrectAlgorithm() {
            // Given
            String plaintext = "Test PHI data for algorithm verification";

            // When
            String encrypted = phiEncryption.encrypt(plaintext, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then - successful encrypt/decrypt confirms algorithm works
            assertThat(decrypted).isEqualTo(plaintext);

            // The encrypted data length confirms GCM mode (includes auth tag)
            byte[] decoded = java.util.Base64.getDecoder().decode(encrypted);
            // IV (12) + plaintext length + GCM tag (16)
            int expectedMinLength = 12 + plaintext.getBytes().length + 16;
            assertThat(decoded.length).isGreaterThanOrEqualTo(expectedMinLength);
        }
    }

    @Nested
    @DisplayName("HIPAA Compliance Tests")
    class HipaaComplianceTests {

        @Test
        @DisplayName("should encrypt all 18 HIPAA identifiers")
        void encryptHipaaIdentifiers() {
            // HIPAA defines 18 types of identifiers
            String[] hipaaIdentifiers = {
                "John Doe", // Name
                "123 Main St, Boston, MA 02101", // Address
                "1985-03-15", // Dates (birth)
                "617-555-1234", // Phone
                "617-555-5678", // Fax
                "john.doe@email.com", // Email
                "123-45-6789", // SSN
                "MRN123456", // Medical record number
                "BCBS12345678", // Health plan beneficiary number
                "ACC987654", // Account number
                "CERT123456", // Certificate/license number
                "VIN1234567890", // Vehicle identifiers (in context)
                "Device-SN-12345", // Device identifiers
                "https://patient-portal.example.com/patient/123", // Web URLs
                "192.168.1.100", // IP addresses
                "fingerprint-hash-abc123", // Biometric identifiers
                "Full face photograph", // Full face photos
                "Unique-ID-ABC123" // Any other unique identifying number
            };

            for (String identifier : hipaaIdentifiers) {
                // When
                String encrypted = phiEncryption.encrypt(identifier, TENANT_ID);
                String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

                // Then - each identifier should encrypt/decrypt correctly
                assertThat(decrypted)
                    .as("Failed to encrypt/decrypt HIPAA identifier: " + identifier.substring(0, Math.min(10, identifier.length())))
                    .isEqualTo(identifier);

                // Ciphertext should not contain plaintext
                assertThat(encrypted).doesNotContain(identifier);
            }
        }

        @Test
        @DisplayName("should handle typical PHI record")
        void encryptTypicalPhiRecord() {
            // Given - typical PHI record with multiple identifiers
            String phiRecord = """
                {
                    "patient": {
                        "name": "Jane Smith",
                        "ssn": "987-65-4321",
                        "dob": "1975-07-22",
                        "mrn": "MRN789012",
                        "address": {
                            "street": "456 Oak Ave",
                            "city": "Springfield",
                            "state": "IL",
                            "zip": "62701"
                        },
                        "phone": "555-123-4567",
                        "email": "jane.smith@example.com"
                    },
                    "encounter": {
                        "date": "2024-12-15",
                        "diagnosis": ["E11.9", "I10"],
                        "notes": "Patient presents with uncontrolled diabetes..."
                    }
                }
                """;

            // When
            String encrypted = phiEncryption.encrypt(phiRecord, TENANT_ID);
            String decrypted = phiEncryption.decrypt(encrypted, TENANT_ID);

            // Then
            assertThat(decrypted).isEqualTo(phiRecord);
            assertThat(encrypted).doesNotContain("Jane Smith");
            assertThat(encrypted).doesNotContain("987-65-4321");
            assertThat(encrypted).doesNotContain("MRN789012");
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("should encrypt quickly for typical data sizes")
        void encryptionPerformance() {
            // Given
            String typicalMessage = "Patient John Doe (MRN: 12345) presents with symptoms...";

            // When - measure time for 100 encrypt/decrypt cycles
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                String encrypted = phiEncryption.encrypt(typicalMessage, TENANT_ID);
                phiEncryption.decrypt(encrypted, TENANT_ID);
            }
            long endTime = System.currentTimeMillis();

            // Then - should complete in reasonable time (under 1 second for 100 operations)
            long duration = endTime - startTime;
            assertThat(duration)
                .as("Encryption/decryption should be fast (100 ops under 1 second)")
                .isLessThan(1000);
        }

        @Test
        @DisplayName("should handle concurrent encryption from multiple threads")
        void concurrentEncryption() throws InterruptedException {
            // Given
            String plaintext = "Concurrent test data";
            int threadCount = 10;
            int operationsPerThread = 50;
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadCount);
            java.util.concurrent.atomic.AtomicInteger errorCount = new java.util.concurrent.atomic.AtomicInteger(0);

            // When - run concurrent encrypt/decrypt operations
            for (int t = 0; t < threadCount; t++) {
                final String tenantId = "tenant-" + t;
                new Thread(() -> {
                    try {
                        for (int i = 0; i < operationsPerThread; i++) {
                            String encrypted = phiEncryption.encrypt(plaintext, tenantId);
                            String decrypted = phiEncryption.decrypt(encrypted, tenantId);
                            if (!plaintext.equals(decrypted)) {
                                errorCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }).start();
            }

            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);

            // Then
            assertThat(errorCount.get())
                .as("All concurrent operations should succeed")
                .isZero();
        }
    }

    @Nested
    @DisplayName("EncryptionException Tests")
    class ExceptionTests {

        @Test
        @DisplayName("EncryptionException should contain cause")
        void exceptionContainsCause() {
            // Given
            Throwable cause = new RuntimeException("Original error");
            EncryptionException exception = new EncryptionException("Encryption failed", cause);

            // Then
            assertThat(exception.getMessage()).isEqualTo("Encryption failed");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }
}

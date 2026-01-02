package com.healthdata.audit.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * HIPAA-compliant encryption service for audit data.
 *
 * Uses AES-256-GCM for encryption (NIST approved, FIPS 140-2 compliant).
 *
 * Security requirements:
 * - AES-256 encryption
 * - Unique IV for each encryption
 * - Authenticated encryption (GCM mode)
 * - Secure key management
 */
@Service
public class AuditEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(AuditEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int GCM_IV_LENGTH = 12; // bytes

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Constructor.
     *
     * @param encryptionKey Base64-encoded 256-bit AES key.
     *                     In production, this should come from a secure key management service (AWS KMS, HashiCorp Vault, etc.)
     */
    public AuditEncryptionService(
        @Value("${audit.encryption.key:#{null}}") String encryptionKey) {

        this.secureRandom = new SecureRandom();

        if (encryptionKey != null && !encryptionKey.isBlank()) {
            // Use provided key
            byte[] keyBytes = Base64.getDecoder().decode(encryptionKey);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
            logger.info("Audit encryption enabled with provided key");
        } else {
            // Generate a temporary key (NOT for production use)
            try {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(256, secureRandom);
                this.secretKey = keyGen.generateKey();
                logger.warn("Audit encryption using TEMPORARY key. Configure 'audit.encryption.key' for production!");
                logger.warn("Temporary key (Base64): {}", Base64.getEncoder().encodeToString(secretKey.getEncoded()));
            } catch (Exception e) {
                throw new IllegalStateException("Failed to generate audit encryption key", e);
            }
        }
    }

    /**
     * Encrypt sensitive data.
     *
     * @param plaintext The data to encrypt
     * @return Base64-encoded encrypted data with IV prepended
     */
    public String encrypt(String plaintext) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintextBytes);

            // Combine IV + ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Encode to Base64
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt audit data", e);
        }
    }

    /**
     * Decrypt encrypted data.
     *
     * @param encryptedData Base64-encoded encrypted data with IV prepended
     * @return Decrypted plaintext
     */
    public String decrypt(String encryptedData) {
        try {
            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt audit data", e);
        }
    }

    /**
     * Check if encryption is properly configured.
     */
    public boolean isConfigured() {
        return secretKey != null;
    }
}

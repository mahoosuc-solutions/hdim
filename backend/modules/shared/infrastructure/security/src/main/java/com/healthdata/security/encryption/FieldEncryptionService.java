package com.healthdata.security.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Field-Level Encryption Service for PII/PHI Data
 *
 * Implements AES-256-GCM encryption for sensitive fields stored in the database.
 * Compliant with HIPAA encryption requirements for PHI at rest.
 *
 * Features:
 * - AES-256-GCM authenticated encryption
 * - Per-field random IV (initialization vector)
 * - Key derivation from master key using PBKDF2
 * - Automatic IV rotation per encryption
 */
@Service
public class FieldEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(FieldEncryptionService.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int AES_KEY_SIZE = 256;
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final String KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String ENCRYPTION_PREFIX = "ENC:";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public FieldEncryptionService(
            @Value("${encryption.master-key:}") String masterKey,
            @Value("${encryption.salt:hdim-phi-salt-2025}") String salt) {

        if (masterKey == null || masterKey.isEmpty()) {
            log.warn("No encryption master key configured. Using default key (NOT SECURE FOR PRODUCTION)");
            masterKey = "default-insecure-key-change-me-in-production-256bit";
        }

        this.secretKey = deriveKey(masterKey, salt);
        this.secureRandom = new SecureRandom();

        log.info("Field encryption service initialized with AES-256-GCM");
    }

    /**
     * Encrypt a plaintext value
     *
     * @param plaintext The value to encrypt
     * @return Base64-encoded encrypted value with IV prepended, prefixed with "ENC:"
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        // Don't double-encrypt
        if (plaintext.startsWith(ENCRYPTION_PREFIX)) {
            return plaintext;
        }

        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Return prefixed Base64-encoded result
            return ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new EncryptionException("Failed to encrypt value", e);
        }
    }

    /**
     * Decrypt an encrypted value
     *
     * @param ciphertext The Base64-encoded encrypted value (with "ENC:" prefix)
     * @return The decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }

        // Check if value is encrypted
        if (!ciphertext.startsWith(ENCRYPTION_PREFIX)) {
            // Return as-is if not encrypted (migration support)
            return ciphertext;
        }

        try {
            // Remove prefix and decode
            String encoded = ciphertext.substring(ENCRYPTION_PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(encoded);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(encryptedBytes);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new EncryptionException("Failed to decrypt value", e);
        }
    }

    /**
     * Check if a value is encrypted
     */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTION_PREFIX);
    }

    /**
     * Derive encryption key from master key using PBKDF2
     */
    private SecretKey deriveKey(String masterKey, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM);
            KeySpec spec = new PBEKeySpec(
                    masterKey.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8),
                    PBKDF2_ITERATIONS,
                    AES_KEY_SIZE);

            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");

        } catch (Exception e) {
            log.error("Key derivation failed", e);
            throw new EncryptionException("Failed to derive encryption key", e);
        }
    }

    /**
     * Encryption exception
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

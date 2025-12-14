package com.healthdata.agent.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PHI encryption service for protecting sensitive data in memory stores.
 * Uses AES-256-GCM encryption with tenant-specific keys.
 */
@Slf4j
@Component
public class PHIEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;

    @Value("${hdim.agent.security.encryption.master-key}")
    private String masterKey;

    @Value("${hdim.agent.security.encryption.salt}")
    private String salt;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SecretKey> keyCache = new ConcurrentHashMap<>();

    /**
     * Encrypt data with tenant-specific key.
     */
    public String encrypt(String plaintext, String tenantId) {
        try {
            SecretKey key = getOrCreateKey(tenantId);

            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypt data with tenant-specific key.
     */
    public String decrypt(String ciphertext, String tenantId) {
        try {
            SecretKey key = getOrCreateKey(tenantId);

            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            // Initialize cipher
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            // Decrypt
            byte[] plaintext = cipher.doFinal(encryptedData);
            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new EncryptionException("Failed to decrypt data", e);
        }
    }

    /**
     * Get or create tenant-specific key from master key.
     */
    private SecretKey getOrCreateKey(String tenantId) {
        return keyCache.computeIfAbsent(tenantId, tid -> {
            try {
                // Derive tenant-specific key from master key + tenant ID
                String keyInput = masterKey + ":" + tid;
                String saltInput = salt + ":" + tid;

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                KeySpec spec = new PBEKeySpec(
                    keyInput.toCharArray(),
                    saltInput.getBytes(StandardCharsets.UTF_8),
                    ITERATION_COUNT,
                    KEY_LENGTH
                );

                byte[] keyBytes = factory.generateSecret(spec).getEncoded();
                return new SecretKeySpec(keyBytes, "AES");

            } catch (Exception e) {
                log.error("Failed to derive key for tenant {}: {}", tenantId, e.getMessage());
                throw new EncryptionException("Failed to derive encryption key", e);
            }
        });
    }

    /**
     * Clear cached keys (for key rotation).
     */
    public void clearKeyCache() {
        keyCache.clear();
        log.info("Encryption key cache cleared");
    }

    /**
     * Clear specific tenant key from cache.
     */
    public void clearTenantKey(String tenantId) {
        keyCache.remove(tenantId);
        log.info("Cleared encryption key for tenant: {}", tenantId);
    }

    /**
     * Exception for encryption operations.
     */
    public static class EncryptionException extends RuntimeException {
        public EncryptionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

package com.healthdata.security.encryption;

import jakarta.persistence.Convert;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking fields that should be encrypted at rest.
 *
 * Apply to entity fields containing PII/PHI data that must be encrypted
 * in the database for HIPAA compliance.
 *
 * Usage:
 * <pre>
 * {@code
 * @Entity
 * public class Patient {
 *     @Encrypted
 *     @Column(name = "ssn", length = 500) // Encrypted values are longer
 *     private String socialSecurityNumber;
 *
 *     @Encrypted
 *     @Column(name = "date_of_birth", length = 500)
 *     private String dateOfBirth;
 *
 *     @Encrypted
 *     @Column(name = "address", length = 1000)
 *     private String address;
 * }
 * }
 * </pre>
 *
 * Note: Column length should be increased for encrypted fields as
 * encryption adds overhead (Base64 encoding, IV, authentication tag).
 * A good rule of thumb is: encrypted_length = (plaintext_length * 2) + 100
 *
 * Protected Field Types (recommended for encryption):
 * - Social Security Number (SSN)
 * - Date of Birth
 * - Medical Record Number (MRN)
 * - Phone numbers
 * - Email addresses
 * - Street addresses
 * - Insurance policy numbers
 * - Lab results and diagnoses
 * - Any other PHI under HIPAA
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Convert(converter = EncryptedStringConverter.class)
public @interface Encrypted {
    /**
     * Description of the encrypted field for documentation purposes
     */
    String value() default "";

    /**
     * HIPAA data category for audit purposes
     */
    HipaaCategory category() default HipaaCategory.PHI;

    /**
     * HIPAA data categories
     */
    enum HipaaCategory {
        /**
         * Protected Health Information
         */
        PHI,

        /**
         * Personally Identifiable Information
         */
        PII,

        /**
         * Payment Card Industry data
         */
        PCI,

        /**
         * Other sensitive data
         */
        SENSITIVE
    }
}

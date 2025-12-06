package com.healthdata.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Attribute Converter for Transparent Field Encryption
 *
 * Apply to entity fields containing PII/PHI that require encryption at rest.
 *
 * Usage in entity:
 * <pre>
 * {@code
 * @Entity
 * public class Patient {
 *     @Convert(converter = EncryptedStringConverter.class)
 *     @Column(name = "ssn")
 *     private String socialSecurityNumber;
 *
 *     @Convert(converter = EncryptedStringConverter.class)
 *     @Column(name = "medical_record_number")
 *     private String medicalRecordNumber;
 * }
 * }
 * </pre>
 *
 * The converter automatically encrypts on write and decrypts on read.
 * Encrypted values are stored with "ENC:" prefix for identification.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static FieldEncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(FieldEncryptionService service) {
        EncryptedStringConverter.encryptionService = service;
    }

    /**
     * Encrypt value before storing in database
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized");
        }

        return encryptionService.encrypt(attribute);
    }

    /**
     * Decrypt value when reading from database
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized");
        }

        return encryptionService.decrypt(dbData);
    }
}

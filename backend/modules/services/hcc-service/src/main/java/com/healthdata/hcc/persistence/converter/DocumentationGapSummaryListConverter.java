package com.healthdata.hcc.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.hcc.persistence.PatientHccProfileEntity.DocumentationGapSummary;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA AttributeConverter for List<DocumentationGapSummary> to JSON string.
 * Works with both H2 (for testing) and PostgreSQL (for production).
 */
@Converter
public class DocumentationGapSummaryListConverter
        implements AttributeConverter<List<DocumentationGapSummary>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<List<DocumentationGapSummary>> TYPE_REF =
            new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<DocumentationGapSummary> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting list to JSON", e);
        }
    }

    @Override
    public List<DocumentationGapSummary> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(dbData, TYPE_REF);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting JSON to list", e);
        }
    }
}

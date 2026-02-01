package com.healthdata.caregap.config;

import com.healthdata.caregap.client.CqlEngineServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Test profile configuration for CQL Engine client stubs.
 */
@Configuration
@Profile("test")
public class TestCqlEngineClientConfig {

    @Bean
    @Primary
    public CqlEngineServiceClient cqlEngineServiceClient() {
        return new CqlEngineServiceClient() {
            @Override
            public String evaluateCql(String tenantId, String libraryName, UUID patientId, String parameters) {
                String measureName = libraryName.replace('_', ' ');
                return """
                    {
                      "hasGap": true,
                      "measureId": "%s",
                      "measureName": "%s",
                      "priority": "HIGH",
                      "severity": "HIGH",
                      "riskScore": 0.75,
                      "gapDescription": "Test gap identified",
                      "gapReason": "Test reason",
                      "recommendation": "Schedule follow-up",
                      "recommendationType": "screening",
                      "recommendedAction": "Order screening",
                      "dueDate": "%s"
                    }
                    """.formatted(libraryName, measureName, LocalDate.now().plusDays(30));
            }

            @Override
            public String evaluateCqlBatch(String tenantId, UUID patientId, String request) {
                return "{\"results\":[]}";
            }

            @Override
            public String getAvailableLibraries() {
                return "[{\"name\":\"HEDIS_AWV\"},{\"name\":\"HEDIS_CDC_A1C\"},{\"name\":\"HEDIS_BCS\"}]";
            }

            @Override
            public String getLibraryDetails(String libraryName) {
                return "{\"name\":\"" + libraryName + "\",\"expressions\":[]}";
            }

            @Override
            public String healthCheck() {
                return "{\"status\":\"UP\"}";
            }
        };
    }
}

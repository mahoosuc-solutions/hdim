package com.healthdata.ehr.factory;

import com.healthdata.ehr.connector.EhrConnector;
import com.healthdata.ehr.connector.impl.AthenaConnector;
import com.healthdata.ehr.connector.impl.CernerFhirConnector;
import com.healthdata.ehr.connector.impl.EpicFhirConnector;
import com.healthdata.ehr.connector.impl.GenericFhirConnector;
import com.healthdata.ehr.dto.EhrConnectionConfig;
import com.healthdata.ehr.model.EhrVendorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Factory for creating EHR connector instances based on vendor type.
 * Uses the Factory pattern to encapsulate connector creation logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EhrConnectorFactory {

    private final WebClient.Builder webClientBuilder;

    /**
     * Create an EHR connector instance based on the configuration.
     *
     * @param config Connection configuration
     * @return EhrConnector instance for the specified vendor
     * @throws IllegalArgumentException if configuration is invalid
     */
    public EhrConnector createConnector(EhrConnectionConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Connection configuration cannot be null");
        }

        if (config.getVendorType() == null) {
            throw new IllegalArgumentException("Vendor type cannot be null");
        }

        log.info("Creating connector for vendor: {} and tenant: {}",
                config.getVendorType(), config.getTenantId());

        return switch (config.getVendorType()) {
            case EPIC -> createEpicConnector(config);
            case CERNER -> createCernerConnector(config);
            case ATHENA -> createAthenaConnector(config);
            case GENERIC -> createGenericConnector(config);
        };
    }

    private EhrConnector createEpicConnector(EhrConnectionConfig config) {
        log.debug("Creating Epic FHIR connector");
        return new EpicFhirConnector(config, webClientBuilder);
    }

    private EhrConnector createCernerConnector(EhrConnectionConfig config) {
        log.debug("Creating Cerner FHIR connector");
        return new CernerFhirConnector(config, webClientBuilder);
    }

    private EhrConnector createAthenaConnector(EhrConnectionConfig config) {
        log.debug("Creating Athena connector");
        return new AthenaConnector(config, webClientBuilder);
    }

    private EhrConnector createGenericConnector(EhrConnectionConfig config) {
        log.debug("Creating Generic FHIR connector");
        return new GenericFhirConnector(config, webClientBuilder);
    }
}

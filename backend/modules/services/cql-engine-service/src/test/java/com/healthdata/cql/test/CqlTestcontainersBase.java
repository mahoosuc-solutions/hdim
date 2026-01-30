package com.healthdata.cql.test;

import com.healthdata.cql.config.CqlTestContainersConfiguration;
import com.healthdata.cql.entity.CqlLibrary;
import com.healthdata.cql.entity.ValueSet;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for CQL Engine Spring Boot tests that require Kafka Testcontainers.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class CqlTestcontainersBase {

    @DynamicPropertySource
    static void configureKafkaProperties(DynamicPropertyRegistry registry) {
        CqlTestContainersConfiguration.configureKafka(registry);
    }

    protected static CqlLibrary buildLibrary(String tenantId, String name, String version) {
        CqlLibrary library = new CqlLibrary(tenantId, name, version);
        library.setCqlContent("library " + name + " version '" + version + "'");
        return library;
    }

    protected static ValueSet buildValueSet(String tenantId, String oid, String name, String codeSystem) {
        ValueSet valueSet = new ValueSet(tenantId, oid, name, codeSystem);
        valueSet.setCodes("[\"CODE1\"]");
        return valueSet;
    }
}

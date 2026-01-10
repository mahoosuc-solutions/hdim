package com.healthdata.fhir.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.JsonFactoryBuilder;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonStreamConstraintsConfig {

    private static final int MAX_NESTING_DEPTH = 2000;

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonStreamConstraintsCustomizer() {
        return builder -> builder.factory(new JsonFactoryBuilder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(MAX_NESTING_DEPTH)
                        .build())
                .streamWriteConstraints(StreamWriteConstraints.builder()
                        .maxNestingDepth(MAX_NESTING_DEPTH)
                        .build())
                .build());
    }
}

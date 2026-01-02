package com.healthdata.cdr.config;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.validation.impl.NoValidation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for HAPI HL7 v2 Parser.
 *
 * Configures:
 * - HapiContext for HL7 v2 message processing
 * - Parser for HL7 v2 message parsing
 * - Validation rules (disabled by default for flexibility)
 */
@Slf4j
@Configuration
public class Hl7v2ParserConfig {

    /**
     * Create and configure the HAPI HL7 v2 context.
     *
     * @return Configured HapiContext
     */
    @Bean
    public HapiContext hapiContext() {
        log.info("Initializing HAPI HL7 v2 Context");

        DefaultHapiContext context = new DefaultHapiContext();

        // Use canonical model classes (HL7 v2.5)
        context.setModelClassFactory(new CanonicalModelClassFactory("2.5"));

        // Disable validation for flexibility (can be enabled in production)
        context.setValidationContext(new NoValidation());

        log.info("HAPI HL7 v2 Context initialized successfully");
        return context;
    }

    /**
     * Create HL7 v2 parser bean.
     *
     * @param hapiContext The HAPI context
     * @return Configured Parser
     */
    @Bean
    public Parser hl7v2Parser(HapiContext hapiContext) {
        log.info("Creating HL7 v2 Parser");
        return hapiContext.getPipeParser();
    }
}

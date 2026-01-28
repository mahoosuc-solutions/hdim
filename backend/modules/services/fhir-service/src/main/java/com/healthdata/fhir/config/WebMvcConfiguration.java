package com.healthdata.fhir.config;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Web MVC Configuration for FHIR Service.
 * <p>
 * Disables XML message converter auto-configuration to prevent Jackson XML
 * dependency conflicts. All HDIM services use JSON exclusively (application/json
 * and application/fhir+json).
 * </p>
 *
 * <h2>Why This Configuration is Needed</h2>
 * <p>
 * Spring Boot auto-configures both JSON and XML message converters when
 * jackson-dataformat-xml is on the classpath. This causes ClassCastException
 * when Twilio SDK pulls incompatible Jackson XML dependencies.
 * </p>
 *
 * <h2>Solution</h2>
 * <ul>
 *   <li>Explicitly define HttpMessageConverters bean with only JSON converter</li>
 *   <li>Prevent Spring Boot XML auto-configuration</li>
 *   <li>All FHIR R4 responses use application/fhir+json (JSON only)</li>
 * </ul>
 *
 * @see com.healthdata.authentication.config.JacksonConfiguration
 */
@Configuration
public class WebMvcConfiguration {

    /**
     * Configure HTTP message converters to use JSON only (no XML).
     * <p>
     * This prevents Spring Boot from auto-configuring the XML message converter
     * which causes ClassCastException: JsonFactory cannot be cast to XmlFactory.
     * </p>
     *
     * @param jsonConverter the JSON message converter (auto-configured by Spring Boot)
     * @return HttpMessageConverters with only JSON support
     */
    @Bean
    public HttpMessageConverters messageConverters(MappingJackson2HttpMessageConverter jsonConverter) {
        List<HttpMessageConverter<?>> converters = new ArrayList<>();
        // Handle application/fhir+json payloads as raw strings for HAPI parsing in controllers.
        StringHttpMessageConverter fhirStringConverter = new StringHttpMessageConverter();
        fhirStringConverter.setSupportedMediaTypes(List.of(MediaType.valueOf("application/fhir+json")));
        converters.add(fhirStringConverter);
        converters.add(jsonConverter);
        return new HttpMessageConverters(false, converters);
    }
}

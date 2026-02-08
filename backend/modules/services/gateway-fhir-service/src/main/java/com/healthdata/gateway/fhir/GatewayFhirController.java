package com.healthdata.gateway.fhir;

import com.healthdata.gateway.service.GatewayForwarder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway routing for FHIR-related services.
 */
@RestController
@RequiredArgsConstructor
public class GatewayFhirController {

    private final GatewayForwarder forwarder;

    @Value("${backend.services.cql-engine.url}")
    private String cqlEngineUrl;

    @Value("${backend.services.quality-measure.url}")
    private String qualityMeasureUrl;

    @Value("${backend.services.fhir.url}")
    private String fhirUrl;

    @Value("${backend.services.patient.url}")
    private String patientUrl;

    @RequestMapping(value = "/api/cql/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCqlEngine(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, cqlEngineUrl, "/api/cql");
    }

    @RequestMapping(value = "/cql-engine/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToCqlEngineDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, cqlEngineUrl, "/cql-engine");
    }

    @RequestMapping(value = "/api/quality/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQualityMeasure(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, qualityMeasureUrl, "/api/quality");
    }

    @RequestMapping(value = "/api/v1/quality-measures/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQualityMeasureV1(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, qualityMeasureUrl, "/api/v1/quality-measures");
    }

    @RequestMapping(value = "/quality-measure/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToQualityMeasureDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, qualityMeasureUrl, "/quality-measure");
    }

    @RequestMapping(value = "/api/fhir/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToFhir(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, fhirUrl, "/api/fhir");
    }

    @RequestMapping(value = "/fhir/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToFhirDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, fhirUrl, "/fhir");
    }

    @RequestMapping(value = "/api/patients/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToPatient(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, patientUrl, "/api/patients");
    }

    @RequestMapping(value = "/patient/**", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<?> routeToPatientDirect(
        HttpServletRequest request,
        @RequestBody(required = false) String body
    ) {
        return forwarder.forwardRequest(request, body, patientUrl, "/patient");
    }
}

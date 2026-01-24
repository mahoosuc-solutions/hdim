package com.healthdata.patient.controller;

import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Patient API Controller
 *
 * Provides paged patient list data for the clinical portal.
 */
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Slf4j
public class PatientApiController {

    private final PatientDemographicsRepository patientDemographicsRepository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('PATIENT_READ')")
    public ResponseEntity<Page<PatientDemographicsEntity>> listPatients(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/v1/patients - tenant: {}, page: {}, size: {}", tenantId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<PatientDemographicsEntity> patients =
                patientDemographicsRepository.findByTenantIdAndActiveTrue(tenantId, pageable);

        return ResponseEntity.ok(patients);
    }
}

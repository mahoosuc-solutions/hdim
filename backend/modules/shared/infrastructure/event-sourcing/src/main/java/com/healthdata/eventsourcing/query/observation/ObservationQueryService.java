package com.healthdata.eventsourcing.query.observation;

import com.healthdata.eventsourcing.projection.observation.ObservationProjection;
import com.healthdata.eventsourcing.projection.observation.ObservationProjectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObservationQueryService {

    private final ObservationProjectionRepository observationRepository;

    public List<ObservationProjection> findByPatientAndTenant(String patientId, String tenantId) {
        log.debug("Finding observations for patient: {} in tenant: {}", patientId, tenantId);
        return observationRepository.findByPatientIdAndTenantId(patientId, tenantId);
    }

    public List<ObservationProjection> findByLoincCodeAndTenant(String loincCode, String tenantId) {
        log.debug("Finding observations by LOINC: {} in tenant: {}", loincCode, tenantId);
        return observationRepository.findByLoincCodeAndTenantId(loincCode, tenantId);
    }

    public Optional<ObservationProjection> findLatestByLoincAndPatient(String patientId, String loincCode, String tenantId) {
        log.debug("Finding latest observation for patient: {} LOINC: {} in tenant: {}", patientId, loincCode, tenantId);
        return observationRepository.findByPatientIdAndLoincCodeAndTenantId(patientId, loincCode, tenantId);
    }

    public List<ObservationProjection> findByDateRange(String tenantId, LocalDate startDate, LocalDate endDate) {
        log.debug("Finding observations between {} and {} in tenant: {}", startDate, endDate, tenantId);
        return observationRepository.findByTenantIdAndObservationDateBetween(tenantId, startDate, endDate);
    }
}

package com.healthdata.cqrsquery;

import java.time.LocalDate;
import java.util.*;

/**
 * PatientQueryService - Read-optimized patient queries backed by projections
 */
public class PatientQueryService {
    private final MockProjectionStore projectionStore;
    private final MockCacheStore cacheStore;

    public PatientQueryService(MockProjectionStore projectionStore, MockCacheStore cacheStore) {
        this.projectionStore = projectionStore;
        this.cacheStore = cacheStore;
    }

    public List<PatientQueryResult> searchByFirstName(String tenantId, String firstName) {
        if (firstName == null) return new ArrayList<>();
        List<PatientQueryResult> results = projectionStore.getPatients(tenantId).stream()
            .filter(p -> p.getFirstName().equalsIgnoreCase(firstName))
            .map(this::mapToResult)
            .toList();
        cacheStore.put("search:firstName:" + firstName + ":" + tenantId, results, 300);
        return results;
    }

    public List<PatientQueryResult> searchByLastName(String tenantId, String lastName) {
        return projectionStore.getPatients(tenantId).stream()
            .filter(p -> p.getLastName().equalsIgnoreCase(lastName))
            .map(this::mapToResult)
            .toList();
    }

    public List<PatientQueryResult> searchByDateOfBirth(String tenantId, LocalDate dob) {
        return projectionStore.getPatients(tenantId).stream()
            .filter(p -> dob.equals(p.getDateOfBirth()))
            .map(this::mapToResult)
            .toList();
    }

    public List<PatientQueryResult> fullTextSearch(String tenantId, String query) {
        return projectionStore.getPatients(tenantId).stream()
            .filter(p -> p.getFirstName().toLowerCase().contains(query.toLowerCase()) ||
                        p.getLastName().toLowerCase().contains(query.toLowerCase()))
            .map(this::mapToResult)
            .toList();
    }

    public List<PatientQueryResult> filterByCondition(String tenantId, String condition) {
        return projectionStore.getPatients(tenantId).stream()
            .filter(p -> p.getConditions().contains(condition))
            .map(this::mapToResult)
            .toList();
    }

    public List<PatientQueryResult> filterByMedication(String tenantId, String medication) {
        return projectionStore.getPatients(tenantId).stream()
            .filter(p -> p.getMedications().contains(medication))
            .map(this::mapToResult)
            .toList();
    }

    public List<PatientQueryResult> filterByEnrollmentStatus(String tenantId, String status) {
        return projectionStore.getPatients(tenantId).stream()
            .filter(p -> status.equals(p.getEnrollmentStatus()))
            .map(this::mapToResult)
            .toList();
    }

    public List<PatientQueryResult> getActivePatientsByConditionAndMedication(String tenantId, String condition, String medication) {
        return projectionStore.getPatients(tenantId).stream()
            .filter(p -> p.getConditions().contains(condition) && p.getMedications().contains(medication) && "ACTIVE".equals(p.getEnrollmentStatus()))
            .map(this::mapToResult)
            .toList();
    }

    public PaginatedResult<PatientQueryResult> searchWithPagination(String tenantId, int page, int size) {
        List<PatientQueryResult> all = projectionStore.getPatients(tenantId).stream()
            .map(this::mapToResult)
            .toList();
        int total = all.size();
        int start = page * size;
        int end = Math.min(start + size, total);
        List<PatientQueryResult> content = start < total ? all.subList(start, end) : new ArrayList<>();
        return new PaginatedResult<>(content, page, size, total);
    }

    public List<PatientQueryResult> searchSortedByLastName(String tenantId, boolean ascending) {
        return projectionStore.getPatients(tenantId).stream()
            .map(this::mapToResult)
            .sorted((a, b) -> ascending ? a.getLastName().compareTo(b.getLastName()) : b.getLastName().compareTo(a.getLastName()))
            .toList();
    }

    public List<PatientQueryResult> searchSortedByAge(String tenantId, boolean ascending) {
        return projectionStore.getPatients(tenantId).stream()
            .map(this::mapToResult)
            .sorted((a, b) -> ascending ?
                a.getDateOfBirth().compareTo(b.getDateOfBirth()) :
                b.getDateOfBirth().compareTo(a.getDateOfBirth()))
            .toList();
    }

    private PatientQueryResult mapToResult(PatientQueryServiceTest.PatientProjection p) {
        return new PatientQueryResult(p.getPatientId(), p.getFirstName(), p.getLastName(), p.getDateOfBirth(),
                                     p.getConditions(), p.getMedications(), p.getEnrollmentStatus());
    }
}

class PatientQueryResult {
    private final String patientId;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final Set<String> conditions;
    private final Set<String> medications;
    private final String enrollmentStatus;

    PatientQueryResult(String patientId, String firstName, String lastName, LocalDate dob, Set<String> conditions, Set<String> medications, String status) {
        this.patientId = patientId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dob;
        this.conditions = conditions;
        this.medications = medications;
        this.enrollmentStatus = status;
    }

    String getPatientId() { return patientId; }
    String getFirstName() { return firstName; }
    String getLastName() { return lastName; }
    LocalDate getDateOfBirth() { return dateOfBirth; }
    boolean hasCondition(String condition) { return conditions.contains(condition); }
    boolean hasMedication(String medication) { return medications.contains(medication); }
    String getEnrollmentStatus() { return enrollmentStatus; }
    Set<String> getConditions() { return conditions; }
    Set<String> getMedications() { return medications; }
}

class PaginatedResult<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final int totalElements;

    PaginatedResult(List<T> content, int page, int size, int totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
    }

    List<T> getContent() { return content; }
    int getTotalElements() { return totalElements; }
    int getTotalPages() { return (totalElements + size - 1) / size; }
}

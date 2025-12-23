package com.healthdata.quality.service;

import com.healthdata.quality.dto.FhirResourceEvent;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.CareGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;

/**
 * Care Gap Matching Service
 * Matches FHIR resources to open care gaps based on codes and categories
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CareGapMatchingService {

    private final CareGapRepository careGapRepository;

    /**
     * Find care gaps that match a FHIR resource event
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param event FHIR resource event
     * @return List of matching care gaps
     */
    public List<CareGapEntity> findMatchingCareGaps(
        String tenantId,
        UUID patientId,
        FhirResourceEvent event
    ) {
        log.debug("Finding care gaps matching {} for patient {} in tenant {}",
            event.getResourceType(), patientId, tenantId);

        // Extract codes from the event
        Set<String> eventCodes = extractCodes(event);

        if (eventCodes.isEmpty()) {
            log.debug("No codes found in event, cannot match care gaps");
            return List.of();
        }

        // Get all open care gaps for this patient
        List<CareGapEntity> openGaps = careGapRepository.findOpenCareGaps(tenantId, patientId);

        log.debug("Found {} open care gaps for patient", openGaps.size());

        // Filter to gaps that match any of the event codes
        List<CareGapEntity> matchingGaps = openGaps.stream()
            .filter(gap -> matchesCodes(gap, eventCodes))
            .collect(Collectors.toList());

        log.info("Found {} care gaps matching {} codes from {}",
            matchingGaps.size(), eventCodes.size(), event.getResourceType());

        return matchingGaps;
    }

    /**
     * Extract all codes from a FHIR resource event
     */
    private Set<String> extractCodes(FhirResourceEvent event) {
        if (event.getCodes() == null) {
            return Set.of();
        }

        return event.getCodes().stream()
            .filter(cc -> cc.getCoding() != null)
            .flatMap(cc -> cc.getCoding().stream())
            .filter(coding -> coding.getCode() != null)
            .map(FhirResourceEvent.Coding::getCode)
            .collect(Collectors.toSet());
    }

    /**
     * Check if a care gap matches any of the provided codes
     */
    private boolean matchesCodes(CareGapEntity gap, Set<String> eventCodes) {
        String matchingCodes = gap.getMatchingCodes();

        if (matchingCodes == null || matchingCodes.isBlank()) {
            log.debug("Care gap {} has no matching codes defined", gap.getId());
            return false;
        }

        // Parse comma-separated codes from the gap
        Set<String> gapCodes = Arrays.stream(matchingCodes.split(","))
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .collect(Collectors.toSet());

        // Check for any intersection
        boolean matches = gapCodes.stream()
            .anyMatch(eventCodes::contains);

        if (matches) {
            log.debug("Care gap {} matches codes: gap={}, event={}",
                gap.getId(), gapCodes, eventCodes);
        }

        return matches;
    }

    /**
     * Get a summary of matching codes
     */
    public String getMatchingSummary(CareGapEntity gap, FhirResourceEvent event) {
        Set<String> eventCodes = extractCodes(event);
        String matchingCodes = gap.getMatchingCodes();

        if (matchingCodes == null || matchingCodes.isBlank()) {
            return "No specific codes";
        }

        Set<String> gapCodes = Arrays.stream(matchingCodes.split(","))
            .map(String::trim)
            .filter(code -> !code.isEmpty())
            .collect(Collectors.toSet());

        Set<String> intersection = gapCodes.stream()
            .filter(eventCodes::contains)
            .collect(Collectors.toSet());

        return String.join(", ", intersection);
    }
}

package com.healthdata.priorauth.service;

import com.healthdata.priorauth.dto.PriorAuthRequestDTO;
import com.healthdata.priorauth.persistence.PriorAuthRequestEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for building FHIR Claim resources for Prior Authorization Support (PAS).
 *
 * Implements Da Vinci PAS Implementation Guide for FHIR-based prior authorization.
 * Creates Claim resources with X12 278 equivalent data mapped to FHIR.
 *
 * @see <a href="https://hl7.org/fhir/us/davinci-pas/">Da Vinci PAS IG</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PasClaimBuilder {

    private static final String PAS_PROFILE = "http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim";
    private static final String CLAIM_SYSTEM = "http://terminology.hl7.org/CodeSystem/claim-type";
    private static final String PRIORITY_SYSTEM = "http://terminology.hl7.org/CodeSystem/processpriority";
    private static final String ICD10_SYSTEM = "http://hl7.org/fhir/sid/icd-10-cm";
    private static final String CPT_SYSTEM = "http://www.ama-assn.org/go/cpt";
    private static final String HCPCS_SYSTEM = "https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets";

    /**
     * Build a FHIR Claim resource for prior authorization.
     *
     * @param request The PA request DTO
     * @param entity The PA request entity
     * @param patientReference Reference to the patient resource
     * @return FHIR Claim resource
     */
    public Claim buildClaim(PriorAuthRequestDTO request, PriorAuthRequestEntity entity,
                            String patientReference) {
        log.debug("Building PAS Claim for PA request: {}", entity.getId());

        Claim claim = new Claim();

        // Set profile
        claim.getMeta().addProfile(PAS_PROFILE);

        // Identifier
        claim.addIdentifier()
            .setSystem("urn:healthdata:prior-auth")
            .setValue(entity.getPaRequestId());

        // Status
        claim.setStatus(Claim.ClaimStatus.ACTIVE);

        // Type - institutional or professional
        claim.setType(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(CLAIM_SYSTEM)
                .setCode("institutional")
                .setDisplay("Institutional")));

        // Use - preauthorization
        claim.setUse(Claim.Use.PREAUTHORIZATION);

        // Patient
        claim.setPatient(new Reference(patientReference));

        // Created date
        claim.setCreated(Date.from(LocalDateTime.now()
            .atZone(ZoneId.systemDefault()).toInstant()));

        // Insurer (Payer)
        claim.setInsurer(new Reference()
            .setIdentifier(new Identifier()
                .setSystem("urn:healthdata:payer")
                .setValue(request.getPayerId())));

        // Provider
        if (request.getProviderId() != null) {
            claim.setProvider(new Reference()
                .setIdentifier(new Identifier()
                    .setSystem("http://hl7.org/fhir/sid/us-npi")
                    .setValue(request.getProviderNpi())));
        }

        // Priority
        claim.setPriority(new CodeableConcept()
            .addCoding(new Coding()
                .setSystem(PRIORITY_SYSTEM)
                .setCode(mapUrgencyToPriority(request.getUrgency()))
                .setDisplay(request.getUrgency().name())));

        // Facility
        if (request.getFacilityId() != null) {
            claim.setFacility(new Reference()
                .setIdentifier(new Identifier()
                    .setSystem("urn:healthdata:facility")
                    .setValue(request.getFacilityId())));
        }

        // Diagnosis
        if (request.getDiagnosisCodes() != null) {
            int sequence = 1;
            for (String diagCode : request.getDiagnosisCodes()) {
                claim.addDiagnosis()
                    .setSequence(sequence++)
                    .setDiagnosis(new CodeableConcept()
                        .addCoding(new Coding()
                            .setSystem(ICD10_SYSTEM)
                            .setCode(diagCode)));
            }
        }

        // Insurance
        claim.addInsurance()
            .setSequence(1)
            .setFocal(true)
            .setCoverage(new Reference()
                .setIdentifier(new Identifier()
                    .setSystem("urn:healthdata:coverage")
                    .setValue(request.getPayerId())));

        // Items (services/procedures requested)
        if (request.getProcedureCodes() != null) {
            int sequence = 1;
            for (String procCode : request.getProcedureCodes()) {
                Claim.ItemComponent item = claim.addItem()
                    .setSequence(sequence++);

                // Determine code system based on code format
                String codeSystem = procCode.matches("^\\d{5}$") ? CPT_SYSTEM : HCPCS_SYSTEM;

                item.setProductOrService(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem(codeSystem)
                        .setCode(procCode)));

                // Quantity
                if (request.getQuantityRequested() != null) {
                    item.setQuantity(new Quantity()
                        .setValue(request.getQuantityRequested()));
                }
            }
        } else if (request.getServiceCode() != null) {
            // Single service code
            String codeSystem = request.getServiceCode().matches("^\\d{5}$") ? CPT_SYSTEM : HCPCS_SYSTEM;

            claim.addItem()
                .setSequence(1)
                .setProductOrService(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem(codeSystem)
                        .setCode(request.getServiceCode())
                        .setDisplay(request.getServiceDescription())))
                .setQuantity(new Quantity()
                    .setValue(request.getQuantityRequested() != null ? request.getQuantityRequested() : 1));
        }

        // Supporting information
        if (request.getSupportingInfo() != null) {
            addSupportingInfo(claim, request.getSupportingInfo());
        }

        log.debug("Built PAS Claim with {} items and {} diagnoses",
            claim.getItem().size(), claim.getDiagnosis().size());

        return claim;
    }

    /**
     * Build a FHIR Bundle containing the Claim and related resources.
     *
     * @param claim The Claim resource
     * @param additionalResources Additional resources to include
     * @return FHIR Bundle
     */
    public Bundle buildClaimBundle(Claim claim, List<Resource> additionalResources) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTimestamp(Date.from(LocalDateTime.now()
            .atZone(ZoneId.systemDefault()).toInstant()));

        // Add Claim as first entry
        bundle.addEntry()
            .setFullUrl("urn:uuid:" + UUID.randomUUID())
            .setResource(claim);

        // Add additional resources (Patient, Coverage, etc.)
        if (additionalResources != null) {
            for (Resource resource : additionalResources) {
                bundle.addEntry()
                    .setFullUrl("urn:uuid:" + UUID.randomUUID())
                    .setResource(resource);
            }
        }

        return bundle;
    }

    /**
     * Parse ClaimResponse from payer.
     *
     * @param responseJson The ClaimResponse JSON from payer
     * @return Parsed decision information
     */
    public PaDecision parseClaimResponse(Map<String, Object> responseJson) {
        log.debug("Parsing ClaimResponse from payer");

        PaDecision decision = new PaDecision();

        if (responseJson == null) {
            decision.setStatus(PriorAuthRequestEntity.Status.ERROR);
            decision.setReason("No response received from payer");
            return decision;
        }

        String outcome = (String) responseJson.get("outcome");
        if (outcome != null) {
            switch (outcome.toLowerCase()) {
                case "complete" -> decision.setStatus(PriorAuthRequestEntity.Status.APPROVED);
                case "partial" -> decision.setStatus(PriorAuthRequestEntity.Status.PARTIALLY_APPROVED);
                case "error" -> decision.setStatus(PriorAuthRequestEntity.Status.ERROR);
                case "queued" -> decision.setStatus(PriorAuthRequestEntity.Status.PENDING_REVIEW);
                default -> decision.setStatus(PriorAuthRequestEntity.Status.DENIED);
            }
        }

        // Extract authorization number
        if (responseJson.containsKey("preAuthRef")) {
            decision.setAuthNumber((String) responseJson.get("preAuthRef"));
        }

        // Extract disposition/reason
        if (responseJson.containsKey("disposition")) {
            decision.setReason((String) responseJson.get("disposition"));
        }

        // Extract approved quantity from items
        if (responseJson.containsKey("item")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) responseJson.get("item");
            if (!items.isEmpty()) {
                Map<String, Object> firstItem = items.get(0);
                if (firstItem.containsKey("adjudication")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> adjudications =
                        (List<Map<String, Object>>) firstItem.get("adjudication");
                    for (Map<String, Object> adj : adjudications) {
                        if ("benefit".equals(adj.get("category"))) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> value = (Map<String, Object>) adj.get("value");
                            if (value != null && value.containsKey("value")) {
                                decision.setApprovedQuantity(((Number) value.get("value")).intValue());
                            }
                        }
                    }
                }
            }
        }

        return decision;
    }

    private String mapUrgencyToPriority(PriorAuthRequestEntity.Urgency urgency) {
        return switch (urgency) {
            case STAT -> "stat";
            case ROUTINE -> "normal";
        };
    }

    private void addSupportingInfo(Claim claim, Map<String, Object> supportingInfo) {
        int sequence = 1;

        for (Map.Entry<String, Object> entry : supportingInfo.entrySet()) {
            Claim.SupportingInformationComponent info = claim.addSupportingInfo()
                .setSequence(sequence++)
                .setCategory(new CodeableConcept()
                    .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/claiminformationcategory")
                        .setCode(entry.getKey())));

            // Add value based on type
            if (entry.getValue() instanceof String) {
                info.setValue(new StringType((String) entry.getValue()));
            } else if (entry.getValue() instanceof Boolean) {
                info.setValue(new BooleanType((Boolean) entry.getValue()));
            }
        }
    }

    /**
     * Decision result from ClaimResponse parsing.
     */
    @lombok.Data
    public static class PaDecision {
        private PriorAuthRequestEntity.Status status;
        private String authNumber;
        private String reason;
        private Integer approvedQuantity;
        private LocalDateTime effectiveDate;
        private LocalDateTime expirationDate;
    }
}

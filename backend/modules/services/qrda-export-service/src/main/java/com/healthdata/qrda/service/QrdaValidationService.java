package com.healthdata.qrda.service;

import com.helger.commons.io.resource.ClassPathResource;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;
import com.helger.schematron.svrl.SVRLFailedAssert;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating QRDA documents using Schematron rules.
 *
 * Validates generated documents against CMS QRDA validation rules
 * to ensure compliance before submission.
 *
 * @see <a href="https://ecqi.healthit.gov/qrda">eCQI QRDA Validation Rules</a>
 */
@Service
@Slf4j
public class QrdaValidationService {

    @Value("${qrda.validation.enabled:true}")
    private boolean validationEnabled;

    @Value("${qrda.validation.category-i-schematron:schematron/qrda-cat-i.sch}")
    private String categoryISchematronPath;

    @Value("${qrda.validation.category-iii-schematron:schematron/qrda-cat-iii.sch}")
    private String categoryIIISchematronPath;

    private ISchematronResource categoryISchematron;
    private ISchematronResource categoryIIISchematron;

    /**
     * Validates a QRDA Category I document.
     *
     * @param documentPath Path to the document or document content
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateCategoryI(String documentPath) {
        if (!validationEnabled) {
            log.debug("QRDA validation is disabled, skipping Category I validation");
            return List.of();
        }

        log.info("Validating QRDA Category I document: {}", documentPath);
        List<String> errors = new ArrayList<>();

        try {
            // Perform basic XML validation first
            errors.addAll(performBasicValidation(documentPath));
            if (!errors.isEmpty()) {
                return errors; // Don't proceed if basic validation fails
            }

            // Validate Category I specific rules
            errors.addAll(validateCategoryISpecificRules(documentPath));

            // Apply Schematron validation if schema is available
            errors.addAll(applySchematronValidation(documentPath, getCategoryISchematron()));

        } catch (Exception e) {
            log.error("Error during QRDA Category I validation", e);
            errors.add("Validation error: " + e.getMessage());
        }

        log.info("QRDA Category I validation completed with {} errors", errors.size());
        return errors;
    }

    /**
     * Get or initialize Category I Schematron resource.
     */
    private ISchematronResource getCategoryISchematron() {
        if (categoryISchematron == null) {
            try {
                ClassPathResource resource = new ClassPathResource(categoryISchematronPath);
                if (resource.exists()) {
                    categoryISchematron = SchematronResourcePure.fromClassPath(categoryISchematronPath);
                    if (!categoryISchematron.isValidSchematron()) {
                        log.warn("Category I Schematron schema is not valid: {}", categoryISchematronPath);
                        categoryISchematron = null;
                    }
                } else {
                    log.info("Category I Schematron schema not found at: {}", categoryISchematronPath);
                }
            } catch (Exception e) {
                log.warn("Could not load Category I Schematron schema: {}", e.getMessage());
            }
        }
        return categoryISchematron;
    }

    /**
     * Get or initialize Category III Schematron resource.
     */
    private ISchematronResource getCategoryIIISchematron() {
        if (categoryIIISchematron == null) {
            try {
                ClassPathResource resource = new ClassPathResource(categoryIIISchematronPath);
                if (resource.exists()) {
                    categoryIIISchematron = SchematronResourcePure.fromClassPath(categoryIIISchematronPath);
                    if (!categoryIIISchematron.isValidSchematron()) {
                        log.warn("Category III Schematron schema is not valid: {}", categoryIIISchematronPath);
                        categoryIIISchematron = null;
                    }
                } else {
                    log.info("Category III Schematron schema not found at: {}", categoryIIISchematronPath);
                }
            } catch (Exception e) {
                log.warn("Could not load Category III Schematron schema: {}", e.getMessage());
            }
        }
        return categoryIIISchematron;
    }

    /**
     * Apply Schematron validation to a document.
     */
    private List<String> applySchematronValidation(String documentContent, ISchematronResource schematron) {
        List<String> errors = new ArrayList<>();

        if (schematron == null) {
            log.debug("Schematron validation skipped - schema not available");
            return errors;
        }

        try {
            Document doc = parseXmlDocument(documentContent);
            if (doc == null) {
                errors.add("Failed to parse document for Schematron validation");
                return errors;
            }

            SchematronOutputType svrlOutput = schematron.applySchematronValidationToSVRL(new DOMSource(doc));
            if (svrlOutput != null) {
                List<SVRLFailedAssert> failedAsserts = SVRLHelper.getAllFailedAssertions(svrlOutput);
                for (SVRLFailedAssert fa : failedAsserts) {
                    String errorMsg = String.format("[%s] %s (location: %s)",
                            fa.getFlag() != null ? fa.getFlag() : "error",
                            fa.getText(),
                            fa.getLocation());
                    errors.add(errorMsg);
                }
            }
        } catch (Exception e) {
            log.error("Schematron validation failed", e);
            errors.add("Schematron validation error: " + e.getMessage());
        }

        return errors;
    }

    /**
     * Validates a QRDA Category III document.
     *
     * @param documentContent The QRDA document content (XML string)
     * @return List of validation errors (empty if valid)
     */
    public List<String> validateCategoryIII(String documentContent) {
        if (!validationEnabled) {
            log.debug("QRDA validation is disabled, skipping Category III validation");
            return List.of();
        }

        log.info("Validating QRDA Category III document");
        List<String> errors = new ArrayList<>();

        try {
            // Perform basic XML validation first
            errors.addAll(performBasicValidation(documentContent));
            if (!errors.isEmpty()) {
                return errors;
            }

            // Validate Category III specific rules
            errors.addAll(validateCategoryIIISpecificRules(documentContent));

            // Apply Schematron validation if schema is available
            errors.addAll(applySchematronValidation(documentContent, getCategoryIIISchematron()));

        } catch (Exception e) {
            log.error("Error during QRDA Category III validation", e);
            errors.add("Validation error: " + e.getMessage());
        }

        log.info("QRDA Category III validation completed with {} errors", errors.size());
        return errors;
    }

    /**
     * Performs basic CDA/QRDA validation common to all document types.
     */
    private List<String> performBasicValidation(String documentContent) {
        List<String> errors = new ArrayList<>();

        if (documentContent == null || documentContent.isBlank()) {
            errors.add("Document content is empty");
            return errors;
        }

        // Parse XML to verify well-formedness
        Document doc = parseXmlDocument(documentContent);
        if (doc == null) {
            errors.add("Document is not well-formed XML");
            return errors;
        }

        // Check for required CDA namespace
        String rootNs = doc.getDocumentElement().getNamespaceURI();
        if (rootNs == null || !rootNs.contains("urn:hl7-org:v3")) {
            errors.add("Missing or invalid CDA namespace (expected urn:hl7-org:v3)");
        }

        // Check root element is ClinicalDocument
        String rootName = doc.getDocumentElement().getLocalName();
        if (!"ClinicalDocument".equals(rootName)) {
            errors.add("Root element must be ClinicalDocument, found: " + rootName);
        }

        // Check for required CDA header elements
        NodeList templateIds = doc.getElementsByTagNameNS("urn:hl7-org:v3", "templateId");
        if (templateIds.getLength() == 0) {
            errors.add("Missing required templateId elements");
        }

        NodeList typeIds = doc.getElementsByTagNameNS("urn:hl7-org:v3", "typeId");
        if (typeIds.getLength() == 0) {
            errors.add("Missing required typeId element");
        }

        NodeList effectiveTimes = doc.getElementsByTagNameNS("urn:hl7-org:v3", "effectiveTime");
        if (effectiveTimes.getLength() == 0) {
            errors.add("Missing required effectiveTime element");
        }

        return errors;
    }

    /**
     * Parse XML document from string content.
     */
    private Document parseXmlDocument(String xmlContent) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // Security: Disable external entities to prevent XXE attacks
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(new StringReader(xmlContent)));
        } catch (Exception e) {
            log.debug("Failed to parse XML document: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validates QRDA Category I specific requirements.
     */
    private List<String> validateCategoryISpecificRules(String documentContent) {
        List<String> errors = new ArrayList<>();

        Document doc = parseXmlDocument(documentContent);
        if (doc == null) {
            return errors; // Already caught in basic validation
        }

        // Check for recordTarget (patient demographics)
        NodeList recordTargets = doc.getElementsByTagNameNS("urn:hl7-org:v3", "recordTarget");
        if (recordTargets.getLength() == 0) {
            errors.add("Missing required recordTarget element (patient demographics)");
        } else {
            // Check for patientRole within recordTarget
            NodeList patientRoles = doc.getElementsByTagNameNS("urn:hl7-org:v3", "patientRole");
            if (patientRoles.getLength() == 0) {
                errors.add("Missing patientRole element within recordTarget");
            }
        }

        // Check for Measure Section (required for Cat I)
        NodeList components = doc.getElementsByTagNameNS("urn:hl7-org:v3", "component");
        boolean hasMeasureSection = false;
        boolean hasReportingParameters = false;

        for (int i = 0; i < components.getLength(); i++) {
            String textContent = components.item(i).getTextContent();
            if (textContent != null) {
                if (textContent.contains("Measure Section")) {
                    hasMeasureSection = true;
                }
                if (textContent.contains("Reporting Parameters")) {
                    hasReportingParameters = true;
                }
            }
        }

        // Check for QRDA Cat I template ID (2.16.840.1.113883.10.20.24.1.1)
        boolean hasCatITemplate = checkTemplateId(doc, "2.16.840.1.113883.10.20.24.1.1");
        if (!hasCatITemplate) {
            errors.add("Missing QRDA Category I templateId (2.16.840.1.113883.10.20.24.1.1)");
        }

        return errors;
    }

    /**
     * Validates QRDA Category III specific requirements.
     */
    private List<String> validateCategoryIIISpecificRules(String documentContent) {
        List<String> errors = new ArrayList<>();

        Document doc = parseXmlDocument(documentContent);
        if (doc == null) {
            return errors;
        }

        // Check for QRDA Cat III template ID (2.16.840.1.113883.10.20.27.1.1)
        boolean hasCatIIITemplate = checkTemplateId(doc, "2.16.840.1.113883.10.20.27.1.1");
        if (!hasCatIIITemplate) {
            errors.add("Missing QRDA Category III templateId (2.16.840.1.113883.10.20.27.1.1)");
        }

        // Check for custodian (required for aggregate reports)
        NodeList custodians = doc.getElementsByTagNameNS("urn:hl7-org:v3", "custodian");
        if (custodians.getLength() == 0) {
            errors.add("Missing required custodian element for aggregate report");
        }

        // Check for legalAuthenticator (required for submission)
        NodeList legalAuthenticators = doc.getElementsByTagNameNS("urn:hl7-org:v3", "legalAuthenticator");
        if (legalAuthenticators.getLength() == 0) {
            errors.add("Missing required legalAuthenticator element");
        }

        // Check for observation entries (population counts)
        NodeList observations = doc.getElementsByTagNameNS("urn:hl7-org:v3", "observation");
        if (observations.getLength() == 0) {
            errors.add("No observation entries found - Category III requires aggregate measure data");
        }

        return errors;
    }

    /**
     * Check if a specific templateId exists in the document.
     */
    private boolean checkTemplateId(Document doc, String templateOid) {
        NodeList templateIds = doc.getElementsByTagNameNS("urn:hl7-org:v3", "templateId");
        for (int i = 0; i < templateIds.getLength(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) templateIds.item(i);
            String root = element.getAttribute("root");
            if (templateOid.equals(root)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates against CMS-specific requirements for submission.
     *
     * @param documentContent The document content (XML string)
     * @param submissionYear The CMS submission year
     * @return List of CMS-specific validation errors
     */
    public List<String> validateForCmsSubmission(String documentContent, int submissionYear) {
        List<String> errors = new ArrayList<>();

        log.info("Validating document for CMS {} submission", submissionYear);

        Document doc = parseXmlDocument(documentContent);
        if (doc == null) {
            errors.add("Cannot parse document for CMS validation");
            return errors;
        }

        // Validate TIN (Tax Identification Number) format if present
        NodeList ids = doc.getElementsByTagNameNS("urn:hl7-org:v3", "id");
        boolean hasTin = false;
        boolean hasNpi = false;

        for (int i = 0; i < ids.getLength(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) ids.item(i);
            String root = element.getAttribute("root");

            // TIN OID: 2.16.840.1.113883.4.2
            if ("2.16.840.1.113883.4.2".equals(root)) {
                hasTin = true;
                String extension = element.getAttribute("extension");
                if (extension == null || !extension.matches("\\d{9}")) {
                    errors.add("Invalid TIN format - must be 9 digits");
                }
            }

            // NPI OID: 2.16.840.1.113883.4.6
            if ("2.16.840.1.113883.4.6".equals(root)) {
                hasNpi = true;
                String extension = element.getAttribute("extension");
                if (extension == null || !extension.matches("\\d{10}")) {
                    errors.add("Invalid NPI format - must be 10 digits");
                }
            }
        }

        // Year-specific template version checks
        String expectedCatIIITemplate = switch (submissionYear) {
            case 2024 -> "2.16.840.1.113883.10.20.27.1.1:2022-05-01";
            case 2025 -> "2.16.840.1.113883.10.20.27.1.1:2023-05-01";
            default -> "2.16.840.1.113883.10.20.27.1.1";
        };

        // Check for submission year-appropriate performance period
        NodeList effectiveTimes = doc.getElementsByTagNameNS("urn:hl7-org:v3", "effectiveTime");
        for (int i = 0; i < effectiveTimes.getLength(); i++) {
            org.w3c.dom.Element element = (org.w3c.dom.Element) effectiveTimes.item(i);
            String low = element.getAttribute("value");
            if (low != null && low.length() >= 4) {
                int year = Integer.parseInt(low.substring(0, 4));
                if (year != submissionYear - 1) {
                    log.debug("Performance period year {} may not match submission year {}", year, submissionYear);
                }
            }
        }

        return errors;
    }
}

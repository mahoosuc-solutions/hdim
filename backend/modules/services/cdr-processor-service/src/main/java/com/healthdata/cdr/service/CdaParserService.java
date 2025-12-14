package com.healthdata.cdr.service;

import com.healthdata.cdr.dto.CdaDocument;
import com.healthdata.cdr.handler.cda.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for parsing CDA/C-CDA documents.
 * Orchestrates extraction of clinical data from various CDA sections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdaParserService {

    private final CdaPatientHandler patientHandler;
    private final CdaProblemHandler problemHandler;
    private final CdaMedicationHandler medicationHandler;
    private final CdaAllergyHandler allergyHandler;
    private final CdaImmunizationHandler immunizationHandler;
    private final CdaProcedureHandler procedureHandler;
    private final CdaResultHandler resultHandler;
    private final CdaVitalSignHandler vitalSignHandler;
    private final CdaEncounterHandler encounterHandler;

    // CDA Namespace
    private static final String CDA_NAMESPACE = "urn:hl7-org:v3";
    private static final String SDTC_NAMESPACE = "urn:hl7-org:sdtc";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    // Document Type Template OIDs
    private static final Map<String, String> DOCUMENT_TYPES = Map.of(
        "2.16.840.1.113883.10.20.22.1.1", "US Realm Header",
        "2.16.840.1.113883.10.20.22.1.2", "Continuity of Care Document (CCD)",
        "2.16.840.1.113883.10.20.22.1.8", "Discharge Summary",
        "2.16.840.1.113883.10.20.22.1.9", "Care Plan",
        "2.16.840.1.113883.10.20.22.1.4", "Consultation Note",
        "2.16.840.1.113883.10.20.22.1.5", "Diagnostic Imaging Report",
        "2.16.840.1.113883.10.20.22.1.6", "History and Physical",
        "2.16.840.1.113883.10.20.22.1.7", "Operative Note",
        "2.16.840.1.113883.10.20.22.1.3", "Progress Note",
        "2.16.840.1.113883.10.20.22.1.10", "Unstructured Document"
    );

    /**
     * Parse a CDA document and extract clinical data.
     *
     * @param rawDocument Raw CDA XML content
     * @param tenantId    Tenant ID for multi-tenant support
     * @return Parsed CdaDocument with extracted data
     */
    public CdaDocument parseDocument(String rawDocument, String tenantId) {
        log.debug("Parsing CDA document for tenant: {}", tenantId);

        long startTime = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            // Parse XML
            Document xmlDoc = parseXml(rawDocument);
            XPath xpath = createNamespaceAwareXPath();

            // Extract document metadata
            String documentId = extractDocumentId(xmlDoc, xpath);
            String documentType = determineDocumentType(xmlDoc, xpath);
            String templateId = extractTemplateId(xmlDoc, xpath);
            String title = extractTitle(xmlDoc, xpath);
            LocalDateTime effectiveTime = extractEffectiveTime(xmlDoc, xpath);
            String confidentialityCode = extractConfidentialityCode(xmlDoc, xpath);

            CdaDocument cdaDoc = CdaDocument.builder()
                .tenantId(tenantId)
                .rawDocument(rawDocument)
                .documentId(documentId)
                .documentType(documentType)
                .templateId(templateId)
                .title(title)
                .effectiveTime(effectiveTime)
                .confidentialityCode(confidentialityCode)
                .processedAt(LocalDateTime.now())
                .status("PARSED")
                .build();

            // Extract document header information
            cdaDoc.setCustodian(extractCustodian(xmlDoc, xpath));
            cdaDoc.setAuthors(extractAuthors(xmlDoc, xpath));

            // Extract patient demographics (recordTarget)
            cdaDoc.setPatient(patientHandler.extractPatient(xmlDoc, xpath));

            // Extract clinical sections
            cdaDoc.setProblems(problemHandler.extractProblems(xmlDoc, xpath));
            cdaDoc.setMedications(medicationHandler.extractMedications(xmlDoc, xpath));
            cdaDoc.setAllergies(allergyHandler.extractAllergies(xmlDoc, xpath));
            cdaDoc.setImmunizations(immunizationHandler.extractImmunizations(xmlDoc, xpath));
            cdaDoc.setProcedures(procedureHandler.extractProcedures(xmlDoc, xpath));
            cdaDoc.setResults(resultHandler.extractResults(xmlDoc, xpath));
            cdaDoc.setVitalSigns(vitalSignHandler.extractVitalSigns(xmlDoc, xpath));
            cdaDoc.setEncounters(encounterHandler.extractEncounters(xmlDoc, xpath));

            // Set warnings if any
            cdaDoc.setWarnings(warnings);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Parsed CDA document {} in {}ms - type: {}, patient: {}, problems: {}, meds: {}, allergies: {}",
                documentId, duration, documentType,
                cdaDoc.getPatient() != null ? "present" : "missing",
                cdaDoc.getProblems() != null ? cdaDoc.getProblems().size() : 0,
                cdaDoc.getMedications() != null ? cdaDoc.getMedications().size() : 0,
                cdaDoc.getAllergies() != null ? cdaDoc.getAllergies().size() : 0);

            return cdaDoc;

        } catch (Exception e) {
            log.error("Failed to parse CDA document: {}", e.getMessage(), e);
            return CdaDocument.builder()
                .tenantId(tenantId)
                .rawDocument(rawDocument)
                .status("ERROR")
                .errorMessage("Parse error: " + e.getMessage())
                .processedAt(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Validate a CDA document structure.
     *
     * @param rawDocument Raw CDA XML content
     * @return true if document is valid, false otherwise
     */
    public boolean validateDocument(String rawDocument) {
        if (rawDocument == null || rawDocument.isEmpty()) {
            return false;
        }

        if (!rawDocument.contains("<ClinicalDocument")) {
            return false;
        }

        try {
            Document xmlDoc = parseXml(rawDocument);
            XPath xpath = createNamespaceAwareXPath();

            // Check for required elements
            Element root = xmlDoc.getDocumentElement();
            if (root == null || !root.getLocalName().equals("ClinicalDocument")) {
                return false;
            }

            // Check for recordTarget (patient)
            Element recordTarget = (Element) xpath.evaluate(
                "//hl7:recordTarget", xmlDoc, XPathConstants.NODE);
            if (recordTarget == null) {
                log.warn("CDA document missing recordTarget element");
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("CDA document validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get supported document types.
     *
     * @return Map of template OID to document type name
     */
    public Map<String, String> getSupportedDocumentTypes() {
        return new HashMap<>(DOCUMENT_TYPES);
    }

    // Private helper methods

    private Document parseXml(String rawDocument) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        // Security: Disable external entities
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(rawDocument));
        return builder.parse(inputSource);
    }

    private XPath createNamespaceAwareXPath() {
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        xpath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return switch (prefix) {
                    case "hl7" -> CDA_NAMESPACE;
                    case "sdtc" -> SDTC_NAMESPACE;
                    case "xsi" -> XSI_NAMESPACE;
                    default -> null;
                };
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return null;
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                return null;
            }
        });

        return xpath;
    }

    private String extractDocumentId(Document doc, XPath xpath) throws Exception {
        Element idElement = (Element) xpath.evaluate(
            "//hl7:ClinicalDocument/hl7:id", doc, XPathConstants.NODE);

        if (idElement != null) {
            String root = idElement.getAttribute("root");
            String extension = idElement.getAttribute("extension");
            return extension != null && !extension.isEmpty() ? extension : root;
        }
        return UUID.randomUUID().toString();
    }

    private String determineDocumentType(Document doc, XPath xpath) throws Exception {
        NodeList templateNodes = (NodeList) xpath.evaluate(
            "//hl7:ClinicalDocument/hl7:templateId", doc, XPathConstants.NODESET);

        for (int i = 0; i < templateNodes.getLength(); i++) {
            Element templateElement = (Element) templateNodes.item(i);
            String root = templateElement.getAttribute("root");
            if (DOCUMENT_TYPES.containsKey(root)) {
                return DOCUMENT_TYPES.get(root);
            }
        }

        return "Unknown CDA Document";
    }

    private String extractTemplateId(Document doc, XPath xpath) throws Exception {
        Element templateElement = (Element) xpath.evaluate(
            "//hl7:ClinicalDocument/hl7:templateId[1]", doc, XPathConstants.NODE);

        if (templateElement != null) {
            return templateElement.getAttribute("root");
        }
        return null;
    }

    private String extractTitle(Document doc, XPath xpath) throws Exception {
        return xpath.evaluate("//hl7:ClinicalDocument/hl7:title/text()", doc);
    }

    private LocalDateTime extractEffectiveTime(Document doc, XPath xpath) throws Exception {
        Element effectiveTimeElement = (Element) xpath.evaluate(
            "//hl7:ClinicalDocument/hl7:effectiveTime", doc, XPathConstants.NODE);

        if (effectiveTimeElement != null) {
            String value = effectiveTimeElement.getAttribute("value");
            return parseHL7DateTime(value);
        }
        return null;
    }

    private String extractConfidentialityCode(Document doc, XPath xpath) throws Exception {
        Element confElement = (Element) xpath.evaluate(
            "//hl7:ClinicalDocument/hl7:confidentialityCode", doc, XPathConstants.NODE);

        if (confElement != null) {
            return confElement.getAttribute("code");
        }
        return null;
    }

    private Map<String, Object> extractCustodian(Document doc, XPath xpath) throws Exception {
        Map<String, Object> custodian = new HashMap<>();

        Element custodianElement = (Element) xpath.evaluate(
            "//hl7:ClinicalDocument/hl7:custodian/hl7:assignedCustodian/hl7:representedCustodianOrganization",
            doc, XPathConstants.NODE);

        if (custodianElement != null) {
            Element idElement = (Element) xpath.evaluate("hl7:id", custodianElement, XPathConstants.NODE);
            if (idElement != null) {
                custodian.put("id", idElement.getAttribute("root"));
                custodian.put("extension", idElement.getAttribute("extension"));
            }

            String name = xpath.evaluate("hl7:name/text()", custodianElement);
            if (name != null && !name.isEmpty()) {
                custodian.put("name", name);
            }

            Element telecomElement = (Element) xpath.evaluate("hl7:telecom", custodianElement, XPathConstants.NODE);
            if (telecomElement != null) {
                custodian.put("telecom", telecomElement.getAttribute("value"));
            }

            Element addrElement = (Element) xpath.evaluate("hl7:addr", custodianElement, XPathConstants.NODE);
            if (addrElement != null) {
                Map<String, Object> address = extractAddress(addrElement, xpath);
                custodian.put("address", address);
            }
        }

        return custodian;
    }

    private List<Map<String, Object>> extractAuthors(Document doc, XPath xpath) throws Exception {
        List<Map<String, Object>> authors = new ArrayList<>();

        NodeList authorNodes = (NodeList) xpath.evaluate(
            "//hl7:ClinicalDocument/hl7:author", doc, XPathConstants.NODESET);

        for (int i = 0; i < authorNodes.getLength(); i++) {
            Element authorElement = (Element) authorNodes.item(i);
            Map<String, Object> author = new HashMap<>();

            // Author time
            Element timeElement = (Element) xpath.evaluate("hl7:time", authorElement, XPathConstants.NODE);
            if (timeElement != null) {
                author.put("time", timeElement.getAttribute("value"));
            }

            // Author ID
            Element idElement = (Element) xpath.evaluate(
                "hl7:assignedAuthor/hl7:id", authorElement, XPathConstants.NODE);
            if (idElement != null) {
                author.put("id", idElement.getAttribute("extension"));
                author.put("idRoot", idElement.getAttribute("root"));
            }

            // Author name
            Element nameElement = (Element) xpath.evaluate(
                "hl7:assignedAuthor/hl7:assignedPerson/hl7:name", authorElement, XPathConstants.NODE);
            if (nameElement != null) {
                String family = xpath.evaluate("hl7:family/text()", nameElement);
                String given = xpath.evaluate("hl7:given/text()", nameElement);
                author.put("familyName", family);
                author.put("givenName", given);
            }

            // Author organization
            String orgName = xpath.evaluate(
                "hl7:assignedAuthor/hl7:representedOrganization/hl7:name/text()", authorElement);
            if (orgName != null && !orgName.isEmpty()) {
                author.put("organization", orgName);
            }

            // Authoring device (if software-generated)
            String deviceName = xpath.evaluate(
                "hl7:assignedAuthor/hl7:assignedAuthoringDevice/hl7:softwareName/text()", authorElement);
            if (deviceName != null && !deviceName.isEmpty()) {
                author.put("device", deviceName);
            }

            authors.add(author);
        }

        return authors;
    }

    private Map<String, Object> extractAddress(Element addrElement, XPath xpath) throws Exception {
        Map<String, Object> address = new HashMap<>();

        NodeList streetLines = (NodeList) xpath.evaluate(
            "hl7:streetAddressLine", addrElement, XPathConstants.NODESET);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < streetLines.getLength(); i++) {
            String line = streetLines.item(i).getTextContent();
            if (line != null && !line.isEmpty()) {
                lines.add(line);
            }
        }
        address.put("streetAddressLines", lines);

        String city = xpath.evaluate("hl7:city/text()", addrElement);
        if (city != null && !city.isEmpty()) address.put("city", city);

        String state = xpath.evaluate("hl7:state/text()", addrElement);
        if (state != null && !state.isEmpty()) address.put("state", state);

        String postalCode = xpath.evaluate("hl7:postalCode/text()", addrElement);
        if (postalCode != null && !postalCode.isEmpty()) address.put("postalCode", postalCode);

        String country = xpath.evaluate("hl7:country/text()", addrElement);
        if (country != null && !country.isEmpty()) address.put("country", country);

        return address;
    }

    private LocalDateTime parseHL7DateTime(String hl7DateTime) {
        if (hl7DateTime == null || hl7DateTime.isEmpty()) {
            return null;
        }

        try {
            // HL7 datetime format: YYYYMMDDHHMMSS.UUUU[+/-ZZZZ]
            // Remove timezone and microseconds for parsing
            String normalized = hl7DateTime.replaceAll("[+-]\\d{4}$", "")
                .replaceAll("\\.\\d+", "");

            // Pad to ensure proper length
            while (normalized.length() < 14) {
                normalized += "0";
            }
            normalized = normalized.substring(0, 14);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            return LocalDateTime.parse(normalized, formatter);

        } catch (Exception e) {
            log.debug("Could not parse HL7 datetime: {}", hl7DateTime);
            return null;
        }
    }
}

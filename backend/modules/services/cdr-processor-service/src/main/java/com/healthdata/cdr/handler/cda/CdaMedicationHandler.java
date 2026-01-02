package com.healthdata.cdr.handler.cda;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler for extracting medications from CDA Medications Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.1.1
 */
@Slf4j
@Component
public class CdaMedicationHandler {

    // Medications Section Template OID
    private static final String MEDICATIONS_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.1.1";

    // Medication Activity Template OID
    private static final String MEDICATION_ACTIVITY_TEMPLATE = "2.16.840.1.113883.10.20.22.4.16";

    /**
     * Extract medications from CDA Medications Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of medication maps
     */
    public List<Map<String, Object>> extractMedications(Document doc, XPath xpath) {
        List<Map<String, Object>> medications = new ArrayList<>();

        try {
            // Find medications section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", MEDICATIONS_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Medications section found in document");
                return medications;
            }

            // Find all medication activities (entry/substanceAdministration)
            NodeList entryNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:substanceAdministration[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.16']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element substanceAdminElement = (Element) entryNodes.item(i);
                Map<String, Object> medication = extractMedicationActivity(substanceAdminElement, xpath);
                if (!medication.isEmpty()) {
                    medications.add(medication);
                }
            }

            log.debug("Extracted {} medications from document", medications.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting medications: {}", e.getMessage(), e);
        }

        return medications;
    }

    private Map<String, Object> extractMedicationActivity(Element substanceAdminElement, XPath xpath)
            throws XPathExpressionException {
        Map<String, Object> medication = new HashMap<>();

        // Medication ID
        Element idElement = (Element) xpath.evaluate("hl7:id", substanceAdminElement, XPathConstants.NODE);
        if (idElement != null) {
            medication.put("id", idElement.getAttribute("root"));
            medication.put("extension", idElement.getAttribute("extension"));
        }

        // Status code (active, completed, etc.)
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", substanceAdminElement, XPathConstants.NODE);
        if (statusElement != null) {
            medication.put("status", statusElement.getAttribute("code"));
        }

        // Effective time - can be IVL_TS (period) or PIVL_TS (frequency)
        extractEffectiveTimes(substanceAdminElement, xpath, medication);

        // Route of administration
        Element routeElement = (Element) xpath.evaluate("hl7:routeCode", substanceAdminElement, XPathConstants.NODE);
        if (routeElement != null) {
            Map<String, String> route = new HashMap<>();
            route.put("code", routeElement.getAttribute("code"));
            route.put("codeSystem", routeElement.getAttribute("codeSystem"));
            route.put("displayName", routeElement.getAttribute("displayName"));
            medication.put("route", route);
        }

        // Dose quantity
        Element doseElement = (Element) xpath.evaluate("hl7:doseQuantity", substanceAdminElement, XPathConstants.NODE);
        if (doseElement != null) {
            Map<String, String> dose = new HashMap<>();
            dose.put("value", doseElement.getAttribute("value"));
            dose.put("unit", doseElement.getAttribute("unit"));
            medication.put("doseQuantity", dose);
        }

        // Rate quantity (for IV medications)
        Element rateElement = (Element) xpath.evaluate("hl7:rateQuantity", substanceAdminElement, XPathConstants.NODE);
        if (rateElement != null) {
            Map<String, String> rate = new HashMap<>();
            rate.put("value", rateElement.getAttribute("value"));
            rate.put("unit", rateElement.getAttribute("unit"));
            medication.put("rateQuantity", rate);
        }

        // Administration unit (e.g., tablet, capsule)
        Element adminUnitElement = (Element) xpath.evaluate("hl7:administrationUnitCode", substanceAdminElement, XPathConstants.NODE);
        if (adminUnitElement != null) {
            medication.put("administrationUnit", adminUnitElement.getAttribute("displayName"));
            medication.put("administrationUnitCode", adminUnitElement.getAttribute("code"));
        }

        // Consumable (the actual medication)
        Element consumableElement = (Element) xpath.evaluate(
            "hl7:consumable/hl7:manufacturedProduct/hl7:manufacturedMaterial",
            substanceAdminElement, XPathConstants.NODE);

        if (consumableElement != null) {
            extractMedicationCode(consumableElement, xpath, medication);
        }

        // Instructions (free text)
        String instructions = xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:act[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.20']]/hl7:text/text()",
            substanceAdminElement);
        if (instructions != null && !instructions.isEmpty()) {
            medication.put("instructions", instructions);
        }

        // Indication (reason for medication)
        Element indicationElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='RSON']/hl7:observation/hl7:value",
            substanceAdminElement, XPathConstants.NODE);

        if (indicationElement != null) {
            Map<String, String> indication = new HashMap<>();
            indication.put("code", indicationElement.getAttribute("code"));
            indication.put("displayName", indicationElement.getAttribute("displayName"));
            indication.put("codeSystem", indicationElement.getAttribute("codeSystem"));
            medication.put("indication", indication);
        }

        // Supply (dispense information)
        Element supplyElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='REFR']/hl7:supply",
            substanceAdminElement, XPathConstants.NODE);

        if (supplyElement != null) {
            extractSupplyInfo(supplyElement, xpath, medication);
        }

        // Prescriber
        Element authorElement = (Element) xpath.evaluate("hl7:author", substanceAdminElement, XPathConstants.NODE);
        if (authorElement != null) {
            Map<String, Object> prescriber = extractPrescriber(authorElement, xpath);
            medication.put("prescriber", prescriber);
        }

        return medication;
    }

    private void extractEffectiveTimes(Element substanceAdminElement, XPath xpath, Map<String, Object> medication)
            throws XPathExpressionException {

        NodeList effectiveTimeNodes = (NodeList) xpath.evaluate(
            "hl7:effectiveTime", substanceAdminElement, XPathConstants.NODESET);

        for (int i = 0; i < effectiveTimeNodes.getLength(); i++) {
            Element etElement = (Element) effectiveTimeNodes.item(i);
            String xsiType = etElement.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "type");

            if (xsiType != null && xsiType.contains("IVL_TS")) {
                // Period (start/end dates)
                Element lowElement = (Element) xpath.evaluate("hl7:low", etElement, XPathConstants.NODE);
                Element highElement = (Element) xpath.evaluate("hl7:high", etElement, XPathConstants.NODE);

                if (lowElement != null) {
                    medication.put("startDate", lowElement.getAttribute("value"));
                }
                if (highElement != null) {
                    medication.put("endDate", highElement.getAttribute("value"));
                }
            } else if (xsiType != null && xsiType.contains("PIVL_TS")) {
                // Frequency
                Element periodElement = (Element) xpath.evaluate("hl7:period", etElement, XPathConstants.NODE);
                if (periodElement != null) {
                    Map<String, String> frequency = new HashMap<>();
                    frequency.put("value", periodElement.getAttribute("value"));
                    frequency.put("unit", periodElement.getAttribute("unit"));
                    medication.put("frequency", frequency);
                }

                String institutionSpecified = etElement.getAttribute("institutionSpecified");
                if (institutionSpecified != null && !institutionSpecified.isEmpty()) {
                    medication.put("institutionSpecified", Boolean.parseBoolean(institutionSpecified));
                }
            }
        }
    }

    private void extractMedicationCode(Element materialElement, XPath xpath, Map<String, Object> medication)
            throws XPathExpressionException {

        Element codeElement = (Element) xpath.evaluate("hl7:code", materialElement, XPathConstants.NODE);
        if (codeElement != null) {
            Map<String, String> drugCode = new HashMap<>();
            drugCode.put("code", codeElement.getAttribute("code"));
            drugCode.put("codeSystem", codeElement.getAttribute("codeSystem"));
            drugCode.put("codeSystemName", codeElement.getAttribute("codeSystemName"));
            drugCode.put("displayName", codeElement.getAttribute("displayName"));
            medication.put("drugCode", drugCode);

            // Map code system OID to URI
            String codeSystemOid = codeElement.getAttribute("codeSystem");
            medication.put("codeSystemUri", mapCodeSystemToUri(codeSystemOid));

            // Translation codes (NDC, etc.)
            NodeList translationNodes = (NodeList) xpath.evaluate(
                "hl7:translation", codeElement, XPathConstants.NODESET);

            List<Map<String, String>> translations = new ArrayList<>();
            for (int i = 0; i < translationNodes.getLength(); i++) {
                Element transElement = (Element) translationNodes.item(i);
                Map<String, String> translation = new HashMap<>();
                translation.put("code", transElement.getAttribute("code"));
                translation.put("codeSystem", transElement.getAttribute("codeSystem"));
                translation.put("codeSystemName", transElement.getAttribute("codeSystemName"));
                translation.put("displayName", transElement.getAttribute("displayName"));
                translations.add(translation);
            }
            if (!translations.isEmpty()) {
                medication.put("translations", translations);
            }
        }

        // Lot number (if present)
        Element lotElement = (Element) xpath.evaluate("hl7:lotNumberText", materialElement, XPathConstants.NODE);
        if (lotElement != null) {
            medication.put("lotNumber", lotElement.getTextContent());
        }
    }

    private void extractSupplyInfo(Element supplyElement, XPath xpath, Map<String, Object> medication)
            throws XPathExpressionException {

        Map<String, Object> supply = new HashMap<>();

        // Quantity
        Element quantityElement = (Element) xpath.evaluate("hl7:quantity", supplyElement, XPathConstants.NODE);
        if (quantityElement != null) {
            supply.put("quantity", quantityElement.getAttribute("value"));
            supply.put("quantityUnit", quantityElement.getAttribute("unit"));
        }

        // Repeat number (refills)
        Element repeatElement = (Element) xpath.evaluate("hl7:repeatNumber", supplyElement, XPathConstants.NODE);
        if (repeatElement != null) {
            supply.put("refills", repeatElement.getAttribute("value"));
        }

        // Expected supply duration
        Element expectedElement = (Element) xpath.evaluate(
            "hl7:expectedUseTime/hl7:width", supplyElement, XPathConstants.NODE);
        if (expectedElement != null) {
            supply.put("expectedSupplyDuration", expectedElement.getAttribute("value"));
            supply.put("expectedSupplyDurationUnit", expectedElement.getAttribute("unit"));
        }

        if (!supply.isEmpty()) {
            medication.put("supply", supply);
        }
    }

    private Map<String, Object> extractPrescriber(Element authorElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> prescriber = new HashMap<>();

        // Author time
        Element timeElement = (Element) xpath.evaluate("hl7:time", authorElement, XPathConstants.NODE);
        if (timeElement != null) {
            prescriber.put("time", timeElement.getAttribute("value"));
        }

        // Prescriber ID (NPI)
        Element idElement = (Element) xpath.evaluate(
            "hl7:assignedAuthor/hl7:id", authorElement, XPathConstants.NODE);
        if (idElement != null) {
            prescriber.put("id", idElement.getAttribute("extension"));
            prescriber.put("idRoot", idElement.getAttribute("root"));
        }

        // Prescriber name
        Element nameElement = (Element) xpath.evaluate(
            "hl7:assignedAuthor/hl7:assignedPerson/hl7:name", authorElement, XPathConstants.NODE);
        if (nameElement != null) {
            String family = xpath.evaluate("hl7:family/text()", nameElement);
            String given = xpath.evaluate("hl7:given/text()", nameElement);
            String prefix = xpath.evaluate("hl7:prefix/text()", nameElement);
            prescriber.put("familyName", family);
            prescriber.put("givenName", given);
            if (prefix != null && !prefix.isEmpty()) {
                prescriber.put("prefix", prefix);
            }
        }

        return prescriber;
    }

    private String mapCodeSystemToUri(String codeSystemOid) {
        if (codeSystemOid == null) return null;

        return switch (codeSystemOid) {
            case "2.16.840.1.113883.6.88" -> "http://www.nlm.nih.gov/research/umls/rxnorm";
            case "2.16.840.1.113883.6.69" -> "http://hl7.org/fhir/sid/ndc";
            case "2.16.840.1.113883.6.96" -> "http://snomed.info/sct";
            default -> "urn:oid:" + codeSystemOid;
        };
    }
}

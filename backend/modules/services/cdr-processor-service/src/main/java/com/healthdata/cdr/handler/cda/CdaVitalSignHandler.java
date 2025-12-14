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
 * Handler for extracting vital signs from CDA Vital Signs Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.4.1
 */
@Slf4j
@Component
public class CdaVitalSignHandler {

    // Vital Signs Section Template OID
    private static final String VITAL_SIGNS_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.4.1";

    // Vital Signs Organizer Template OID
    private static final String VITAL_SIGNS_ORGANIZER_TEMPLATE = "2.16.840.1.113883.10.20.22.4.26";

    // Vital Sign Observation Template OID
    private static final String VITAL_SIGN_OBSERVATION_TEMPLATE = "2.16.840.1.113883.10.20.22.4.27";

    /**
     * Extract vital signs from CDA Vital Signs Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of vital sign maps
     */
    public List<Map<String, Object>> extractVitalSigns(Document doc, XPath xpath) {
        List<Map<String, Object>> vitalSigns = new ArrayList<>();

        try {
            // Find vital signs section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", VITAL_SIGNS_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Vital Signs section found in document");
                return vitalSigns;
            }

            // Find all vital signs organizers
            NodeList organizerNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:organizer[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.26']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < organizerNodes.getLength(); i++) {
                Element organizerElement = (Element) organizerNodes.item(i);
                List<Map<String, Object>> panelVitals = extractVitalSignsOrganizer(organizerElement, xpath);
                vitalSigns.addAll(panelVitals);
            }

            log.debug("Extracted {} vital signs from document", vitalSigns.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting vital signs: {}", e.getMessage(), e);
        }

        return vitalSigns;
    }

    private List<Map<String, Object>> extractVitalSignsOrganizer(Element organizerElement, XPath xpath)
            throws XPathExpressionException {
        List<Map<String, Object>> vitalSigns = new ArrayList<>();

        // Panel effective time (when vitals were taken)
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", organizerElement, XPathConstants.NODE);
        String panelDateTime = null;
        if (effectiveTimeElement != null) {
            panelDateTime = effectiveTimeElement.getAttribute("value");
        }

        // Panel status
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", organizerElement, XPathConstants.NODE);
        String panelStatus = statusElement != null ? statusElement.getAttribute("code") : null;

        // Find all vital sign observations in this organizer
        NodeList observationNodes = (NodeList) xpath.evaluate(
            "hl7:component/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.27']]",
            organizerElement, XPathConstants.NODESET);

        for (int i = 0; i < observationNodes.getLength(); i++) {
            Element observationElement = (Element) observationNodes.item(i);
            Map<String, Object> vitalSign = extractVitalSignObservation(observationElement, xpath);

            // Add panel context if not already set
            if (panelDateTime != null && vitalSign.get("effectiveDateTime") == null) {
                vitalSign.put("effectiveDateTime", panelDateTime);
            }
            if (panelStatus != null) {
                vitalSign.put("panelStatus", panelStatus);
            }

            vitalSigns.add(vitalSign);
        }

        return vitalSigns;
    }

    private Map<String, Object> extractVitalSignObservation(Element observationElement, XPath xpath)
            throws XPathExpressionException {
        Map<String, Object> vitalSign = new HashMap<>();

        // Observation ID
        Element idElement = (Element) xpath.evaluate("hl7:id", observationElement, XPathConstants.NODE);
        if (idElement != null) {
            vitalSign.put("id", idElement.getAttribute("root"));
            vitalSign.put("extension", idElement.getAttribute("extension"));
        }

        // Vital sign code (LOINC)
        Element codeElement = (Element) xpath.evaluate("hl7:code", observationElement, XPathConstants.NODE);
        if (codeElement != null) {
            Map<String, String> code = new HashMap<>();
            code.put("code", codeElement.getAttribute("code"));
            code.put("codeSystem", codeElement.getAttribute("codeSystem"));
            code.put("codeSystemName", codeElement.getAttribute("codeSystemName"));
            code.put("displayName", codeElement.getAttribute("displayName"));
            vitalSign.put("code", code);

            // Determine vital sign category
            String loincCode = codeElement.getAttribute("code");
            vitalSign.put("category", categorizeVitalSign(loincCode));

            vitalSign.put("codeSystemUri", "http://loinc.org");
        }

        // Status
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", observationElement, XPathConstants.NODE);
        if (statusElement != null) {
            vitalSign.put("status", statusElement.getAttribute("code"));
        }

        // Effective time
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", observationElement, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            String value = effectiveTimeElement.getAttribute("value");
            if (value != null && !value.isEmpty()) {
                vitalSign.put("effectiveDateTime", value);
            }
        }

        // Value (physical quantity)
        Element valueElement = (Element) xpath.evaluate("hl7:value", observationElement, XPathConstants.NODE);
        if (valueElement != null) {
            Map<String, String> valueQuantity = new HashMap<>();
            valueQuantity.put("value", valueElement.getAttribute("value"));
            valueQuantity.put("unit", valueElement.getAttribute("unit"));
            vitalSign.put("valueQuantity", valueQuantity);
        }

        // Interpretation code
        Element interpretationElement = (Element) xpath.evaluate("hl7:interpretationCode", observationElement, XPathConstants.NODE);
        if (interpretationElement != null) {
            Map<String, String> interpretation = new HashMap<>();
            interpretation.put("code", interpretationElement.getAttribute("code"));
            interpretation.put("displayName", interpretationElement.getAttribute("displayName"));
            vitalSign.put("interpretation", interpretation);
        }

        // Method
        Element methodElement = (Element) xpath.evaluate("hl7:methodCode", observationElement, XPathConstants.NODE);
        if (methodElement != null) {
            vitalSign.put("method", methodElement.getAttribute("displayName"));
            vitalSign.put("methodCode", methodElement.getAttribute("code"));
        }

        // Target site (for body position, etc.)
        Element targetSiteElement = (Element) xpath.evaluate("hl7:targetSiteCode", observationElement, XPathConstants.NODE);
        if (targetSiteElement != null) {
            Map<String, String> bodySite = new HashMap<>();
            bodySite.put("code", targetSiteElement.getAttribute("code"));
            bodySite.put("displayName", targetSiteElement.getAttribute("displayName"));
            vitalSign.put("bodySite", bodySite);
        }

        // Performer
        Element performerElement = (Element) xpath.evaluate("hl7:performer", observationElement, XPathConstants.NODE);
        if (performerElement != null) {
            Map<String, Object> performer = extractPerformer(performerElement, xpath);
            vitalSign.put("performer", performer);
        }

        // Author
        Element authorElement = (Element) xpath.evaluate("hl7:author", observationElement, XPathConstants.NODE);
        if (authorElement != null) {
            Element authorTimeElement = (Element) xpath.evaluate("hl7:time", authorElement, XPathConstants.NODE);
            if (authorTimeElement != null) {
                vitalSign.put("recordedDateTime", authorTimeElement.getAttribute("value"));
            }
        }

        return vitalSign;
    }

    private Map<String, Object> extractPerformer(Element performerElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> performer = new HashMap<>();

        // Performer ID
        Element idElement = (Element) xpath.evaluate(
            "hl7:assignedEntity/hl7:id", performerElement, XPathConstants.NODE);
        if (idElement != null) {
            performer.put("id", idElement.getAttribute("extension"));
            performer.put("idRoot", idElement.getAttribute("root"));
        }

        // Performer name
        Element nameElement = (Element) xpath.evaluate(
            "hl7:assignedEntity/hl7:assignedPerson/hl7:name", performerElement, XPathConstants.NODE);
        if (nameElement != null) {
            String family = xpath.evaluate("hl7:family/text()", nameElement);
            String given = xpath.evaluate("hl7:given/text()", nameElement);
            performer.put("familyName", family);
            performer.put("givenName", given);
        }

        return performer;
    }

    /**
     * Categorize vital sign based on LOINC code.
     */
    private String categorizeVitalSign(String loincCode) {
        if (loincCode == null) return "vital-signs";

        return switch (loincCode) {
            // Blood Pressure
            case "8480-6" -> "blood-pressure-systolic";
            case "8462-4" -> "blood-pressure-diastolic";
            case "85354-9" -> "blood-pressure";

            // Heart Rate
            case "8867-4" -> "heart-rate";

            // Respiratory Rate
            case "9279-1" -> "respiratory-rate";

            // Body Temperature
            case "8310-5" -> "body-temperature";

            // Oxygen Saturation
            case "2708-6", "59408-5" -> "oxygen-saturation";

            // Body Height
            case "8302-2" -> "body-height";

            // Body Weight
            case "29463-7" -> "body-weight";

            // BMI
            case "39156-5" -> "bmi";

            // Head Circumference
            case "9843-4", "8287-5" -> "head-circumference";

            default -> "vital-signs";
        };
    }
}

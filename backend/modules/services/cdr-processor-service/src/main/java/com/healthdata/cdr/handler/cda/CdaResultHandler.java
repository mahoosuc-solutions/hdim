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
 * Handler for extracting lab results from CDA Results Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.3.1
 */
@Slf4j
@Component
public class CdaResultHandler {

    // Results Section Template OID
    private static final String RESULTS_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.3.1";

    // Result Organizer Template OID
    private static final String RESULT_ORGANIZER_TEMPLATE = "2.16.840.1.113883.10.20.22.4.1";

    // Result Observation Template OID
    private static final String RESULT_OBSERVATION_TEMPLATE = "2.16.840.1.113883.10.20.22.4.2";

    /**
     * Extract lab results from CDA Results Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of result maps
     */
    public List<Map<String, Object>> extractResults(Document doc, XPath xpath) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            // Find results section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", RESULTS_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Results section found in document");
                return results;
            }

            // Find all result organizers
            NodeList organizerNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:organizer[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.1']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < organizerNodes.getLength(); i++) {
                Element organizerElement = (Element) organizerNodes.item(i);
                List<Map<String, Object>> panelResults = extractResultOrganizer(organizerElement, xpath);
                results.addAll(panelResults);
            }

            log.debug("Extracted {} results from document", results.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting results: {}", e.getMessage(), e);
        }

        return results;
    }

    private List<Map<String, Object>> extractResultOrganizer(Element organizerElement, XPath xpath)
            throws XPathExpressionException {
        List<Map<String, Object>> results = new ArrayList<>();

        // Panel/battery information
        Map<String, Object> panelInfo = new HashMap<>();

        Element panelCodeElement = (Element) xpath.evaluate("hl7:code", organizerElement, XPathConstants.NODE);
        if (panelCodeElement != null) {
            panelInfo.put("panelCode", panelCodeElement.getAttribute("code"));
            panelInfo.put("panelCodeSystem", panelCodeElement.getAttribute("codeSystem"));
            panelInfo.put("panelDisplayName", panelCodeElement.getAttribute("displayName"));
        }

        // Panel status
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", organizerElement, XPathConstants.NODE);
        String panelStatus = statusElement != null ? statusElement.getAttribute("code") : null;

        // Find all result observations in this organizer
        NodeList observationNodes = (NodeList) xpath.evaluate(
            "hl7:component/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.2']]",
            organizerElement, XPathConstants.NODESET);

        for (int i = 0; i < observationNodes.getLength(); i++) {
            Element observationElement = (Element) observationNodes.item(i);
            Map<String, Object> result = extractResultObservation(observationElement, xpath);

            // Add panel context
            result.put("panel", new HashMap<>(panelInfo));
            if (panelStatus != null) {
                result.put("panelStatus", panelStatus);
            }

            results.add(result);
        }

        return results;
    }

    private Map<String, Object> extractResultObservation(Element observationElement, XPath xpath)
            throws XPathExpressionException {
        Map<String, Object> result = new HashMap<>();

        // Result ID
        Element idElement = (Element) xpath.evaluate("hl7:id", observationElement, XPathConstants.NODE);
        if (idElement != null) {
            result.put("id", idElement.getAttribute("root"));
            result.put("extension", idElement.getAttribute("extension"));
        }

        // Result code (LOINC)
        Element codeElement = (Element) xpath.evaluate("hl7:code", observationElement, XPathConstants.NODE);
        if (codeElement != null) {
            Map<String, String> code = new HashMap<>();
            code.put("code", codeElement.getAttribute("code"));
            code.put("codeSystem", codeElement.getAttribute("codeSystem"));
            code.put("codeSystemName", codeElement.getAttribute("codeSystemName"));
            code.put("displayName", codeElement.getAttribute("displayName"));
            result.put("code", code);

            // Map code system to URI
            String codeSystemOid = codeElement.getAttribute("codeSystem");
            result.put("codeSystemUri", mapCodeSystemToUri(codeSystemOid));
        }

        // Status
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", observationElement, XPathConstants.NODE);
        if (statusElement != null) {
            result.put("status", statusElement.getAttribute("code"));
        }

        // Effective time (when specimen collected or result observed)
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", observationElement, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            String value = effectiveTimeElement.getAttribute("value");
            if (value != null && !value.isEmpty()) {
                result.put("effectiveDateTime", value);
            } else {
                Element lowElement = (Element) xpath.evaluate("hl7:low", effectiveTimeElement, XPathConstants.NODE);
                if (lowElement != null) {
                    result.put("effectiveDateTime", lowElement.getAttribute("value"));
                }
            }
        }

        // Value - can be different types (PQ, ST, CD, CO, etc.)
        Element valueElement = (Element) xpath.evaluate("hl7:value", observationElement, XPathConstants.NODE);
        if (valueElement != null) {
            extractValue(valueElement, result);
        }

        // Interpretation code (normal, abnormal, etc.)
        Element interpretationElement = (Element) xpath.evaluate("hl7:interpretationCode", observationElement, XPathConstants.NODE);
        if (interpretationElement != null) {
            Map<String, String> interpretation = new HashMap<>();
            interpretation.put("code", interpretationElement.getAttribute("code"));
            interpretation.put("codeSystem", interpretationElement.getAttribute("codeSystem"));
            interpretation.put("displayName", interpretationElement.getAttribute("displayName"));
            result.put("interpretation", interpretation);
        }

        // Reference range
        Element referenceRangeElement = (Element) xpath.evaluate(
            "hl7:referenceRange/hl7:observationRange", observationElement, XPathConstants.NODE);
        if (referenceRangeElement != null) {
            Map<String, Object> referenceRange = extractReferenceRange(referenceRangeElement, xpath);
            result.put("referenceRange", referenceRange);
        }

        // Method code
        Element methodElement = (Element) xpath.evaluate("hl7:methodCode", observationElement, XPathConstants.NODE);
        if (methodElement != null) {
            result.put("method", methodElement.getAttribute("displayName"));
            result.put("methodCode", methodElement.getAttribute("code"));
        }

        // Specimen
        Element specimenElement = (Element) xpath.evaluate(
            "hl7:specimen/hl7:specimenRole/hl7:specimenPlayingEntity/hl7:code",
            observationElement, XPathConstants.NODE);
        if (specimenElement != null) {
            Map<String, String> specimen = new HashMap<>();
            specimen.put("code", specimenElement.getAttribute("code"));
            specimen.put("displayName", specimenElement.getAttribute("displayName"));
            result.put("specimen", specimen);
        }

        // Performer
        Element performerElement = (Element) xpath.evaluate("hl7:performer", observationElement, XPathConstants.NODE);
        if (performerElement != null) {
            Map<String, Object> performer = extractPerformer(performerElement, xpath);
            result.put("performer", performer);
        }

        // Author (who recorded)
        Element authorElement = (Element) xpath.evaluate("hl7:author", observationElement, XPathConstants.NODE);
        if (authorElement != null) {
            Element authorTimeElement = (Element) xpath.evaluate("hl7:time", authorElement, XPathConstants.NODE);
            if (authorTimeElement != null) {
                result.put("recordedDateTime", authorTimeElement.getAttribute("value"));
            }
        }

        return result;
    }

    private void extractValue(Element valueElement, Map<String, Object> result) {
        String xsiType = valueElement.getAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "type");

        if (xsiType == null) {
            xsiType = "";
        }

        if (xsiType.contains("PQ")) {
            // Physical quantity (numeric with unit)
            Map<String, String> valueQuantity = new HashMap<>();
            valueQuantity.put("value", valueElement.getAttribute("value"));
            valueQuantity.put("unit", valueElement.getAttribute("unit"));
            result.put("valueQuantity", valueQuantity);
            result.put("valueType", "Quantity");

        } else if (xsiType.contains("ST") || xsiType.contains("ED")) {
            // String or text
            String textValue = valueElement.getTextContent();
            result.put("valueString", textValue);
            result.put("valueType", "String");

        } else if (xsiType.contains("CD") || xsiType.contains("CE") || xsiType.contains("CO")) {
            // Coded value
            Map<String, String> codedValue = new HashMap<>();
            codedValue.put("code", valueElement.getAttribute("code"));
            codedValue.put("codeSystem", valueElement.getAttribute("codeSystem"));
            codedValue.put("displayName", valueElement.getAttribute("displayName"));
            result.put("valueCodeableConcept", codedValue);
            result.put("valueType", "CodeableConcept");

        } else if (xsiType.contains("IVL_PQ")) {
            // Interval of physical quantities (range)
            Map<String, Object> valueRange = new HashMap<>();
            try {
                // These evaluations happen in a try block since xpath operations can throw
                String lowValue = valueElement.getElementsByTagNameNS("*", "low").item(0) != null ?
                    ((Element) valueElement.getElementsByTagNameNS("*", "low").item(0)).getAttribute("value") : null;
                String highValue = valueElement.getElementsByTagNameNS("*", "high").item(0) != null ?
                    ((Element) valueElement.getElementsByTagNameNS("*", "high").item(0)).getAttribute("value") : null;
                String unit = valueElement.getElementsByTagNameNS("*", "low").item(0) != null ?
                    ((Element) valueElement.getElementsByTagNameNS("*", "low").item(0)).getAttribute("unit") : null;

                if (lowValue != null) valueRange.put("low", lowValue);
                if (highValue != null) valueRange.put("high", highValue);
                if (unit != null) valueRange.put("unit", unit);
            } catch (Exception e) {
                log.debug("Could not extract IVL_PQ value: {}", e.getMessage());
            }
            result.put("valueRange", valueRange);
            result.put("valueType", "Range");

        } else if (xsiType.contains("INT")) {
            // Integer
            result.put("valueInteger", valueElement.getAttribute("value"));
            result.put("valueType", "Integer");

        } else if (xsiType.contains("REAL")) {
            // Real number
            result.put("valueDecimal", valueElement.getAttribute("value"));
            result.put("valueType", "Decimal");

        } else if (xsiType.contains("BL")) {
            // Boolean
            result.put("valueBoolean", Boolean.parseBoolean(valueElement.getAttribute("value")));
            result.put("valueType", "Boolean");

        } else if (xsiType.contains("RTO")) {
            // Ratio
            Map<String, String> ratio = new HashMap<>();
            try {
                Element numerator = (Element) valueElement.getElementsByTagNameNS("*", "numerator").item(0);
                Element denominator = (Element) valueElement.getElementsByTagNameNS("*", "denominator").item(0);
                if (numerator != null) {
                    ratio.put("numeratorValue", numerator.getAttribute("value"));
                    ratio.put("numeratorUnit", numerator.getAttribute("unit"));
                }
                if (denominator != null) {
                    ratio.put("denominatorValue", denominator.getAttribute("value"));
                    ratio.put("denominatorUnit", denominator.getAttribute("unit"));
                }
            } catch (Exception e) {
                log.debug("Could not extract RTO value: {}", e.getMessage());
            }
            result.put("valueRatio", ratio);
            result.put("valueType", "Ratio");

        } else {
            // Default: treat as string
            String textContent = valueElement.getTextContent();
            if (textContent != null && !textContent.isEmpty()) {
                result.put("valueString", textContent);
            } else {
                // Try to get value attribute
                String attrValue = valueElement.getAttribute("value");
                if (attrValue != null && !attrValue.isEmpty()) {
                    result.put("valueString", attrValue);
                }
            }
            result.put("valueType", "String");
        }
    }

    private Map<String, Object> extractReferenceRange(Element rangeElement, XPath xpath)
            throws XPathExpressionException {
        Map<String, Object> referenceRange = new HashMap<>();

        // Text description
        String text = xpath.evaluate("hl7:text/text()", rangeElement);
        if (text != null && !text.isEmpty()) {
            referenceRange.put("text", text);
        }

        // Value range
        Element valueElement = (Element) xpath.evaluate("hl7:value", rangeElement, XPathConstants.NODE);
        if (valueElement != null) {
            Element lowElement = (Element) xpath.evaluate("hl7:low", valueElement, XPathConstants.NODE);
            Element highElement = (Element) xpath.evaluate("hl7:high", valueElement, XPathConstants.NODE);

            if (lowElement != null) {
                referenceRange.put("lowValue", lowElement.getAttribute("value"));
                referenceRange.put("unit", lowElement.getAttribute("unit"));
            }
            if (highElement != null) {
                referenceRange.put("highValue", highElement.getAttribute("value"));
                if (referenceRange.get("unit") == null) {
                    referenceRange.put("unit", highElement.getAttribute("unit"));
                }
            }
        }

        // Interpretation code (for who the range applies to)
        Element interpretationElement = (Element) xpath.evaluate("hl7:interpretationCode", rangeElement, XPathConstants.NODE);
        if (interpretationElement != null) {
            referenceRange.put("type", interpretationElement.getAttribute("code"));
            referenceRange.put("typeDisplay", interpretationElement.getAttribute("displayName"));
        }

        return referenceRange;
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

        // Performing organization
        String orgName = xpath.evaluate(
            "hl7:assignedEntity/hl7:representedOrganization/hl7:name/text()", performerElement);
        if (orgName != null && !orgName.isEmpty()) {
            performer.put("organization", orgName);
        }

        return performer;
    }

    private String mapCodeSystemToUri(String codeSystemOid) {
        if (codeSystemOid == null) return null;

        return switch (codeSystemOid) {
            case "2.16.840.1.113883.6.1" -> "http://loinc.org";
            case "2.16.840.1.113883.6.96" -> "http://snomed.info/sct";
            default -> "urn:oid:" + codeSystemOid;
        };
    }
}

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
 * Handler for extracting procedures from CDA Procedures Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.7.1
 */
@Slf4j
@Component
public class CdaProcedureHandler {

    // Procedures Section Template OID
    private static final String PROCEDURES_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.7.1";

    // Procedure Activity Templates
    private static final String PROCEDURE_ACTIVITY_PROCEDURE = "2.16.840.1.113883.10.20.22.4.14";
    private static final String PROCEDURE_ACTIVITY_OBSERVATION = "2.16.840.1.113883.10.20.22.4.13";
    private static final String PROCEDURE_ACTIVITY_ACT = "2.16.840.1.113883.10.20.22.4.12";

    /**
     * Extract procedures from CDA Procedures Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of procedure maps
     */
    public List<Map<String, Object>> extractProcedures(Document doc, XPath xpath) {
        List<Map<String, Object>> procedures = new ArrayList<>();

        try {
            // Find procedures section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", PROCEDURES_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Procedures section found in document");
                return procedures;
            }

            // Find procedure activity procedures
            NodeList procedureNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:procedure[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.14']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < procedureNodes.getLength(); i++) {
                Element procedureElement = (Element) procedureNodes.item(i);
                Map<String, Object> procedure = extractProcedureActivity(procedureElement, xpath, "procedure");
                if (!procedure.isEmpty()) {
                    procedures.add(procedure);
                }
            }

            // Find procedure activity observations
            NodeList observationNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.13']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < observationNodes.getLength(); i++) {
                Element observationElement = (Element) observationNodes.item(i);
                Map<String, Object> procedure = extractProcedureActivity(observationElement, xpath, "observation");
                if (!procedure.isEmpty()) {
                    procedures.add(procedure);
                }
            }

            // Find procedure activity acts
            NodeList actNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:act[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.12']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < actNodes.getLength(); i++) {
                Element actElement = (Element) actNodes.item(i);
                Map<String, Object> procedure = extractProcedureActivity(actElement, xpath, "act");
                if (!procedure.isEmpty()) {
                    procedures.add(procedure);
                }
            }

            log.debug("Extracted {} procedures from document", procedures.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting procedures: {}", e.getMessage(), e);
        }

        return procedures;
    }

    private Map<String, Object> extractProcedureActivity(Element element, XPath xpath, String activityType)
            throws XPathExpressionException {
        Map<String, Object> procedure = new HashMap<>();
        procedure.put("activityType", activityType);

        // Procedure ID
        Element idElement = (Element) xpath.evaluate("hl7:id", element, XPathConstants.NODE);
        if (idElement != null) {
            procedure.put("id", idElement.getAttribute("root"));
            procedure.put("extension", idElement.getAttribute("extension"));
        }

        // Procedure code
        Element codeElement = (Element) xpath.evaluate("hl7:code", element, XPathConstants.NODE);
        if (codeElement != null) {
            Map<String, String> code = new HashMap<>();
            code.put("code", codeElement.getAttribute("code"));
            code.put("codeSystem", codeElement.getAttribute("codeSystem"));
            code.put("codeSystemName", codeElement.getAttribute("codeSystemName"));
            code.put("displayName", codeElement.getAttribute("displayName"));
            procedure.put("code", code);

            // Map code system to URI
            String codeSystemOid = codeElement.getAttribute("codeSystem");
            procedure.put("codeSystemUri", mapCodeSystemToUri(codeSystemOid));

            // Translation codes
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
                procedure.put("translations", translations);
            }
        }

        // Status
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", element, XPathConstants.NODE);
        if (statusElement != null) {
            procedure.put("status", statusElement.getAttribute("code"));
        }

        // Effective time (when performed)
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", element, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            String value = effectiveTimeElement.getAttribute("value");
            if (value != null && !value.isEmpty()) {
                procedure.put("performedDateTime", value);
            } else {
                Element lowElement = (Element) xpath.evaluate("hl7:low", effectiveTimeElement, XPathConstants.NODE);
                Element highElement = (Element) xpath.evaluate("hl7:high", effectiveTimeElement, XPathConstants.NODE);

                if (lowElement != null) {
                    procedure.put("performedPeriodStart", lowElement.getAttribute("value"));
                }
                if (highElement != null) {
                    procedure.put("performedPeriodEnd", highElement.getAttribute("value"));
                }
            }
        }

        // Priority code
        Element priorityElement = (Element) xpath.evaluate("hl7:priorityCode", element, XPathConstants.NODE);
        if (priorityElement != null) {
            procedure.put("priority", priorityElement.getAttribute("code"));
            procedure.put("priorityDisplay", priorityElement.getAttribute("displayName"));
        }

        // Method code
        Element methodElement = (Element) xpath.evaluate("hl7:methodCode", element, XPathConstants.NODE);
        if (methodElement != null) {
            Map<String, String> method = new HashMap<>();
            method.put("code", methodElement.getAttribute("code"));
            method.put("displayName", methodElement.getAttribute("displayName"));
            procedure.put("method", method);
        }

        // Target site
        Element targetSiteElement = (Element) xpath.evaluate("hl7:targetSiteCode", element, XPathConstants.NODE);
        if (targetSiteElement != null) {
            Map<String, String> bodySite = new HashMap<>();
            bodySite.put("code", targetSiteElement.getAttribute("code"));
            bodySite.put("displayName", targetSiteElement.getAttribute("displayName"));
            bodySite.put("codeSystem", targetSiteElement.getAttribute("codeSystem"));
            procedure.put("bodySite", bodySite);
        }

        // Performer
        Element performerElement = (Element) xpath.evaluate("hl7:performer", element, XPathConstants.NODE);
        if (performerElement != null) {
            Map<String, Object> performer = extractPerformer(performerElement, xpath);
            procedure.put("performer", performer);
        }

        // Service delivery location
        Element locationElement = (Element) xpath.evaluate(
            "hl7:participant[@typeCode='LOC']/hl7:participantRole/hl7:playingEntity",
            element, XPathConstants.NODE);

        if (locationElement != null) {
            String locationName = xpath.evaluate("hl7:name/text()", locationElement);
            if (locationName != null && !locationName.isEmpty()) {
                procedure.put("location", locationName);
            }
        }

        // Indication (reason for procedure)
        Element indicationElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='RSON']/hl7:observation/hl7:value",
            element, XPathConstants.NODE);

        if (indicationElement != null) {
            Map<String, String> indication = new HashMap<>();
            indication.put("code", indicationElement.getAttribute("code"));
            indication.put("displayName", indicationElement.getAttribute("displayName"));
            indication.put("codeSystem", indicationElement.getAttribute("codeSystem"));
            procedure.put("indication", indication);
        }

        // Note/comment
        String text = xpath.evaluate("hl7:text/text()", element);
        if (text != null && !text.isEmpty()) {
            procedure.put("note", text);
        }

        return procedure;
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
            String prefix = xpath.evaluate("hl7:prefix/text()", nameElement);
            performer.put("familyName", family);
            performer.put("givenName", given);
            if (prefix != null && !prefix.isEmpty()) {
                performer.put("prefix", prefix);
            }
        }

        // Performer organization
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
            case "2.16.840.1.113883.6.96" -> "http://snomed.info/sct";
            case "2.16.840.1.113883.6.12" -> "http://www.ama-assn.org/go/cpt";
            case "2.16.840.1.113883.6.4" -> "http://www.cms.gov/Medicare/Coding/ICD10";
            case "2.16.840.1.113883.6.104" -> "http://hl7.org/fhir/sid/icd-9-cm";
            case "2.16.840.1.113883.6.14" -> "http://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets";
            default -> "urn:oid:" + codeSystemOid;
        };
    }
}

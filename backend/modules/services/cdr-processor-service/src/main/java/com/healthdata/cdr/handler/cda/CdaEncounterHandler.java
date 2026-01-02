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
 * Handler for extracting encounters from CDA Encounters Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.22.1
 */
@Slf4j
@Component
public class CdaEncounterHandler {

    // Encounters Section Template OID
    private static final String ENCOUNTERS_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.22.1";

    // Encounter Activity Template OID
    private static final String ENCOUNTER_ACTIVITY_TEMPLATE = "2.16.840.1.113883.10.20.22.4.49";

    /**
     * Extract encounters from CDA Encounters Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of encounter maps
     */
    public List<Map<String, Object>> extractEncounters(Document doc, XPath xpath) {
        List<Map<String, Object>> encounters = new ArrayList<>();

        try {
            // Find encounters section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", ENCOUNTERS_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Encounters section found in document");
                return encounters;
            }

            // Find all encounter activities
            NodeList entryNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:encounter[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.49']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element encounterElement = (Element) entryNodes.item(i);
                Map<String, Object> encounter = extractEncounterActivity(encounterElement, xpath);
                if (!encounter.isEmpty()) {
                    encounters.add(encounter);
                }
            }

            log.debug("Extracted {} encounters from document", encounters.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting encounters: {}", e.getMessage(), e);
        }

        return encounters;
    }

    private Map<String, Object> extractEncounterActivity(Element encounterElement, XPath xpath)
            throws XPathExpressionException {
        Map<String, Object> encounter = new HashMap<>();

        // Encounter ID
        Element idElement = (Element) xpath.evaluate("hl7:id", encounterElement, XPathConstants.NODE);
        if (idElement != null) {
            encounter.put("id", idElement.getAttribute("root"));
            encounter.put("extension", idElement.getAttribute("extension"));
        }

        // Encounter code (type of encounter)
        Element codeElement = (Element) xpath.evaluate("hl7:code", encounterElement, XPathConstants.NODE);
        if (codeElement != null) {
            Map<String, String> code = new HashMap<>();
            code.put("code", codeElement.getAttribute("code"));
            code.put("codeSystem", codeElement.getAttribute("codeSystem"));
            code.put("codeSystemName", codeElement.getAttribute("codeSystemName"));
            code.put("displayName", codeElement.getAttribute("displayName"));
            encounter.put("type", code);

            // Map code system to URI
            String codeSystemOid = codeElement.getAttribute("codeSystem");
            encounter.put("typeCodeSystemUri", mapCodeSystemToUri(codeSystemOid));

            // Translation codes
            NodeList translationNodes = (NodeList) xpath.evaluate(
                "hl7:translation", codeElement, XPathConstants.NODESET);

            List<Map<String, String>> translations = new ArrayList<>();
            for (int i = 0; i < translationNodes.getLength(); i++) {
                Element transElement = (Element) translationNodes.item(i);
                Map<String, String> translation = new HashMap<>();
                translation.put("code", transElement.getAttribute("code"));
                translation.put("codeSystem", transElement.getAttribute("codeSystem"));
                translation.put("displayName", transElement.getAttribute("displayName"));
                translations.add(translation);
            }
            if (!translations.isEmpty()) {
                encounter.put("typeTranslations", translations);
            }
        }

        // Status (for FHIR mapping)
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", encounterElement, XPathConstants.NODE);
        if (statusElement != null) {
            encounter.put("status", mapStatusToFhir(statusElement.getAttribute("code")));
        }

        // Effective time (period of encounter)
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", encounterElement, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            String value = effectiveTimeElement.getAttribute("value");
            if (value != null && !value.isEmpty()) {
                encounter.put("periodStart", value);
                encounter.put("periodEnd", value);
            } else {
                Element lowElement = (Element) xpath.evaluate("hl7:low", effectiveTimeElement, XPathConstants.NODE);
                Element highElement = (Element) xpath.evaluate("hl7:high", effectiveTimeElement, XPathConstants.NODE);

                if (lowElement != null) {
                    encounter.put("periodStart", lowElement.getAttribute("value"));
                }
                if (highElement != null) {
                    encounter.put("periodEnd", highElement.getAttribute("value"));
                }
            }
        }

        // Performer (practitioner)
        NodeList performerNodes = (NodeList) xpath.evaluate("hl7:performer", encounterElement, XPathConstants.NODESET);
        List<Map<String, Object>> performers = new ArrayList<>();
        for (int i = 0; i < performerNodes.getLength(); i++) {
            Element performerElement = (Element) performerNodes.item(i);
            Map<String, Object> performer = extractPerformer(performerElement, xpath);
            if (!performer.isEmpty()) {
                performers.add(performer);
            }
        }
        if (!performers.isEmpty()) {
            encounter.put("participants", performers);
        }

        // Location (service delivery location)
        Element locationElement = (Element) xpath.evaluate(
            "hl7:participant[@typeCode='LOC']/hl7:participantRole",
            encounterElement, XPathConstants.NODE);

        if (locationElement != null) {
            Map<String, Object> location = extractLocation(locationElement, xpath);
            encounter.put("location", location);
        }

        // Encounter diagnosis
        NodeList diagnosisNodes = (NodeList) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:act[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.80']]",
            encounterElement, XPathConstants.NODESET);

        List<Map<String, Object>> diagnoses = new ArrayList<>();
        for (int i = 0; i < diagnosisNodes.getLength(); i++) {
            Element diagnosisElement = (Element) diagnosisNodes.item(i);
            Map<String, Object> diagnosis = extractDiagnosis(diagnosisElement, xpath);
            if (!diagnosis.isEmpty()) {
                diagnoses.add(diagnosis);
            }
        }
        if (!diagnoses.isEmpty()) {
            encounter.put("diagnoses", diagnoses);
        }

        // Reason for visit
        Element reasonElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='RSON']/hl7:observation/hl7:value",
            encounterElement, XPathConstants.NODE);

        if (reasonElement != null) {
            Map<String, String> reason = new HashMap<>();
            reason.put("code", reasonElement.getAttribute("code"));
            reason.put("displayName", reasonElement.getAttribute("displayName"));
            reason.put("codeSystem", reasonElement.getAttribute("codeSystem"));
            encounter.put("reasonCode", reason);
        }

        // Discharge disposition
        Element dischargeElement = (Element) xpath.evaluate(
            "sdtc:dischargeDispositionCode", encounterElement, XPathConstants.NODE);

        if (dischargeElement != null) {
            Map<String, String> dischargeDisposition = new HashMap<>();
            dischargeDisposition.put("code", dischargeElement.getAttribute("code"));
            dischargeDisposition.put("displayName", dischargeElement.getAttribute("displayName"));
            dischargeDisposition.put("codeSystem", dischargeElement.getAttribute("codeSystem"));
            encounter.put("dischargeDisposition", dischargeDisposition);
        }

        // Class (inpatient, outpatient, emergency, etc.) - from code
        String encounterClass = determineEncounterClass(encounter);
        if (encounterClass != null) {
            encounter.put("class", encounterClass);
        }

        return encounter;
    }

    private Map<String, Object> extractPerformer(Element performerElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> performer = new HashMap<>();

        // Type code
        String typeCode = performerElement.getAttribute("typeCode");
        if (typeCode != null && !typeCode.isEmpty()) {
            performer.put("type", mapPerformerType(typeCode));
        }

        // Performer ID (NPI)
        Element idElement = (Element) xpath.evaluate(
            "hl7:assignedEntity/hl7:id", performerElement, XPathConstants.NODE);
        if (idElement != null) {
            performer.put("id", idElement.getAttribute("extension"));
            performer.put("idRoot", idElement.getAttribute("root"));
        }

        // Performer code (role/specialty)
        Element codeElement = (Element) xpath.evaluate(
            "hl7:assignedEntity/hl7:code", performerElement, XPathConstants.NODE);
        if (codeElement != null) {
            performer.put("role", codeElement.getAttribute("code"));
            performer.put("roleDisplay", codeElement.getAttribute("displayName"));
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

        // Organization
        String orgName = xpath.evaluate(
            "hl7:assignedEntity/hl7:representedOrganization/hl7:name/text()", performerElement);
        if (orgName != null && !orgName.isEmpty()) {
            performer.put("organization", orgName);
        }

        return performer;
    }

    private Map<String, Object> extractLocation(Element locationElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> location = new HashMap<>();

        // Location ID
        Element idElement = (Element) xpath.evaluate("hl7:id", locationElement, XPathConstants.NODE);
        if (idElement != null) {
            location.put("id", idElement.getAttribute("extension"));
            location.put("idRoot", idElement.getAttribute("root"));
        }

        // Location code
        Element codeElement = (Element) xpath.evaluate("hl7:code", locationElement, XPathConstants.NODE);
        if (codeElement != null) {
            location.put("type", codeElement.getAttribute("code"));
            location.put("typeDisplay", codeElement.getAttribute("displayName"));
        }

        // Location name
        String name = xpath.evaluate("hl7:playingEntity/hl7:name/text()", locationElement);
        if (name != null && !name.isEmpty()) {
            location.put("name", name);
        }

        // Location address
        Element addrElement = (Element) xpath.evaluate("hl7:addr", locationElement, XPathConstants.NODE);
        if (addrElement != null) {
            Map<String, Object> address = extractAddress(addrElement, xpath);
            location.put("address", address);
        }

        // Location telecom
        Element telecomElement = (Element) xpath.evaluate("hl7:telecom", locationElement, XPathConstants.NODE);
        if (telecomElement != null) {
            location.put("telecom", telecomElement.getAttribute("value"));
        }

        return location;
    }

    private Map<String, Object> extractAddress(Element addrElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> address = new HashMap<>();

        NodeList streetLines = (NodeList) xpath.evaluate("hl7:streetAddressLine", addrElement, XPathConstants.NODESET);
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

    private Map<String, Object> extractDiagnosis(Element diagnosisElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> diagnosis = new HashMap<>();

        // Problem observation within the diagnosis act
        Element problemObsElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation/hl7:value",
            diagnosisElement, XPathConstants.NODE);

        if (problemObsElement != null) {
            Map<String, String> code = new HashMap<>();
            code.put("code", problemObsElement.getAttribute("code"));
            code.put("codeSystem", problemObsElement.getAttribute("codeSystem"));
            code.put("codeSystemName", problemObsElement.getAttribute("codeSystemName"));
            code.put("displayName", problemObsElement.getAttribute("displayName"));
            diagnosis.put("condition", code);

            // Map code system
            String codeSystemOid = problemObsElement.getAttribute("codeSystem");
            diagnosis.put("codeSystemUri", mapCodeSystemToUri(codeSystemOid));
        }

        // Diagnosis priority
        Element priorityElement = (Element) xpath.evaluate(
            "hl7:priorityCode", diagnosisElement, XPathConstants.NODE);
        if (priorityElement != null) {
            diagnosis.put("rank", priorityElement.getAttribute("value"));
        }

        return diagnosis;
    }

    private String mapStatusToFhir(String cdaStatus) {
        if (cdaStatus == null) return "unknown";

        return switch (cdaStatus.toLowerCase()) {
            case "active" -> "in-progress";
            case "completed" -> "finished";
            case "aborted" -> "cancelled";
            case "cancelled" -> "cancelled";
            default -> "unknown";
        };
    }

    private String mapPerformerType(String typeCode) {
        return switch (typeCode) {
            case "PRF" -> "practitioner";
            case "PPRF" -> "primary-performer";
            case "SPRF" -> "secondary-performer";
            case "ATND" -> "attender";
            case "ADM" -> "admitter";
            case "DIS" -> "discharger";
            case "REF" -> "referrer";
            case "CON" -> "consultant";
            default -> "participant";
        };
    }

    private String determineEncounterClass(Map<String, Object> encounter) {
        @SuppressWarnings("unchecked")
        Map<String, String> type = (Map<String, String>) encounter.get("type");
        if (type == null) return null;

        String code = type.get("code");
        String displayName = type.get("displayName");

        if (code == null && displayName == null) return null;

        // Try to determine from code or display name
        String combined = (code != null ? code : "") + " " + (displayName != null ? displayName.toLowerCase() : "");

        if (combined.contains("inpatient") || combined.contains("IP") || combined.contains("hosp")) {
            return "IMP"; // Inpatient
        } else if (combined.contains("emergency") || combined.contains("ER") || combined.contains("ED")) {
            return "EMER"; // Emergency
        } else if (combined.contains("outpatient") || combined.contains("AMB") || combined.contains("office")) {
            return "AMB"; // Ambulatory
        } else if (combined.contains("home") || combined.contains("HH")) {
            return "HH"; // Home Health
        } else if (combined.contains("virtual") || combined.contains("tele")) {
            return "VR"; // Virtual
        }

        return "AMB"; // Default to ambulatory
    }

    private String mapCodeSystemToUri(String codeSystemOid) {
        if (codeSystemOid == null) return null;

        return switch (codeSystemOid) {
            case "2.16.840.1.113883.6.96" -> "http://snomed.info/sct";
            case "2.16.840.1.113883.6.12" -> "http://www.ama-assn.org/go/cpt";
            case "2.16.840.1.113883.6.90" -> "http://hl7.org/fhir/sid/icd-10-cm";
            default -> "urn:oid:" + codeSystemOid;
        };
    }
}

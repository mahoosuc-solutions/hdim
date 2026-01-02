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
 * Handler for extracting allergies from CDA Allergies Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.6.1
 */
@Slf4j
@Component
public class CdaAllergyHandler {

    // Allergies Section Template OID
    private static final String ALLERGIES_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.6.1";

    // Allergy Concern Act Template OID
    private static final String ALLERGY_CONCERN_TEMPLATE = "2.16.840.1.113883.10.20.22.4.30";

    // Allergy Observation Template OID
    private static final String ALLERGY_OBSERVATION_TEMPLATE = "2.16.840.1.113883.10.20.22.4.7";

    /**
     * Extract allergies from CDA Allergies Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of allergy maps
     */
    public List<Map<String, Object>> extractAllergies(Document doc, XPath xpath) {
        List<Map<String, Object>> allergies = new ArrayList<>();

        try {
            // Find allergies section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", ALLERGIES_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Allergies section found in document");
                return allergies;
            }

            // Find all allergy concern acts
            NodeList entryNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:act[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.30']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element actElement = (Element) entryNodes.item(i);
                Map<String, Object> allergy = extractAllergyConcern(actElement, xpath);
                if (!allergy.isEmpty()) {
                    allergies.add(allergy);
                }
            }

            log.debug("Extracted {} allergies from document", allergies.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting allergies: {}", e.getMessage(), e);
        }

        return allergies;
    }

    private Map<String, Object> extractAllergyConcern(Element actElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> allergy = new HashMap<>();

        // Allergy ID
        Element idElement = (Element) xpath.evaluate("hl7:id", actElement, XPathConstants.NODE);
        if (idElement != null) {
            allergy.put("id", idElement.getAttribute("root"));
            allergy.put("extension", idElement.getAttribute("extension"));
        }

        // Status code
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", actElement, XPathConstants.NODE);
        if (statusElement != null) {
            allergy.put("concernStatus", statusElement.getAttribute("code"));
        }

        // Effective time
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", actElement, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            Element lowElement = (Element) xpath.evaluate("hl7:low", effectiveTimeElement, XPathConstants.NODE);
            Element highElement = (Element) xpath.evaluate("hl7:high", effectiveTimeElement, XPathConstants.NODE);

            if (lowElement != null) {
                allergy.put("onsetDate", lowElement.getAttribute("value"));
            }
            if (highElement != null) {
                allergy.put("resolutionDate", highElement.getAttribute("value"));
            }
        }

        // Extract allergy observation
        Element observationElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.7']]",
            actElement, XPathConstants.NODE);

        if (observationElement != null) {
            extractAllergyObservation(observationElement, xpath, allergy);
        }

        return allergy;
    }

    private void extractAllergyObservation(Element observationElement, XPath xpath, Map<String, Object> allergy)
            throws XPathExpressionException {

        // Allergy type (drug allergy, food allergy, etc.)
        Element valueElement = (Element) xpath.evaluate("hl7:value", observationElement, XPathConstants.NODE);
        if (valueElement != null) {
            Map<String, String> allergyType = new HashMap<>();
            allergyType.put("code", valueElement.getAttribute("code"));
            allergyType.put("displayName", valueElement.getAttribute("displayName"));
            allergyType.put("codeSystem", valueElement.getAttribute("codeSystem"));
            allergy.put("allergyType", allergyType);
        }

        // Allergy status observation
        Element statusObsElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.28']]/hl7:value",
            observationElement, XPathConstants.NODE);

        if (statusObsElement != null) {
            allergy.put("clinicalStatus", statusObsElement.getAttribute("code"));
            allergy.put("clinicalStatusDisplay", statusObsElement.getAttribute("displayName"));
        }

        // Allergen (participant)
        Element allergenElement = (Element) xpath.evaluate(
            "hl7:participant[@typeCode='CSM']/hl7:participantRole/hl7:playingEntity/hl7:code",
            observationElement, XPathConstants.NODE);

        if (allergenElement != null) {
            Map<String, String> allergen = new HashMap<>();
            allergen.put("code", allergenElement.getAttribute("code"));
            allergen.put("displayName", allergenElement.getAttribute("displayName"));
            allergen.put("codeSystem", allergenElement.getAttribute("codeSystem"));
            allergen.put("codeSystemName", allergenElement.getAttribute("codeSystemName"));
            allergy.put("allergen", allergen);

            // Map code system
            String codeSystemOid = allergenElement.getAttribute("codeSystem");
            allergy.put("allergenCodeSystemUri", mapCodeSystemToUri(codeSystemOid));
        }

        // Reactions
        NodeList reactionNodes = (NodeList) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='MFST']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.9']]",
            observationElement, XPathConstants.NODESET);

        List<Map<String, Object>> reactions = new ArrayList<>();
        for (int i = 0; i < reactionNodes.getLength(); i++) {
            Element reactionElement = (Element) reactionNodes.item(i);
            Map<String, Object> reaction = extractReaction(reactionElement, xpath);
            if (!reaction.isEmpty()) {
                reactions.add(reaction);
            }
        }
        if (!reactions.isEmpty()) {
            allergy.put("reactions", reactions);
        }

        // Severity (overall)
        Element severityElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.8']]/hl7:value",
            observationElement, XPathConstants.NODE);

        if (severityElement != null) {
            allergy.put("severity", severityElement.getAttribute("code"));
            allergy.put("severityDisplay", severityElement.getAttribute("displayName"));
        }

        // Criticality
        Element criticalityElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.145']]/hl7:value",
            observationElement, XPathConstants.NODE);

        if (criticalityElement != null) {
            allergy.put("criticality", criticalityElement.getAttribute("code"));
            allergy.put("criticalityDisplay", criticalityElement.getAttribute("displayName"));
        }
    }

    private Map<String, Object> extractReaction(Element reactionElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> reaction = new HashMap<>();

        // Reaction code
        Element valueElement = (Element) xpath.evaluate("hl7:value", reactionElement, XPathConstants.NODE);
        if (valueElement != null) {
            Map<String, String> manifestation = new HashMap<>();
            manifestation.put("code", valueElement.getAttribute("code"));
            manifestation.put("displayName", valueElement.getAttribute("displayName"));
            manifestation.put("codeSystem", valueElement.getAttribute("codeSystem"));
            reaction.put("manifestation", manifestation);
        }

        // Reaction effective time
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", reactionElement, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            Element lowElement = (Element) xpath.evaluate("hl7:low", effectiveTimeElement, XPathConstants.NODE);
            if (lowElement != null) {
                reaction.put("onsetDate", lowElement.getAttribute("value"));
            }
        }

        // Reaction severity
        Element severityElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.8']]/hl7:value",
            reactionElement, XPathConstants.NODE);

        if (severityElement != null) {
            reaction.put("severity", severityElement.getAttribute("code"));
            reaction.put("severityDisplay", severityElement.getAttribute("displayName"));
        }

        return reaction;
    }

    private String mapCodeSystemToUri(String codeSystemOid) {
        if (codeSystemOid == null) return null;

        return switch (codeSystemOid) {
            case "2.16.840.1.113883.6.88" -> "http://www.nlm.nih.gov/research/umls/rxnorm";
            case "2.16.840.1.113883.6.69" -> "http://hl7.org/fhir/sid/ndc";
            case "2.16.840.1.113883.6.96" -> "http://snomed.info/sct";
            case "2.16.840.1.113883.3.26.1.5" -> "http://fdasis.nlm.nih.gov";
            default -> "urn:oid:" + codeSystemOid;
        };
    }
}

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
 * Handler for extracting problems/conditions from CDA Problems Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.5.1
 */
@Slf4j
@Component
public class CdaProblemHandler {

    // Problem Section Template OID
    private static final String PROBLEM_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.5.1";

    // Problem Concern Act Template OID
    private static final String PROBLEM_CONCERN_TEMPLATE = "2.16.840.1.113883.10.20.22.4.3";

    // Problem Observation Template OID
    private static final String PROBLEM_OBSERVATION_TEMPLATE = "2.16.840.1.113883.10.20.22.4.4";

    /**
     * Extract problems from CDA Problems Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of problem maps
     */
    public List<Map<String, Object>> extractProblems(Document doc, XPath xpath) {
        List<Map<String, Object>> problems = new ArrayList<>();

        try {
            // Find problem section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", PROBLEM_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Problems section found in document");
                return problems;
            }

            // Find all problem concern acts (entry/act)
            NodeList entryNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:act[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.3']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element actElement = (Element) entryNodes.item(i);
                Map<String, Object> problem = extractProblemConcern(actElement, xpath);
                if (!problem.isEmpty()) {
                    problems.add(problem);
                }
            }

            log.debug("Extracted {} problems from document", problems.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting problems: {}", e.getMessage(), e);
        }

        return problems;
    }

    private Map<String, Object> extractProblemConcern(Element actElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> problem = new HashMap<>();

        // Problem Concern ID
        Element idElement = (Element) xpath.evaluate("hl7:id", actElement, XPathConstants.NODE);
        if (idElement != null) {
            problem.put("id", idElement.getAttribute("root"));
            problem.put("extension", idElement.getAttribute("extension"));
        }

        // Status code (active, completed, aborted)
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", actElement, XPathConstants.NODE);
        if (statusElement != null) {
            problem.put("concernStatus", statusElement.getAttribute("code"));
        }

        // Effective time (date range of concern)
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", actElement, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            Element lowElement = (Element) xpath.evaluate("hl7:low", effectiveTimeElement, XPathConstants.NODE);
            Element highElement = (Element) xpath.evaluate("hl7:high", effectiveTimeElement, XPathConstants.NODE);

            if (lowElement != null) {
                problem.put("onsetDate", lowElement.getAttribute("value"));
            }
            if (highElement != null) {
                problem.put("abatementDate", highElement.getAttribute("value"));
            }
        }

        // Extract problem observation (entryRelationship/observation)
        Element observationElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.4']]",
            actElement, XPathConstants.NODE);

        if (observationElement != null) {
            extractProblemObservation(observationElement, xpath, problem);
        }

        return problem;
    }

    private void extractProblemObservation(Element observationElement, XPath xpath, Map<String, Object> problem)
            throws XPathExpressionException {

        // Problem code (ICD-10, SNOMED)
        Element codeElement = (Element) xpath.evaluate("hl7:value", observationElement, XPathConstants.NODE);
        if (codeElement != null) {
            Map<String, String> code = new HashMap<>();
            code.put("code", codeElement.getAttribute("code"));
            code.put("codeSystem", codeElement.getAttribute("codeSystem"));
            code.put("codeSystemName", codeElement.getAttribute("codeSystemName"));
            code.put("displayName", codeElement.getAttribute("displayName"));
            problem.put("code", code);

            // Map code system OID to URI
            String codeSystemOid = codeElement.getAttribute("codeSystem");
            problem.put("codeSystemUri", mapCodeSystemToUri(codeSystemOid));
        }

        // Problem status observation (active, inactive, resolved)
        Element statusObsElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='REFR']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.6']]/hl7:value",
            observationElement, XPathConstants.NODE);

        if (statusObsElement != null) {
            problem.put("clinicalStatus", statusObsElement.getAttribute("code"));
            problem.put("clinicalStatusDisplay", statusObsElement.getAttribute("displayName"));
        }

        // Severity
        Element severityElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.8']]/hl7:value",
            observationElement, XPathConstants.NODE);

        if (severityElement != null) {
            problem.put("severity", severityElement.getAttribute("code"));
            problem.put("severityDisplay", severityElement.getAttribute("displayName"));
        }

        // Age at onset
        Element ageElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='SUBJ']/hl7:observation[hl7:code[@code='445518008']]/hl7:value",
            observationElement, XPathConstants.NODE);

        if (ageElement != null) {
            problem.put("ageAtOnset", ageElement.getAttribute("value"));
            problem.put("ageAtOnsetUnit", ageElement.getAttribute("unit"));
        }

        // Problem effective time (different from concern effective time)
        Element obsEffectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", observationElement, XPathConstants.NODE);
        if (obsEffectiveTimeElement != null) {
            Element lowElement = (Element) xpath.evaluate("hl7:low", obsEffectiveTimeElement, XPathConstants.NODE);
            if (lowElement != null && problem.get("onsetDate") == null) {
                problem.put("onsetDate", lowElement.getAttribute("value"));
            }
        }

        // Author (who documented the problem)
        Element authorElement = (Element) xpath.evaluate("hl7:author", observationElement, XPathConstants.NODE);
        if (authorElement != null) {
            Map<String, Object> author = extractAuthor(authorElement, xpath);
            problem.put("author", author);
        }
    }

    private Map<String, Object> extractAuthor(Element authorElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> author = new HashMap<>();

        // Author time
        Element timeElement = (Element) xpath.evaluate("hl7:time", authorElement, XPathConstants.NODE);
        if (timeElement != null) {
            author.put("time", timeElement.getAttribute("value"));
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

        return author;
    }

    private String mapCodeSystemToUri(String codeSystemOid) {
        if (codeSystemOid == null) return null;

        return switch (codeSystemOid) {
            case "2.16.840.1.113883.6.96" -> "http://snomed.info/sct";
            case "2.16.840.1.113883.6.90" -> "http://hl7.org/fhir/sid/icd-10-cm";
            case "2.16.840.1.113883.6.103" -> "http://hl7.org/fhir/sid/icd-9-cm";
            case "2.16.840.1.113883.6.1" -> "http://loinc.org";
            default -> "urn:oid:" + codeSystemOid;
        };
    }
}

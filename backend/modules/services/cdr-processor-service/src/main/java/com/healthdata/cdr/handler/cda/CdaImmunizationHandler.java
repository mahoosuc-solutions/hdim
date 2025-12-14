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
 * Handler for extracting immunizations from CDA Immunizations Section.
 * C-CDA Template OID: 2.16.840.1.113883.10.20.22.2.2.1
 */
@Slf4j
@Component
public class CdaImmunizationHandler {

    // Immunizations Section Template OID
    private static final String IMMUNIZATIONS_SECTION_TEMPLATE = "2.16.840.1.113883.10.20.22.2.2.1";

    // Immunization Activity Template OID
    private static final String IMMUNIZATION_ACTIVITY_TEMPLATE = "2.16.840.1.113883.10.20.22.4.52";

    /**
     * Extract immunizations from CDA Immunizations Section.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return List of immunization maps
     */
    public List<Map<String, Object>> extractImmunizations(Document doc, XPath xpath) {
        List<Map<String, Object>> immunizations = new ArrayList<>();

        try {
            // Find immunizations section by template ID
            String sectionXpath = String.format(
                "//hl7:section[hl7:templateId[@root='%s']]", IMMUNIZATIONS_SECTION_TEMPLATE);

            Element section = (Element) xpath.evaluate(sectionXpath, doc, XPathConstants.NODE);

            if (section == null) {
                log.debug("No Immunizations section found in document");
                return immunizations;
            }

            // Find all immunization activities
            NodeList entryNodes = (NodeList) xpath.evaluate(
                "hl7:entry/hl7:substanceAdministration[hl7:templateId[@root='2.16.840.1.113883.10.20.22.4.52']]",
                section, XPathConstants.NODESET);

            for (int i = 0; i < entryNodes.getLength(); i++) {
                Element substanceAdminElement = (Element) entryNodes.item(i);
                Map<String, Object> immunization = extractImmunizationActivity(substanceAdminElement, xpath);
                if (!immunization.isEmpty()) {
                    immunizations.add(immunization);
                }
            }

            log.debug("Extracted {} immunizations from document", immunizations.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting immunizations: {}", e.getMessage(), e);
        }

        return immunizations;
    }

    private Map<String, Object> extractImmunizationActivity(Element substanceAdminElement, XPath xpath)
            throws XPathExpressionException {
        Map<String, Object> immunization = new HashMap<>();

        // Immunization ID
        Element idElement = (Element) xpath.evaluate("hl7:id", substanceAdminElement, XPathConstants.NODE);
        if (idElement != null) {
            immunization.put("id", idElement.getAttribute("root"));
            immunization.put("extension", idElement.getAttribute("extension"));
        }

        // Status code (completed, not done)
        Element statusElement = (Element) xpath.evaluate("hl7:statusCode", substanceAdminElement, XPathConstants.NODE);
        if (statusElement != null) {
            immunization.put("status", statusElement.getAttribute("code"));
        }

        // Negation indicator (if vaccine was refused/not given)
        String negationInd = substanceAdminElement.getAttribute("negationInd");
        if ("true".equalsIgnoreCase(negationInd)) {
            immunization.put("notGiven", true);
        }

        // Effective time (administration date)
        Element effectiveTimeElement = (Element) xpath.evaluate("hl7:effectiveTime", substanceAdminElement, XPathConstants.NODE);
        if (effectiveTimeElement != null) {
            String value = effectiveTimeElement.getAttribute("value");
            if (value != null && !value.isEmpty()) {
                immunization.put("administrationDate", value);
            } else {
                // Check for low element
                Element lowElement = (Element) xpath.evaluate("hl7:low", effectiveTimeElement, XPathConstants.NODE);
                if (lowElement != null) {
                    immunization.put("administrationDate", lowElement.getAttribute("value"));
                }
            }
        }

        // Route of administration
        Element routeElement = (Element) xpath.evaluate("hl7:routeCode", substanceAdminElement, XPathConstants.NODE);
        if (routeElement != null) {
            Map<String, String> route = new HashMap<>();
            route.put("code", routeElement.getAttribute("code"));
            route.put("displayName", routeElement.getAttribute("displayName"));
            route.put("codeSystem", routeElement.getAttribute("codeSystem"));
            immunization.put("route", route);
        }

        // Site of administration
        Element siteElement = (Element) xpath.evaluate("hl7:approachSiteCode", substanceAdminElement, XPathConstants.NODE);
        if (siteElement != null) {
            Map<String, String> site = new HashMap<>();
            site.put("code", siteElement.getAttribute("code"));
            site.put("displayName", siteElement.getAttribute("displayName"));
            immunization.put("site", site);
        }

        // Dose quantity
        Element doseElement = (Element) xpath.evaluate("hl7:doseQuantity", substanceAdminElement, XPathConstants.NODE);
        if (doseElement != null) {
            Map<String, String> dose = new HashMap<>();
            dose.put("value", doseElement.getAttribute("value"));
            dose.put("unit", doseElement.getAttribute("unit"));
            immunization.put("doseQuantity", dose);
        }

        // Vaccine (consumable)
        Element vaccineElement = (Element) xpath.evaluate(
            "hl7:consumable/hl7:manufacturedProduct/hl7:manufacturedMaterial",
            substanceAdminElement, XPathConstants.NODE);

        if (vaccineElement != null) {
            extractVaccineInfo(vaccineElement, xpath, immunization);
        }

        // Performer (who administered)
        Element performerElement = (Element) xpath.evaluate("hl7:performer", substanceAdminElement, XPathConstants.NODE);
        if (performerElement != null) {
            Map<String, Object> performer = extractPerformer(performerElement, xpath);
            immunization.put("performer", performer);
        }

        // Refusal reason (if not given)
        Element refusalReasonElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='RSON']/hl7:observation/hl7:code",
            substanceAdminElement, XPathConstants.NODE);

        if (refusalReasonElement != null) {
            Map<String, String> refusalReason = new HashMap<>();
            refusalReason.put("code", refusalReasonElement.getAttribute("code"));
            refusalReason.put("displayName", refusalReasonElement.getAttribute("displayName"));
            immunization.put("refusalReason", refusalReason);
        }

        // Reaction observation
        Element reactionElement = (Element) xpath.evaluate(
            "hl7:entryRelationship[@typeCode='CAUS']/hl7:observation/hl7:value",
            substanceAdminElement, XPathConstants.NODE);

        if (reactionElement != null) {
            Map<String, String> reaction = new HashMap<>();
            reaction.put("code", reactionElement.getAttribute("code"));
            reaction.put("displayName", reactionElement.getAttribute("displayName"));
            immunization.put("reaction", reaction);
        }

        // Immunization information source
        Element informantElement = (Element) xpath.evaluate("hl7:informant", substanceAdminElement, XPathConstants.NODE);
        if (informantElement != null) {
            immunization.put("primarySource", false);
            String orgName = xpath.evaluate(
                "hl7:assignedEntity/hl7:representedOrganization/hl7:name/text()", informantElement);
            if (orgName != null && !orgName.isEmpty()) {
                immunization.put("informationSource", orgName);
            }
        } else {
            immunization.put("primarySource", true);
        }

        return immunization;
    }

    private void extractVaccineInfo(Element materialElement, XPath xpath, Map<String, Object> immunization)
            throws XPathExpressionException {

        // Vaccine code (CVX)
        Element codeElement = (Element) xpath.evaluate("hl7:code", materialElement, XPathConstants.NODE);
        if (codeElement != null) {
            Map<String, String> vaccineCode = new HashMap<>();
            vaccineCode.put("code", codeElement.getAttribute("code"));
            vaccineCode.put("codeSystem", codeElement.getAttribute("codeSystem"));
            vaccineCode.put("codeSystemName", codeElement.getAttribute("codeSystemName"));
            vaccineCode.put("displayName", codeElement.getAttribute("displayName"));
            immunization.put("vaccineCode", vaccineCode);

            // Map code system to URI
            String codeSystemOid = codeElement.getAttribute("codeSystem");
            immunization.put("vaccineCodeSystemUri", mapCodeSystemToUri(codeSystemOid));

            // Translation codes (NDC)
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
                immunization.put("vaccineTranslations", translations);
            }
        }

        // Lot number
        Element lotElement = (Element) xpath.evaluate("hl7:lotNumberText", materialElement, XPathConstants.NODE);
        if (lotElement != null) {
            immunization.put("lotNumber", lotElement.getTextContent());
        }

        // Manufacturer
        Element manufacturerElement = (Element) xpath.evaluate(
            "../hl7:manufacturerOrganization/hl7:name", materialElement, XPathConstants.NODE);
        if (manufacturerElement != null) {
            immunization.put("manufacturer", manufacturerElement.getTextContent());
        }
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
            case "2.16.840.1.113883.12.292" -> "http://hl7.org/fhir/sid/cvx";
            case "2.16.840.1.113883.6.69" -> "http://hl7.org/fhir/sid/ndc";
            case "2.16.840.1.113883.12.227" -> "http://hl7.org/fhir/sid/mvx";
            default -> "urn:oid:" + codeSystemOid;
        };
    }
}

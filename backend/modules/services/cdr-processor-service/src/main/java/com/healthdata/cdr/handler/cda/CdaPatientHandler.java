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
 * Handler for extracting patient demographics from CDA recordTarget.
 */
@Slf4j
@Component
public class CdaPatientHandler {

    private static final String NS_PREFIX = "hl7:";

    /**
     * Extract patient data from CDA recordTarget element.
     *
     * @param doc   XML document
     * @param xpath XPath evaluator with namespace context
     * @return Map containing patient demographics
     */
    public Map<String, Object> extractPatient(Document doc, XPath xpath) {
        Map<String, Object> patient = new HashMap<>();

        try {
            // Patient identifiers
            NodeList idNodes = (NodeList) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:id",
                doc, XPathConstants.NODESET);

            List<Map<String, String>> identifiers = new ArrayList<>();
            for (int i = 0; i < idNodes.getLength(); i++) {
                Element idElement = (Element) idNodes.item(i);
                Map<String, String> identifier = new HashMap<>();
                identifier.put("root", idElement.getAttribute("root"));
                identifier.put("extension", idElement.getAttribute("extension"));
                identifier.put("assigningAuthorityName", idElement.getAttribute("assigningAuthorityName"));
                identifiers.add(identifier);
            }
            patient.put("identifiers", identifiers);

            // Patient name
            Element nameElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:name",
                doc, XPathConstants.NODE);

            if (nameElement != null) {
                Map<String, Object> name = extractName(nameElement, xpath);
                patient.put("name", name);
            }

            // Gender (administrativeGenderCode)
            Element genderElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:administrativeGenderCode",
                doc, XPathConstants.NODE);

            if (genderElement != null) {
                patient.put("gender", genderElement.getAttribute("code"));
                patient.put("genderDisplay", genderElement.getAttribute("displayName"));
            }

            // Birth time
            Element birthTimeElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:birthTime",
                doc, XPathConstants.NODE);

            if (birthTimeElement != null) {
                patient.put("birthTime", birthTimeElement.getAttribute("value"));
            }

            // Marital status
            Element maritalElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:maritalStatusCode",
                doc, XPathConstants.NODE);

            if (maritalElement != null) {
                patient.put("maritalStatus", maritalElement.getAttribute("code"));
                patient.put("maritalStatusDisplay", maritalElement.getAttribute("displayName"));
            }

            // Race (SDTC extension)
            Element raceElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/sdtc:raceCode",
                doc, XPathConstants.NODE);

            if (raceElement != null) {
                patient.put("race", raceElement.getAttribute("code"));
                patient.put("raceDisplay", raceElement.getAttribute("displayName"));
            }

            // Ethnicity (SDTC extension)
            Element ethnicElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/sdtc:ethnicGroupCode",
                doc, XPathConstants.NODE);

            if (ethnicElement != null) {
                patient.put("ethnicity", ethnicElement.getAttribute("code"));
                patient.put("ethnicityDisplay", ethnicElement.getAttribute("displayName"));
            }

            // Religious affiliation
            Element religionElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:religiousAffiliationCode",
                doc, XPathConstants.NODE);

            if (religionElement != null) {
                patient.put("religion", religionElement.getAttribute("code"));
                patient.put("religionDisplay", religionElement.getAttribute("displayName"));
            }

            // Language
            Element langElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:languageCommunication/hl7:languageCode",
                doc, XPathConstants.NODE);

            if (langElement != null) {
                patient.put("language", langElement.getAttribute("code"));
            }

            // Address
            NodeList addrNodes = (NodeList) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:addr",
                doc, XPathConstants.NODESET);

            List<Map<String, Object>> addresses = new ArrayList<>();
            for (int i = 0; i < addrNodes.getLength(); i++) {
                Element addrElement = (Element) addrNodes.item(i);
                Map<String, Object> address = extractAddress(addrElement, xpath);
                addresses.add(address);
            }
            patient.put("addresses", addresses);

            // Telecom (phone, email)
            NodeList telecomNodes = (NodeList) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:telecom",
                doc, XPathConstants.NODESET);

            List<Map<String, String>> telecoms = new ArrayList<>();
            for (int i = 0; i < telecomNodes.getLength(); i++) {
                Element telecomElement = (Element) telecomNodes.item(i);
                Map<String, String> telecom = new HashMap<>();
                telecom.put("value", telecomElement.getAttribute("value"));
                telecom.put("use", telecomElement.getAttribute("use"));
                telecoms.add(telecom);
            }
            patient.put("telecoms", telecoms);

            // Guardian (if present)
            Element guardianElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:patient/hl7:guardian",
                doc, XPathConstants.NODE);

            if (guardianElement != null) {
                Map<String, Object> guardian = extractGuardian(guardianElement, xpath);
                patient.put("guardian", guardian);
            }

            // Provider organization
            Element providerOrgElement = (Element) xpath.evaluate(
                "//hl7:recordTarget/hl7:patientRole/hl7:providerOrganization",
                doc, XPathConstants.NODE);

            if (providerOrgElement != null) {
                Map<String, Object> providerOrg = extractOrganization(providerOrgElement, xpath);
                patient.put("providerOrganization", providerOrg);
            }

            log.debug("Extracted patient data with {} identifiers", identifiers.size());

        } catch (XPathExpressionException e) {
            log.error("Error extracting patient data: {}", e.getMessage(), e);
        }

        return patient;
    }

    private Map<String, Object> extractName(Element nameElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> name = new HashMap<>();

        // Family name
        String family = xpath.evaluate("hl7:family/text()", nameElement);
        if (family != null && !family.isEmpty()) {
            name.put("family", family);
        }

        // Given names
        NodeList givenNodes = (NodeList) xpath.evaluate("hl7:given", nameElement, XPathConstants.NODESET);
        List<String> givenNames = new ArrayList<>();
        for (int i = 0; i < givenNodes.getLength(); i++) {
            String given = givenNodes.item(i).getTextContent();
            if (given != null && !given.isEmpty()) {
                givenNames.add(given);
            }
        }
        name.put("given", givenNames);

        // Prefix
        String prefix = xpath.evaluate("hl7:prefix/text()", nameElement);
        if (prefix != null && !prefix.isEmpty()) {
            name.put("prefix", prefix);
        }

        // Suffix
        String suffix = xpath.evaluate("hl7:suffix/text()", nameElement);
        if (suffix != null && !suffix.isEmpty()) {
            name.put("suffix", suffix);
        }

        // Use attribute
        String use = ((Element) nameElement).getAttribute("use");
        if (use != null && !use.isEmpty()) {
            name.put("use", use);
        }

        return name;
    }

    private Map<String, Object> extractAddress(Element addrElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> address = new HashMap<>();

        // Street address lines
        NodeList streetLines = (NodeList) xpath.evaluate("hl7:streetAddressLine", addrElement, XPathConstants.NODESET);
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < streetLines.getLength(); i++) {
            String line = streetLines.item(i).getTextContent();
            if (line != null && !line.isEmpty()) {
                lines.add(line);
            }
        }
        address.put("streetAddressLines", lines);

        // City
        String city = xpath.evaluate("hl7:city/text()", addrElement);
        if (city != null && !city.isEmpty()) {
            address.put("city", city);
        }

        // State
        String state = xpath.evaluate("hl7:state/text()", addrElement);
        if (state != null && !state.isEmpty()) {
            address.put("state", state);
        }

        // Postal code
        String postalCode = xpath.evaluate("hl7:postalCode/text()", addrElement);
        if (postalCode != null && !postalCode.isEmpty()) {
            address.put("postalCode", postalCode);
        }

        // Country
        String country = xpath.evaluate("hl7:country/text()", addrElement);
        if (country != null && !country.isEmpty()) {
            address.put("country", country);
        }

        // Use attribute
        String use = addrElement.getAttribute("use");
        if (use != null && !use.isEmpty()) {
            address.put("use", use);
        }

        return address;
    }

    private Map<String, Object> extractGuardian(Element guardianElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> guardian = new HashMap<>();

        // Guardian code (relationship)
        Element codeElement = (Element) xpath.evaluate("hl7:code", guardianElement, XPathConstants.NODE);
        if (codeElement != null) {
            guardian.put("relationshipCode", codeElement.getAttribute("code"));
            guardian.put("relationshipDisplay", codeElement.getAttribute("displayName"));
        }

        // Guardian name
        Element nameElement = (Element) xpath.evaluate("hl7:guardianPerson/hl7:name", guardianElement, XPathConstants.NODE);
        if (nameElement != null) {
            guardian.put("name", extractName(nameElement, xpath));
        }

        // Guardian address
        Element addrElement = (Element) xpath.evaluate("hl7:addr", guardianElement, XPathConstants.NODE);
        if (addrElement != null) {
            guardian.put("address", extractAddress(addrElement, xpath));
        }

        // Guardian telecom
        Element telecomElement = (Element) xpath.evaluate("hl7:telecom", guardianElement, XPathConstants.NODE);
        if (telecomElement != null) {
            Map<String, String> telecom = new HashMap<>();
            telecom.put("value", telecomElement.getAttribute("value"));
            telecom.put("use", telecomElement.getAttribute("use"));
            guardian.put("telecom", telecom);
        }

        return guardian;
    }

    private Map<String, Object> extractOrganization(Element orgElement, XPath xpath) throws XPathExpressionException {
        Map<String, Object> org = new HashMap<>();

        // Organization ID
        Element idElement = (Element) xpath.evaluate("hl7:id", orgElement, XPathConstants.NODE);
        if (idElement != null) {
            org.put("id", idElement.getAttribute("root"));
            org.put("extension", idElement.getAttribute("extension"));
        }

        // Organization name
        String name = xpath.evaluate("hl7:name/text()", orgElement);
        if (name != null && !name.isEmpty()) {
            org.put("name", name);
        }

        // Organization telecom
        Element telecomElement = (Element) xpath.evaluate("hl7:telecom", orgElement, XPathConstants.NODE);
        if (telecomElement != null) {
            org.put("telecom", telecomElement.getAttribute("value"));
        }

        // Organization address
        Element addrElement = (Element) xpath.evaluate("hl7:addr", orgElement, XPathConstants.NODE);
        if (addrElement != null) {
            org.put("address", extractAddress(addrElement, xpath));
        }

        return org;
    }
}

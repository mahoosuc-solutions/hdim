package com.healthdata.cms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DPC Patient Response DTO
 *
 * Represents a FHIR Patient resource from DPC API.
 * Contains Medicare beneficiary demographics and coverage information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DpcPatientResponse {

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("id")
    private String id;

    @JsonProperty("identifier")
    private java.util.List<Identifier> identifier;

    @JsonProperty("name")
    private java.util.List<HumanName> name;

    @JsonProperty("birthDate")
    private String birthDate;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("address")
    private java.util.List<Address> address;

    @JsonProperty("telecom")
    private java.util.List<Telecom> telecom;

    @JsonProperty("communication")
    private java.util.List<Communication> communication;

    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("extension")
    private java.util.List<Extension> extension;

    /**
     * Get the Medicare beneficiary ID (MBI)
     */
    public String getMedicareId() {
        if (identifier != null) {
            return identifier.stream()
                .filter(id -> "http://hl7.org/fhir/sid/us-mbi".equals(id.system))
                .findFirst()
                .map(Identifier::getValue)
                .orElse(null);
        }
        return null;
    }

    /**
     * Get the patient's full name
     */
    public String getFullName() {
        if (name != null && !name.isEmpty()) {
            HumanName firstName = name.get(0);
            if (firstName.getText() != null) {
                return firstName.getText();
            }
            if (firstName.getFamily() != null && firstName.getGiven() != null && !firstName.getGiven().isEmpty()) {
                return firstName.getGiven().get(0) + " " + firstName.getFamily();
            }
        }
        return null;
    }

    /**
     * Check if patient is valid (has required fields)
     */
    public boolean isValid() {
        return resourceType != null && "Patient".equals(resourceType)
            && id != null && !id.isEmpty()
            && identifier != null && !identifier.isEmpty();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Identifier {
        @JsonProperty("system")
        private String system;

        @JsonProperty("value")
        private String value;

        @JsonProperty("type")
        private CodeableConcept type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HumanName {
        @JsonProperty("use")
        private String use;

        @JsonProperty("text")
        private String text;

        @JsonProperty("family")
        private String family;

        @JsonProperty("given")
        private java.util.List<String> given;

        @JsonProperty("prefix")
        private java.util.List<String> prefix;

        @JsonProperty("suffix")
        private java.util.List<String> suffix;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Address {
        @JsonProperty("use")
        private String use;

        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;

        @JsonProperty("line")
        private java.util.List<String> line;

        @JsonProperty("city")
        private String city;

        @JsonProperty("state")
        private String state;

        @JsonProperty("postalCode")
        private String postalCode;

        @JsonProperty("country")
        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Telecom {
        @JsonProperty("system")
        private String system;

        @JsonProperty("value")
        private String value;

        @JsonProperty("use")
        private String use;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Communication {
        @JsonProperty("language")
        private CodeableConcept language;

        @JsonProperty("preferred")
        private Boolean preferred;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Meta {
        @JsonProperty("versionId")
        private String versionId;

        @JsonProperty("lastUpdated")
        private String lastUpdated;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Extension {
        @JsonProperty("url")
        private String url;

        @JsonProperty("valueString")
        private String valueString;

        @JsonProperty("valueCodeableConcept")
        private CodeableConcept valueCodeableConcept;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CodeableConcept {
        @JsonProperty("coding")
        private java.util.List<Coding> coding;

        @JsonProperty("text")
        private String text;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coding {
        @JsonProperty("system")
        private String system;

        @JsonProperty("code")
        private String code;

        @JsonProperty("display")
        private String display;
    }
}

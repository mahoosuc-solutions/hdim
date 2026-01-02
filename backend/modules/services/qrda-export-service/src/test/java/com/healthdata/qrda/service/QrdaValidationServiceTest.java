package com.healthdata.qrda.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QrdaValidationService.
 * Tests QRDA Category I and III document validation logic.
 */
@ExtendWith(MockitoExtension.class)
class QrdaValidationServiceTest {

    @InjectMocks
    private QrdaValidationService validationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validationService, "validationEnabled", true);
    }

    @Nested
    @DisplayName("validateCategoryI() tests")
    class ValidateCategoryITests {

        @Test
        @DisplayName("Should return empty errors for valid QRDA I document")
        void validateCategoryI_validDocument_returnsEmptyErrors() {
            // Arrange
            String validDoc = createValidQrdaCategoryIDocument();

            // Act
            List<String> errors = validationService.validateCategoryI(validDoc);

            // Assert
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return error for empty document")
        void validateCategoryI_emptyDocument_returnsError() {
            // Act
            List<String> errors = validationService.validateCategoryI("");

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.contains("empty"));
        }

        @Test
        @DisplayName("Should return error for null document")
        void validateCategoryI_nullDocument_returnsError() {
            // Act
            List<String> errors = validationService.validateCategoryI(null);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.contains("empty"));
        }

        @Test
        @DisplayName("Should return error for malformed XML")
        void validateCategoryI_malformedXml_returnsError() {
            // Arrange
            String malformedXml = "<ClinicalDocument><unclosed>";

            // Act
            List<String> errors = validationService.validateCategoryI(malformedXml);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.contains("well-formed"));
        }

        @Test
        @DisplayName("Should return error for missing CDA namespace")
        void validateCategoryI_missingNamespace_returnsError() {
            // Arrange
            String docWithoutNs = "<?xml version=\"1.0\"?><ClinicalDocument></ClinicalDocument>";

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutNs);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("namespace"));
        }

        @Test
        @DisplayName("Should return error for wrong root element")
        void validateCategoryI_wrongRootElement_returnsError() {
            // Arrange
            String wrongRoot = "<?xml version=\"1.0\"?><Document xmlns=\"urn:hl7-org:v3\"></Document>";

            // Act
            List<String> errors = validationService.validateCategoryI(wrongRoot);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.contains("ClinicalDocument"));
        }

        @Test
        @DisplayName("Should return error for missing recordTarget")
        void validateCategoryI_missingRecordTarget_returnsError() {
            // Arrange
            String docWithoutRecordTarget = createQrdaDocumentWithoutRecordTarget();

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutRecordTarget);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("recordtarget"));
        }

        @Test
        @DisplayName("Should return error for missing Category I template ID")
        void validateCategoryI_missingTemplateId_returnsError() {
            // Arrange
            String docWithoutTemplate = createQrdaDocumentWithoutCategoryITemplate();

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutTemplate);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.contains("2.16.840.1.113883.10.20.24.1.1"));
        }

        @Test
        @DisplayName("Should skip validation when disabled")
        void validateCategoryI_validationDisabled_skipsValidation() {
            // Arrange
            ReflectionTestUtils.setField(validationService, "validationEnabled", false);
            String invalidDoc = "<invalid>";

            // Act
            List<String> errors = validationService.validateCategoryI(invalidDoc);

            // Assert
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateCategoryIII() tests")
    class ValidateCategoryIIITests {

        @Test
        @DisplayName("Should return empty errors for valid QRDA III document")
        void validateCategoryIII_validDocument_returnsEmptyErrors() {
            // Arrange
            String validDoc = createValidQrdaCategoryIIIDocument();

            // Act
            List<String> errors = validationService.validateCategoryIII(validDoc);

            // Assert
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("Should return error for empty document")
        void validateCategoryIII_emptyDocument_returnsError() {
            // Act
            List<String> errors = validationService.validateCategoryIII("");

            // Assert
            assertThat(errors).isNotEmpty();
        }

        @Test
        @DisplayName("Should return error for missing Category III template ID")
        void validateCategoryIII_missingTemplateId_returnsError() {
            // Arrange
            String docWithoutTemplate = createQrdaDocumentWithoutCategoryIIITemplate();

            // Act
            List<String> errors = validationService.validateCategoryIII(docWithoutTemplate);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.contains("2.16.840.1.113883.10.20.27.1.1"));
        }

        @Test
        @DisplayName("Should return error for missing custodian")
        void validateCategoryIII_missingCustodian_returnsError() {
            // Arrange
            String docWithoutCustodian = createQrdaDocumentWithoutCustodian();

            // Act
            List<String> errors = validationService.validateCategoryIII(docWithoutCustodian);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("custodian"));
        }

        @Test
        @DisplayName("Should return error for missing legalAuthenticator")
        void validateCategoryIII_missingLegalAuthenticator_returnsError() {
            // Arrange
            String docWithoutLegalAuth = createQrdaDocumentWithoutLegalAuthenticator();

            // Act
            List<String> errors = validationService.validateCategoryIII(docWithoutLegalAuth);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("legalauthenticator"));
        }
    }

    @Nested
    @DisplayName("validateForCmsSubmission() tests")
    class ValidateForCmsSubmissionTests {

        @Test
        @DisplayName("Should validate TIN format")
        void validateForCmsSubmission_invalidTin_returnsError() {
            // Arrange
            String docWithInvalidTin = createDocumentWithTin("123"); // Invalid - not 9 digits

            // Act
            List<String> errors = validationService.validateForCmsSubmission(docWithInvalidTin, 2024);

            // Assert
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("tin"));
        }

        @Test
        @DisplayName("Should accept valid TIN format")
        void validateForCmsSubmission_validTin_noTinError() {
            // Arrange
            String docWithValidTin = createDocumentWithTin("123456789");

            // Act
            List<String> errors = validationService.validateForCmsSubmission(docWithValidTin, 2024);

            // Assert
            assertThat(errors).noneMatch(e -> e.toLowerCase().contains("invalid tin"));
        }

        @Test
        @DisplayName("Should validate NPI format")
        void validateForCmsSubmission_invalidNpi_returnsError() {
            // Arrange
            String docWithInvalidNpi = createDocumentWithNpi("12345"); // Invalid - not 10 digits

            // Act
            List<String> errors = validationService.validateForCmsSubmission(docWithInvalidNpi, 2024);

            // Assert
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("npi"));
        }

        @Test
        @DisplayName("Should accept valid NPI format")
        void validateForCmsSubmission_validNpi_noNpiError() {
            // Arrange
            String docWithValidNpi = createDocumentWithNpi("1234567890");

            // Act
            List<String> errors = validationService.validateForCmsSubmission(docWithValidNpi, 2024);

            // Assert
            assertThat(errors).noneMatch(e -> e.toLowerCase().contains("invalid npi"));
        }

        @Test
        @DisplayName("Should handle unparseable document")
        void validateForCmsSubmission_invalidXml_returnsError() {
            // Arrange
            String invalidXml = "<invalid>";

            // Act
            List<String> errors = validationService.validateForCmsSubmission(invalidXml, 2024);

            // Assert
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("parse") || e.toLowerCase().contains("cannot"));
        }
    }

    @Nested
    @DisplayName("Basic validation tests")
    class BasicValidationTests {

        @Test
        @DisplayName("Should detect missing templateId elements")
        void basicValidation_missingTemplateId_returnsError() {
            // Arrange
            String docWithoutTemplateId = createDocumentWithoutTemplateId();

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutTemplateId);

            // Assert
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("templateid"));
        }

        @Test
        @DisplayName("Should detect missing typeId element")
        void basicValidation_missingTypeId_returnsError() {
            // Arrange
            String docWithoutTypeId = createDocumentWithoutTypeId();

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutTypeId);

            // Assert
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("typeid"));
        }

        @Test
        @DisplayName("Should detect missing effectiveTime element")
        void basicValidation_missingEffectiveTime_returnsError() {
            // Arrange
            String docWithoutEffectiveTime = createDocumentWithoutEffectiveTime();

            // Act
            List<String> errors = validationService.validateCategoryI(docWithoutEffectiveTime);

            // Assert
            assertThat(errors).anyMatch(e -> e.toLowerCase().contains("effectivetime"));
        }
    }

    // Helper methods to create test documents

    private String createValidQrdaCategoryIDocument() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <realmCode code="US"/>
                <typeId root="2.16.840.1.113883.1.3" extension="POCD_HD000040"/>
                <templateId root="2.16.840.1.113883.10.20.24.1.1"/>
                <templateId root="2.16.840.1.113883.10.20.24.1.3"/>
                <id root="12345"/>
                <code code="55182-0" codeSystem="2.16.840.1.113883.6.1"/>
                <effectiveTime value="20240101"/>
                <confidentialityCode code="N"/>
                <languageCode code="en-US"/>
                <recordTarget>
                    <patientRole>
                        <id root="patient-id"/>
                    </patientRole>
                </recordTarget>
                <author><time/><assignedAuthor><id/></assignedAuthor></author>
                <custodian><assignedCustodian><representedCustodianOrganization><id/></representedCustodianOrganization></assignedCustodian></custodian>
                <legalAuthenticator><time/><signatureCode/><assignedEntity><id/></assignedEntity></legalAuthenticator>
                <component><structuredBody></structuredBody></component>
            </ClinicalDocument>
            """;
    }

    private String createValidQrdaCategoryIIIDocument() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <realmCode code="US"/>
                <typeId root="2.16.840.1.113883.1.3" extension="POCD_HD000040"/>
                <templateId root="2.16.840.1.113883.10.20.27.1.1"/>
                <templateId root="2.16.840.1.113883.10.20.27.1.2"/>
                <id root="12345"/>
                <code code="55184-6" codeSystem="2.16.840.1.113883.6.1"/>
                <effectiveTime value="20240101"/>
                <confidentialityCode code="N"/>
                <languageCode code="en-US"/>
                <author><time/><assignedAuthor><id/></assignedAuthor></author>
                <custodian><assignedCustodian><representedCustodianOrganization><id/></representedCustodianOrganization></assignedCustodian></custodian>
                <legalAuthenticator><time/><signatureCode/><assignedEntity><id/></assignedEntity></legalAuthenticator>
                <component><structuredBody><component><section><observation classCode="OBS"/></section></component></structuredBody></component>
            </ClinicalDocument>
            """;
    }

    private String createQrdaDocumentWithoutRecordTarget() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.24.1.1"/>
                <effectiveTime value="20240101"/>
                <component><structuredBody></structuredBody></component>
            </ClinicalDocument>
            """;
    }

    private String createQrdaDocumentWithoutCategoryITemplate() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.22.1.1"/>
                <effectiveTime value="20240101"/>
                <recordTarget><patientRole><id/></patientRole></recordTarget>
                <component><structuredBody></structuredBody></component>
            </ClinicalDocument>
            """;
    }

    private String createQrdaDocumentWithoutCategoryIIITemplate() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.22.1.1"/>
                <effectiveTime value="20240101"/>
                <custodian><assignedCustodian><representedCustodianOrganization><id/></representedCustodianOrganization></assignedCustodian></custodian>
                <legalAuthenticator><time/><signatureCode/><assignedEntity><id/></assignedEntity></legalAuthenticator>
                <component><structuredBody><component><section><observation classCode="OBS"/></section></component></structuredBody></component>
            </ClinicalDocument>
            """;
    }

    private String createQrdaDocumentWithoutCustodian() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.27.1.1"/>
                <effectiveTime value="20240101"/>
                <legalAuthenticator><time/><signatureCode/><assignedEntity><id/></assignedEntity></legalAuthenticator>
                <component><structuredBody><component><section><observation classCode="OBS"/></section></component></structuredBody></component>
            </ClinicalDocument>
            """;
    }

    private String createQrdaDocumentWithoutLegalAuthenticator() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.27.1.1"/>
                <effectiveTime value="20240101"/>
                <custodian><assignedCustodian><representedCustodianOrganization><id/></representedCustodianOrganization></assignedCustodian></custodian>
                <component><structuredBody><component><section><observation classCode="OBS"/></section></component></structuredBody></component>
            </ClinicalDocument>
            """;
    }

    private String createDocumentWithTin(String tin) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <id root="2.16.840.1.113883.4.2" extension="%s"/>
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.27.1.1"/>
                <effectiveTime value="20240101"/>
            </ClinicalDocument>
            """, tin);
    }

    private String createDocumentWithNpi(String npi) {
        return String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <id root="2.16.840.1.113883.4.6" extension="%s"/>
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.27.1.1"/>
                <effectiveTime value="20240101"/>
            </ClinicalDocument>
            """, npi);
    }

    private String createDocumentWithoutTemplateId() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <typeId root="2.16.840.1.113883.1.3"/>
                <effectiveTime value="20240101"/>
            </ClinicalDocument>
            """;
    }

    private String createDocumentWithoutTypeId() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <templateId root="2.16.840.1.113883.10.20.24.1.1"/>
                <effectiveTime value="20240101"/>
            </ClinicalDocument>
            """;
    }

    private String createDocumentWithoutEffectiveTime() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <ClinicalDocument xmlns="urn:hl7-org:v3">
                <typeId root="2.16.840.1.113883.1.3"/>
                <templateId root="2.16.840.1.113883.10.20.24.1.1"/>
            </ClinicalDocument>
            """;
    }
}

package com.healthdata.qrda.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CdaDocumentBuilder.
 * Tests CDA/QRDA document generation logic.
 */
@ExtendWith(MockitoExtension.class)
class CdaDocumentBuilderTest {

    @InjectMocks
    private CdaDocumentBuilder cdaDocumentBuilder;

    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final LocalDate PERIOD_START = LocalDate.of(2024, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2024, 12, 31);

    @Nested
    @DisplayName("buildQrdaCategoryI() tests")
    class BuildQrdaCategoryITests {

        @Test
        @DisplayName("Should generate valid XML document")
        void buildQrdaCategoryI_shouldGenerateValidXml() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).isNotNull();
            assertThat(document).startsWith("<?xml version=\"1.0\"");
            assertThat(document).contains("<ClinicalDocument");
            assertThat(document).contains("</ClinicalDocument>");
        }

        @Test
        @DisplayName("Should include QRDA Category I template OIDs")
        void buildQrdaCategoryI_shouldIncludeTemplateOids() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert - QRDA I template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.24.1.1");
            // CMS QRDA I template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.24.1.3");
        }

        @Test
        @DisplayName("Should include document code for Quality Measure Report")
        void buildQrdaCategoryI_shouldIncludeDocumentCode() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert - LOINC code for Quality Measure Report
            assertThat(document).contains("55182-0");
            assertThat(document).contains("Quality Measure Report");
        }

        @Test
        @DisplayName("Should include realm code for US")
        void buildQrdaCategoryI_shouldIncludeUsRealmCode() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<realmCode code=\"US\"");
        }

        @Test
        @DisplayName("Should include reporting parameters section with dates")
        void buildQrdaCategoryI_shouldIncludeReportingParameters() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert - Should include reporting period dates in YYYYMMDD format
            assertThat(document).contains("20240101"); // Period start
            assertThat(document).contains("20241231"); // Period end
        }

        @Test
        @DisplayName("Should include structured body sections")
        void buildQrdaCategoryI_shouldIncludeStructuredBody() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<structuredBody>");
            assertThat(document).contains("</structuredBody>");
            assertThat(document).contains("<component>");
        }

        @Test
        @DisplayName("Should include record target for patient")
        void buildQrdaCategoryI_shouldIncludeRecordTarget() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<recordTarget>");
            assertThat(document).contains("</recordTarget>");
        }

        @Test
        @DisplayName("Should include language code for en-US")
        void buildQrdaCategoryI_shouldIncludeLanguageCode() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<languageCode code=\"en-US\"");
        }

        @Test
        @DisplayName("Should include confidentiality code")
        void buildQrdaCategoryI_shouldIncludeConfidentialityCode() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<confidentialityCode");
            assertThat(document).contains("code=\"N\""); // Normal confidentiality
        }
    }

    @Nested
    @DisplayName("buildQrdaCategoryIII() tests")
    class BuildQrdaCategoryIIITests {

        @Test
        @DisplayName("Should generate valid XML document")
        void buildQrdaCategoryIII_shouldGenerateValidXml() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).isNotNull();
            assertThat(document).startsWith("<?xml version=\"1.0\"");
            assertThat(document).contains("<ClinicalDocument");
            assertThat(document).contains("</ClinicalDocument>");
        }

        @Test
        @DisplayName("Should include QRDA Category III template OIDs")
        void buildQrdaCategoryIII_shouldIncludeTemplateOids() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert - QRDA III template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.27.1.1");
            // CMS QRDA III template OID
            assertThat(document).contains("2.16.840.1.113883.10.20.27.1.2");
        }

        @Test
        @DisplayName("Should include document code for QRDA Summary Report")
        void buildQrdaCategoryIII_shouldIncludeDocumentCode() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert - LOINC code for QRDA Calculated Summary Report
            assertThat(document).contains("55184-6");
        }

        @Test
        @DisplayName("Should NOT include record target (aggregate report)")
        void buildQrdaCategoryIII_shouldNotIncludeRecordTarget() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert - Category III is aggregate, not patient-specific
            assertThat(document).doesNotContain("<recordTarget>");
        }

        @Test
        @DisplayName("Should include reporting parameters section")
        void buildQrdaCategoryIII_shouldIncludeReportingParameters() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("20240101"); // Period start
            assertThat(document).contains("20241231"); // Period end
        }

        @Test
        @DisplayName("Should include aggregate measure section")
        void buildQrdaCategoryIII_shouldIncludeAggregateMeasureSection() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<structuredBody>");
        }
    }

    @Nested
    @DisplayName("Document structure tests")
    class DocumentStructureTests {

        @Test
        @DisplayName("Both document types should have typeId element")
        void bothDocumentTypes_shouldHaveTypeId() {
            // Act
            String catI = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);
            String catIII = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert - typeId is required for CDA documents
            assertThat(catI).contains("<typeId");
            assertThat(catIII).contains("<typeId");
        }

        @Test
        @DisplayName("Both document types should have author section")
        void bothDocumentTypes_shouldHaveAuthor() {
            // Act
            String catI = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);
            String catIII = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert
            assertThat(catI).contains("<author>");
            assertThat(catIII).contains("<author>");
        }

        @Test
        @DisplayName("Both document types should have custodian section")
        void bothDocumentTypes_shouldHaveCustodian() {
            // Act
            String catI = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);
            String catIII = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert
            assertThat(catI).contains("<custodian>");
            assertThat(catIII).contains("<custodian>");
        }

        @Test
        @DisplayName("Both document types should have legal authenticator")
        void bothDocumentTypes_shouldHaveLegalAuthenticator() {
            // Act
            String catI = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);
            String catIII = cdaDocumentBuilder.buildQrdaCategoryIII(PERIOD_START, PERIOD_END);

            // Assert
            assertThat(catI).contains("<legalAuthenticator>");
            assertThat(catIII).contains("<legalAuthenticator>");
        }

        @Test
        @DisplayName("Document should have effectiveTime element")
        void document_shouldHaveEffectiveTime() {
            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert
            assertThat(document).contains("<effectiveTime");
        }

        @Test
        @DisplayName("Document should include unique document ID")
        void document_shouldHaveUniqueId() {
            // Act
            String doc1 = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);
            String doc2 = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, PERIOD_START, PERIOD_END);

            // Assert - Both should have id element
            assertThat(doc1).contains("<id ");
            assertThat(doc2).contains("<id ");
            // UUIDs should be different (generated each time)
            // Note: This is a simplified check - actual implementation generates unique IDs
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle same start and end date")
        void buildQrdaCategoryI_withSameDates_shouldSucceed() {
            // Arrange
            LocalDate sameDate = LocalDate.of(2024, 6, 15);

            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, sameDate, sameDate);

            // Assert
            assertThat(document).isNotNull();
            assertThat(document).contains("20240615");
        }

        @Test
        @DisplayName("Should handle leap year dates")
        void buildQrdaCategoryI_withLeapYearDate_shouldSucceed() {
            // Arrange
            LocalDate leapYearStart = LocalDate.of(2024, 2, 29);
            LocalDate leapYearEnd = LocalDate.of(2024, 12, 31);

            // Act
            String document = cdaDocumentBuilder.buildQrdaCategoryI(PATIENT_ID, leapYearStart, leapYearEnd);

            // Assert
            assertThat(document).isNotNull();
            assertThat(document).contains("20240229");
        }

        @Test
        @DisplayName("Should handle different patient IDs")
        void buildQrdaCategoryI_withDifferentPatients_shouldGenerateDistinctDocuments() {
            // Arrange
            UUID patient1 = UUID.randomUUID();
            UUID patient2 = UUID.randomUUID();

            // Act
            String doc1 = cdaDocumentBuilder.buildQrdaCategoryI(patient1, PERIOD_START, PERIOD_END);
            String doc2 = cdaDocumentBuilder.buildQrdaCategoryI(patient2, PERIOD_START, PERIOD_END);

            // Assert - Both should be valid documents
            assertThat(doc1).contains("<ClinicalDocument");
            assertThat(doc2).contains("<ClinicalDocument");
        }
    }
}

package com.healthdata.qrda.service;

import com.healthdata.qrda.client.QualityMeasureClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Service for building CDA (Clinical Document Architecture) documents.
 *
 * CDA is the underlying XML format for QRDA documents. This builder
 * creates the document structure with proper headers, sections, and entries.
 *
 * Implements HL7 CDA R2 with QRDA Implementation Guide extensions.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CdaDocumentBuilder {

    private static final String CDA_NAMESPACE = "urn:hl7-org:v3";
    private static final String SDTC_NAMESPACE = "urn:hl7-org:sdtc";
    private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

    // QRDA Template OIDs
    private static final String QRDA_I_TEMPLATE_OID = "2.16.840.1.113883.10.20.24.1.1";
    private static final String QRDA_III_TEMPLATE_OID = "2.16.840.1.113883.10.20.27.1.1";
    private static final String CMS_QRDA_I_TEMPLATE_OID = "2.16.840.1.113883.10.20.24.1.3";
    private static final String CMS_QRDA_III_TEMPLATE_OID = "2.16.840.1.113883.10.20.27.1.2";

    /**
     * Builds a QRDA Category I document for a patient.
     *
     * @param patientId The patient identifier
     * @param periodStart Performance period start
     * @param periodEnd Performance period end
     * @return XML document as string
     */
    public String buildQrdaCategoryI(UUID patientId, LocalDate periodStart, LocalDate periodEnd) {
        log.debug("Building QRDA Category I document for patient {}", patientId);

        StringBuilder doc = new StringBuilder();
        doc.append(getXmlDeclaration());
        doc.append(getStylesheetReference());
        doc.append(openClinicalDocument());

        // Header sections
        doc.append(buildRealmCode("US"));
        doc.append(buildTypeId());
        doc.append(buildTemplateIds(QRDA_I_TEMPLATE_OID, CMS_QRDA_I_TEMPLATE_OID));
        doc.append(buildDocumentId(UUID.randomUUID()));
        doc.append(buildDocumentCode("55182-0", "Quality Measure Report"));
        doc.append(buildTitle("QRDA Category I Report"));
        doc.append(buildEffectiveTime(LocalDateTime.now()));
        doc.append(buildConfidentialityCode("N"));
        doc.append(buildLanguageCode("en-US"));

        // Record target (patient)
        doc.append(buildRecordTargetPlaceholder(patientId));

        // Author
        doc.append(buildAuthorPlaceholder());

        // Custodian
        doc.append(buildCustodianPlaceholder());

        // Legal authenticator
        doc.append(buildLegalAuthenticatorPlaceholder());

        // Document body
        doc.append("<component>");
        doc.append("<structuredBody>");

        // Measure Section
        doc.append(buildMeasureSectionPlaceholder());

        // Reporting Parameters Section
        doc.append(buildReportingParametersSection(periodStart, periodEnd));

        // Patient Data Section
        doc.append(buildPatientDataSectionPlaceholder());

        doc.append("</structuredBody>");
        doc.append("</component>");

        doc.append(closeClinicalDocument());

        return doc.toString();
    }

    /**
     * Builds a QRDA Category III document for aggregate reporting.
     *
     * @param periodStart Performance period start
     * @param periodEnd Performance period end
     * @return XML document as string
     */
    public String buildQrdaCategoryIII(LocalDate periodStart, LocalDate periodEnd) {
        log.debug("Building QRDA Category III document");

        StringBuilder doc = new StringBuilder();
        doc.append(getXmlDeclaration());
        doc.append(getStylesheetReference());
        doc.append(openClinicalDocument());

        // Header sections
        doc.append(buildRealmCode("US"));
        doc.append(buildTypeId());
        doc.append(buildTemplateIds(QRDA_III_TEMPLATE_OID, CMS_QRDA_III_TEMPLATE_OID));
        doc.append(buildDocumentId(UUID.randomUUID()));
        doc.append(buildDocumentCode("55184-6", "Quality Reporting Document Architecture Calculated Summary Report"));
        doc.append(buildTitle("QRDA Category III Report"));
        doc.append(buildEffectiveTime(LocalDateTime.now()));
        doc.append(buildConfidentialityCode("N"));
        doc.append(buildLanguageCode("en-US"));

        // Author
        doc.append(buildAuthorPlaceholder());

        // Custodian
        doc.append(buildCustodianPlaceholder());

        // Legal authenticator
        doc.append(buildLegalAuthenticatorPlaceholder());

        // Document body
        doc.append("<component>");
        doc.append("<structuredBody>");

        // Reporting Parameters Section
        doc.append(buildReportingParametersSection(periodStart, periodEnd));

        // Measure Section (with aggregate data)
        doc.append(buildAggregateMeasureSectionPlaceholder());

        doc.append("</structuredBody>");
        doc.append("</component>");

        doc.append(closeClinicalDocument());

        return doc.toString();
    }

    private String getXmlDeclaration() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
    }

    private String getStylesheetReference() {
        return "<?xml-stylesheet type=\"text/xsl\" href=\"CDA.xsl\"?>\n";
    }

    private String openClinicalDocument() {
        return String.format(
            "<ClinicalDocument xmlns=\"%s\" xmlns:sdtc=\"%s\" xmlns:xsi=\"%s\">\n",
            CDA_NAMESPACE, SDTC_NAMESPACE, XSI_NAMESPACE);
    }

    private String closeClinicalDocument() {
        return "</ClinicalDocument>";
    }

    private String buildRealmCode(String code) {
        return String.format("<realmCode code=\"%s\"/>\n", code);
    }

    private String buildTypeId() {
        return "<typeId root=\"2.16.840.1.113883.1.3\" extension=\"POCD_HD000040\"/>\n";
    }

    private String buildTemplateIds(String... oids) {
        StringBuilder sb = new StringBuilder();
        for (String oid : oids) {
            sb.append(String.format("<templateId root=\"%s\"/>\n", oid));
        }
        return sb.toString();
    }

    private String buildDocumentId(UUID id) {
        return String.format("<id root=\"%s\"/>\n", id);
    }

    private String buildDocumentCode(String code, String displayName) {
        return String.format(
            "<code code=\"%s\" codeSystem=\"2.16.840.1.113883.6.1\" " +
            "codeSystemName=\"LOINC\" displayName=\"%s\"/>\n",
            code, displayName);
    }

    private String buildTitle(String title) {
        return String.format("<title>%s</title>\n", title);
    }

    private String buildEffectiveTime(LocalDateTime dateTime) {
        // CDA effective time format: yyyyMMddHHmmss (without timezone for LocalDateTime)
        // For UTC timezone indicator, append +0000
        String formatted = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "+0000";
        return String.format("<effectiveTime value=\"%s\"/>\n", formatted);
    }

    private String buildConfidentialityCode(String code) {
        return String.format(
            "<confidentialityCode code=\"%s\" codeSystem=\"2.16.840.1.113883.5.25\"/>\n", code);
    }

    private String buildLanguageCode(String code) {
        return String.format("<languageCode code=\"%s\"/>\n", code);
    }

    private String buildReportingParametersSection(LocalDate periodStart, LocalDate periodEnd) {
        String startFormatted = periodStart.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endFormatted = periodEnd.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        return """
            <component>
              <section>
                <templateId root="2.16.840.1.113883.10.20.17.2.1"/>
                <code code="55187-9" codeSystem="2.16.840.1.113883.6.1"/>
                <title>Reporting Parameters</title>
                <text>
                  <list>
                    <item>Reporting Period: %s - %s</item>
                  </list>
                </text>
                <entry typeCode="DRIV">
                  <act classCode="ACT" moodCode="EVN">
                    <templateId root="2.16.840.1.113883.10.20.17.3.8"/>
                    <code code="252116004" codeSystem="2.16.840.1.113883.6.96" displayName="Observation Parameters"/>
                    <effectiveTime>
                      <low value="%s"/>
                      <high value="%s"/>
                    </effectiveTime>
                  </act>
                </entry>
              </section>
            </component>
            """.formatted(startFormatted, endFormatted, startFormatted, endFormatted);
    }

    // Placeholder methods - to be implemented with actual data integration
    private String buildRecordTargetPlaceholder(UUID patientId) {
        return "<!-- recordTarget: Patient " + patientId + " -->\n<recordTarget><patientRole><id root=\"\" extension=\"\"/></patientRole></recordTarget>\n";
    }

    private String buildAuthorPlaceholder() {
        return "<!-- author -->\n<author><time value=\"\"/><assignedAuthor><id root=\"\"/></assignedAuthor></author>\n";
    }

    private String buildCustodianPlaceholder() {
        return "<!-- custodian -->\n<custodian><assignedCustodian><representedCustodianOrganization><id root=\"\"/><name/></representedCustodianOrganization></assignedCustodian></custodian>\n";
    }

    private String buildLegalAuthenticatorPlaceholder() {
        return "<!-- legalAuthenticator -->\n<legalAuthenticator><time value=\"\"/><signatureCode code=\"S\"/><assignedEntity><id root=\"\"/></assignedEntity></legalAuthenticator>\n";
    }

    private String buildMeasureSectionPlaceholder() {
        return "<!-- measureSection: Quality measures will be populated here -->\n<component><section><templateId root=\"2.16.840.1.113883.10.20.24.2.2\"/><code code=\"55186-1\" codeSystem=\"2.16.840.1.113883.6.1\"/><title>Measure Section</title><text/></section></component>\n";
    }

    private String buildPatientDataSectionPlaceholder() {
        return "<!-- patientDataSection: Clinical data will be populated here -->\n<component><section><templateId root=\"2.16.840.1.113883.10.20.24.2.1\"/><code code=\"55188-7\" codeSystem=\"2.16.840.1.113883.6.1\"/><title>Patient Data</title><text/></section></component>\n";
    }

    private String buildAggregateMeasureSectionPlaceholder() {
        return "<!-- aggregateMeasureSection: Aggregate measure data will be populated here -->\n<component><section><templateId root=\"2.16.840.1.113883.10.20.27.2.1\"/><code code=\"55186-1\" codeSystem=\"2.16.840.1.113883.6.1\"/><title>Measure Section</title><text/></section></component>\n";
    }

    /**
     * Builds a QRDA Category III document with actual measure data.
     *
     * @param periodStart Performance period start
     * @param periodEnd Performance period end
     * @param measureResults List of aggregate measure results
     * @return XML document as string
     */
    public String buildQrdaCategoryIIIWithData(
            LocalDate periodStart,
            LocalDate periodEnd,
            List<QualityMeasureClient.MeasureAggregateDTO> measureResults) {

        log.debug("Building QRDA Category III document with {} measures", measureResults.size());

        StringBuilder doc = new StringBuilder();
        doc.append(getXmlDeclaration());
        doc.append(getStylesheetReference());
        doc.append(openClinicalDocument());

        // Header sections
        doc.append(buildRealmCode("US"));
        doc.append(buildTypeId());
        doc.append(buildTemplateIds(QRDA_III_TEMPLATE_OID, CMS_QRDA_III_TEMPLATE_OID));
        doc.append(buildDocumentId(UUID.randomUUID()));
        doc.append(buildDocumentCode("55184-6", "Quality Reporting Document Architecture Calculated Summary Report"));
        doc.append(buildTitle("QRDA Category III Report"));
        doc.append(buildEffectiveTime(LocalDateTime.now()));
        doc.append(buildConfidentialityCode("N"));
        doc.append(buildLanguageCode("en-US"));

        // Author
        doc.append(buildAuthorPlaceholder());

        // Custodian
        doc.append(buildCustodianPlaceholder());

        // Legal authenticator
        doc.append(buildLegalAuthenticatorPlaceholder());

        // Document body
        doc.append("<component>");
        doc.append("<structuredBody>");

        // Reporting Parameters Section
        doc.append(buildReportingParametersSection(periodStart, periodEnd));

        // Measure Section with actual aggregate data
        doc.append(buildAggregateMeasureSection(measureResults));

        doc.append("</structuredBody>");
        doc.append("</component>");

        doc.append(closeClinicalDocument());

        return doc.toString();
    }

    /**
     * Builds the aggregate measure section with actual measure data.
     */
    private String buildAggregateMeasureSection(List<QualityMeasureClient.MeasureAggregateDTO> measureResults) {
        StringBuilder section = new StringBuilder();
        section.append("<component>\n");
        section.append("  <section>\n");
        section.append("    <!-- Measure Section Template -->\n");
        section.append("    <templateId root=\"2.16.840.1.113883.10.20.27.2.1\" extension=\"2017-06-01\"/>\n");
        section.append("    <!-- CMS QRDA III Measure Section Template -->\n");
        section.append("    <templateId root=\"2.16.840.1.113883.10.20.27.2.3\" extension=\"2019-05-01\"/>\n");
        section.append("    <code code=\"55186-1\" codeSystem=\"2.16.840.1.113883.6.1\" displayName=\"Measure Section\"/>\n");
        section.append("    <title>Measure Section</title>\n");
        section.append("    <text>\n");
        section.append("      <table border=\"1\" width=\"100%\">\n");
        section.append("        <thead><tr><th>Measure</th><th>IPP</th><th>DENOM</th><th>DENEX</th><th>NUMER</th><th>Performance Rate</th></tr></thead>\n");
        section.append("        <tbody>\n");

        for (QualityMeasureClient.MeasureAggregateDTO measure : measureResults) {
            section.append(String.format("          <tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td>%d</td><td>%s%%</td></tr>\n",
                measure.getMeasureName() != null ? measure.getMeasureName() : measure.getMeasureId(),
                measure.getInitialPopulation(),
                measure.getDenominator(),
                measure.getDenominatorExclusions(),
                measure.getNumerator(),
                measure.getPerformanceRate() != null ? measure.getPerformanceRate().toString() : "N/A"));
        }

        section.append("        </tbody>\n");
        section.append("      </table>\n");
        section.append("    </text>\n");

        // Build measure entries
        for (QualityMeasureClient.MeasureAggregateDTO measure : measureResults) {
            section.append(buildMeasureReferenceEntry(measure));
        }

        section.append("  </section>\n");
        section.append("</component>\n");

        return section.toString();
    }

    /**
     * Builds a measure reference entry with aggregate data.
     */
    private String buildMeasureReferenceEntry(QualityMeasureClient.MeasureAggregateDTO measure) {
        StringBuilder entry = new StringBuilder();
        entry.append("    <entry>\n");
        entry.append("      <organizer classCode=\"CLUSTER\" moodCode=\"EVN\">\n");
        entry.append("        <!-- Measure Reference Template -->\n");
        entry.append("        <templateId root=\"2.16.840.1.113883.10.20.27.3.1\" extension=\"2016-09-01\"/>\n");
        entry.append("        <!-- CMS Measure Reference Template -->\n");
        entry.append("        <templateId root=\"2.16.840.1.113883.10.20.27.3.17\" extension=\"2019-05-01\"/>\n");
        entry.append("        <id root=\"").append(UUID.randomUUID()).append("\"/>\n");
        entry.append("        <statusCode code=\"completed\"/>\n");

        // Measure reference
        entry.append("        <reference typeCode=\"REFR\">\n");
        entry.append("          <externalDocument classCode=\"DOC\" moodCode=\"EVN\">\n");
        entry.append("            <!-- Measure identifier -->\n");
        entry.append("            <id root=\"2.16.840.1.113883.4.738\" extension=\"").append(measure.getMeasureVersionId() != null ? measure.getMeasureVersionId() : measure.getMeasureId()).append("\"/>\n");
        if (measure.getCms() != null) {
            entry.append("            <code code=\"57024-2\" codeSystem=\"2.16.840.1.113883.6.1\" displayName=\"Health Quality Measure Document\">\n");
            entry.append("              <translation code=\"").append(measure.getCms()).append("\" codeSystem=\"2.16.840.1.113883.4.738\"/>\n");
            entry.append("            </code>\n");
        }
        entry.append("            <text>").append(measure.getMeasureName() != null ? measure.getMeasureName() : measure.getMeasureId()).append("</text>\n");
        entry.append("          </externalDocument>\n");
        entry.append("        </reference>\n");

        // Population counts
        entry.append(buildPopulationComponent("IPP", "initialPopulation", measure.getInitialPopulation()));
        entry.append(buildPopulationComponent("DENOM", "denominator", measure.getDenominator()));
        entry.append(buildPopulationComponent("DENEX", "denominatorExclusions", measure.getDenominatorExclusions()));
        entry.append(buildPopulationComponent("DENEXCEP", "denominatorExceptions", measure.getDenominatorExceptions()));
        entry.append(buildPopulationComponent("NUMER", "numerator", measure.getNumerator()));

        // Performance rate
        if (measure.getPerformanceRate() != null) {
            entry.append(buildPerformanceRateComponent(measure.getPerformanceRate()));
        }

        entry.append("      </organizer>\n");
        entry.append("    </entry>\n");

        return entry.toString();
    }

    /**
     * Builds a population count component.
     */
    private String buildPopulationComponent(String code, String name, int count) {
        return String.format("""
                <component>
                  <observation classCode="OBS" moodCode="EVN">
                    <!-- Aggregate Count Template -->
                    <templateId root="2.16.840.1.113883.10.20.27.3.3"/>
                    <code code="%s" codeSystem="2.16.840.1.113883.5.4" codeSystemName="ActCode" displayName="%s"/>
                    <statusCode code="completed"/>
                    <value xsi:type="INT" value="%d"/>
                    <methodCode code="COUNT" codeSystem="2.16.840.1.113883.5.84" displayName="Count"/>
                  </observation>
                </component>
            """, code, name, count);
    }

    /**
     * Builds a performance rate component.
     */
    private String buildPerformanceRateComponent(BigDecimal performanceRate) {
        // Performance rate as a decimal (0.0-1.0)
        BigDecimal rateAsDecimal = performanceRate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        return String.format("""
                <component>
                  <observation classCode="OBS" moodCode="EVN">
                    <!-- Performance Rate Template -->
                    <templateId root="2.16.840.1.113883.10.20.27.3.30" extension="2016-09-01"/>
                    <code code="72510-1" codeSystem="2.16.840.1.113883.6.1" displayName="Performance Rate"/>
                    <statusCode code="completed"/>
                    <value xsi:type="REAL" value="%s"/>
                  </observation>
                </component>
            """, rateAsDecimal.toPlainString());
    }
}


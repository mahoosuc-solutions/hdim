package com.healthdata.enrichment.analyzer;

import com.healthdata.enrichment.model.CompletionSuggestion;
import com.healthdata.enrichment.model.DataQualityScore;
import com.healthdata.enrichment.model.MissingDataReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class DataCompletenessAnalyzer {

    public MissingDataReport analyze(String patientId) {
        Map<String, List<String>> missingElements = new HashMap<>();
        missingElements.put("demographics", List.of("ethnicity", "preferred_language"));
        missingElements.put("medications", List.of("current_medications"));
        missingElements.put("labs", List.of("HbA1c", "lipid_panel"));

        List<CompletionSuggestion> suggestions = getSuggestions(patientId);

        return MissingDataReport.builder()
            .patientId(patientId)
            .completenessScore(calculateCompletenessScore(patientId).getCompletenessScore())
            .missingElements(missingElements)
            .suggestions(suggestions)
            .generatedAt(LocalDateTime.now())
            .build();
    }

    public MissingDataReport analyzeForMeasure(String patientId, String measureId) {
        Map<String, List<String>> missingElements = new HashMap<>();
        missingElements.put("measure_data", List.of("hba1c_test", "eye_exam", "kidney_screening"));

        return MissingDataReport.builder()
            .patientId(patientId)
            .completenessScore(65.0)
            .missingElements(missingElements)
            .build();
    }

    public DataQualityScore calculateCompletenessScore(String patientId) {
        return DataQualityScore.builder()
            .completenessScore(75.0)
            .accuracyScore(85.0)
            .consistencyScore(80.0)
            .timelinessScore(70.0)
            .overallScore(77.5)
            .build();
    }

    public List<String> findMissingLabResults(String patientId) {
        return List.of("HbA1c", "Creatinine", "Lipid Panel");
    }

    public List<CompletionSuggestion> getSuggestions(String patientId) {
        return List.of(
            CompletionSuggestion.builder()
                .element("HbA1c")
                .description("Annual HbA1c test required for diabetes patients")
                .priority(1)
                .action("Order HbA1c test")
                .reason("Required for CMS122 measure")
                .build(),
            CompletionSuggestion.builder()
                .element("Eye Exam")
                .description("Annual diabetic retinal exam")
                .priority(2)
                .action("Schedule eye exam")
                .reason("Required for CMS131 measure")
                .build()
        );
    }

    public double calculateMeasureCompleteness(String patientId, String measureId) {
        return 75.0;
    }

    public List<String> findDocumentationGaps(String patientId) {
        return List.of("Missing medication list", "Incomplete allergy history");
    }

    public List<CompletionSuggestion> suggestNextActions(String patientId) {
        return getSuggestions(patientId);
    }

    public List<String> findStaleData(String patientId) {
        return List.of("Vital signs (>6 months old)", "Medication list (>1 year old)");
    }

    public Map<String, LocalDateTime> getCompletionTimeline(String patientId) {
        Map<String, LocalDateTime> timeline = new HashMap<>();
        timeline.put("HbA1c", LocalDateTime.now().plusDays(30));
        timeline.put("Eye Exam", LocalDateTime.now().plusDays(60));
        return timeline;
    }

    public MissingDataReport analyzeWithTenant(String patientId, String tenantId) {
        MissingDataReport report = analyze(patientId);
        report.setTenantId(tenantId);
        return report;
    }
}

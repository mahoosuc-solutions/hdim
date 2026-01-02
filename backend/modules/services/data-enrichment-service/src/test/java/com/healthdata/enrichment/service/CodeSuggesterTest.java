package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.CodeSuggestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for CodeSuggester.
 */
@DisplayName("CodeSuggester TDD Tests")
class CodeSuggesterTest {

    private CodeSuggester suggester;

    @BeforeEach
    void setUp() {
        suggester = new CodeSuggester();
    }

    @Test
    @DisplayName("Should suggest ICD-10 codes from text")
    void testSuggestIcd10FromText() {
        String text = "Type 2 Diabetes Mellitus";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).startsWith("E11");
    }

    @Test
    @DisplayName("Should suggest CPT codes from procedure text")
    void testSuggestCptFromText() {
        String text = "Office visit, established patient";
        List<CodeSuggestion> suggestions = suggester.suggestCpt(text);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).startsWith("992");
    }

    @Test
    @DisplayName("Should rank suggestions by confidence")
    void testSuggestionRanking() {
        String text = "Diabetes";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).isSortedAccordingTo((s1, s2) ->
            Double.compare(s2.getConfidence(), s1.getConfidence()));
    }

    @Test
    @DisplayName("Should suggest multiple relevant codes")
    void testMultipleSuggestions() {
        String text = "Type 2 Diabetes with complications";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should include code descriptions")
    void testIncludeDescriptions() {
        String text = "Diabetes";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).allMatch(s -> s.getDescription() != null);
    }

    @Test
    @DisplayName("Should handle complex medical terms")
    void testComplexMedicalTerms() {
        String text = "Acute myocardial infarction";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).startsWith("I21");
    }

    @Test
    @DisplayName("Should suggest SNOMED codes")
    void testSuggestSnomed() {
        String text = "Diabetes mellitus";
        List<CodeSuggestion> suggestions = suggester.suggestSnomed(text);
        assertThat(suggestions).isNotEmpty();
    }

    @Test
    @DisplayName("Should suggest LOINC codes for labs")
    void testSuggestLoincForLabs() {
        String text = "Hemoglobin A1c";
        List<CodeSuggestion> suggestions = suggester.suggestLoinc(text);
        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.get(0).getCode()).isEqualTo("4548-4");
    }

    @Test
    @DisplayName("Should limit number of suggestions")
    void testLimitSuggestions() {
        String text = "Diabetes";
        int maxSuggestions = 5;
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text, maxSuggestions);
        assertThat(suggestions).hasSizeLessThanOrEqualTo(maxSuggestions);
    }

    @Test
    @DisplayName("Should handle empty text")
    void testEmptyText() {
        String text = "";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).isEmpty();
    }

    @Test
    @DisplayName("Should use context for better suggestions")
    void testContextualSuggestions() {
        String text = "Diabetes";
        String context = "Patient has kidney disease";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10WithContext(text, context);
        assertThat(suggestions).anyMatch(s -> s.getCode().contains("E11.2"));
    }

    @Test
    @DisplayName("Should suggest based on partial matches")
    void testPartialMatches() {
        String text = "diabet";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).isNotEmpty();
    }

    @Test
    @DisplayName("Should handle abbreviations")
    void testAbbreviations() {
        String text = "DM Type 2";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions).isNotEmpty();
    }

    @Test
    @DisplayName("Should suggest codes from clinical note")
    void testSuggestFromClinicalNote() {
        String note = "Patient with HTN and DM2 on Metformin";
        var suggestions = suggester.suggestAllCodes(note);
        assertThat(suggestions).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should provide billable code preferences")
    void testBillablePreference() {
        String text = "Diabetes";
        List<CodeSuggestion> suggestions = suggester.suggestIcd10(text);
        assertThat(suggestions.get(0).isBillable()).isTrue();
    }
}

package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDOH Screening Question
 *
 * Represents a standardized screening question based on Gravity Project,
 * AHC-HRSN, or PRAPARE screening tools
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohScreeningQuestion {
    private String questionId;
    private String questionText;
    private SdohCategory category;
    private String loincCode;
    private QuestionType questionType;
    private String[] answerOptions;
    private boolean required;
    private Integer sequenceNumber;

    public enum QuestionType {
        SINGLE_CHOICE,
        MULTIPLE_CHOICE,
        TEXT,
        NUMERIC,
        DATE,
        YES_NO
    }
}

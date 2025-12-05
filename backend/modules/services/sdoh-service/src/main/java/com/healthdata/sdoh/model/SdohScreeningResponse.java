package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SDOH Screening Response
 *
 * Patient's response to a screening question
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohScreeningResponse {
    private String questionId;
    private String answer;
    private String[] multipleAnswers;
    private Integer numericValue;
}

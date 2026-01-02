package com.healthdata.sales.entity;

/**
 * Type of contact role in the buying decision
 */
public enum ContactType {
    DECISION_MAKER,  // Final authority on purchase decision
    INFLUENCER,      // Influences the decision but doesn't make it
    CHAMPION,        // Internal advocate for the solution
    USER,            // End user of the product
    TECHNICAL,       // Technical evaluator
    EXECUTIVE,       // C-suite or VP level
    OTHER
}

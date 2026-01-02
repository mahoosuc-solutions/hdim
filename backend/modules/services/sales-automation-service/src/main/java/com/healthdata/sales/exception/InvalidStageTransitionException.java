package com.healthdata.sales.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when an invalid stage transition is attempted
 */
public class InvalidStageTransitionException extends SalesException {

    public InvalidStageTransitionException(String fromStage, String toStage) {
        super(
            String.format("Invalid stage transition from '%s' to '%s'", fromStage, toStage),
            HttpStatus.BAD_REQUEST,
            "INVALID_STAGE_TRANSITION"
        );
    }

    public InvalidStageTransitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "INVALID_STAGE_TRANSITION");
    }
}

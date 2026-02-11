package com.healthdata.agentvalidation.domain.enums;

/**
 * Potential harm levels identified during agent self-reflection.
 */
public enum HarmLevel {

    /**
     * No potential harm identified.
     */
    NONE,

    /**
     * Low potential for harm - minor inaccuracies or incomplete information.
     */
    LOW,

    /**
     * Medium potential for harm - could lead to suboptimal care decisions.
     */
    MEDIUM,

    /**
     * High potential for harm - could lead to dangerous clinical decisions.
     */
    HIGH
}

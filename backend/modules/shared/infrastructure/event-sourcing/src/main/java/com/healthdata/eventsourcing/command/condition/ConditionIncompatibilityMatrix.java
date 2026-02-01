package com.healthdata.eventsourcing.command.condition;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class ConditionIncompatibilityMatrix {
    private final List<IncompatibilityRule> rules = new ArrayList<>();

    public ConditionIncompatibilityMatrix() {
        // Type 1 DM (E10.*) incompatible with Type 2 DM (E11.*)
        addIncompatibility("E10\\..*", "E11\\..*", "Type 1 and Type 2 diabetes are mutually exclusive");
        addIncompatibility("E11\\..*", "E10\\..*", "Type 1 and Type 2 diabetes are mutually exclusive");

        // Type 1 (E10.*) incompatible with Gestational (O24.4*)
        addIncompatibility("E10\\..*", "O24\\.4.*", "Type 1 DM incompatible with Gestational diabetes");
        addIncompatibility("O24\\.4.*", "E10\\..*", "Type 1 DM incompatible with Gestational diabetes");

        // Type 2 (E11.*) incompatible with Gestational (O24.4*)
        addIncompatibility("E11\\..*", "O24\\.4.*", "Type 2 DM incompatible with Gestational diabetes");
        addIncompatibility("O24\\.4.*", "E11\\..*", "Type 2 DM incompatible with Gestational diabetes");

        // Without complications (E10.9) incompatible with complications (E10.[2-8].*)
        addIncompatibility("E10\\.9", "E10\\.[2-8].*", "Cannot have both uncomplicated and complicated diabetes");
    }

    private void addIncompatibility(String codePattern1, String codePattern2, String reason) {
        rules.add(new IncompatibilityRule(codePattern1, codePattern2, reason));
    }

    public boolean areIncompatible(String code1, String code2) {
        return rules.stream()
            .anyMatch(rule ->
                code1.matches(rule.pattern1) && code2.matches(rule.pattern2) ||
                code1.matches(rule.pattern2) && code2.matches(rule.pattern1)
            );
    }

    public List<String> findIncompatibleCodes(String newCode, List<String> existingCodes) {
        List<String> incompatible = new ArrayList<>();
        for (String existingCode : existingCodes) {
            if (areIncompatible(newCode, existingCode)) {
                incompatible.add(existingCode);
            }
        }
        return incompatible;
    }

    public String getReasonForIncompatibility(String code1, String code2) {
        return rules.stream()
            .filter(rule ->
                code1.matches(rule.pattern1) && code2.matches(rule.pattern2) ||
                code1.matches(rule.pattern2) && code2.matches(rule.pattern1)
            )
            .map(rule -> rule.reason)
            .findFirst()
            .orElse("Codes are incompatible");
    }

    private static class IncompatibilityRule {
        final String pattern1;
        final String pattern2;
        final String reason;

        IncompatibilityRule(String pattern1, String pattern2, String reason) {
            this.pattern1 = pattern1;
            this.pattern2 = pattern2;
            this.reason = reason;
        }
    }
}

package com.healthdata.eventrouter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.eventrouter.dto.EventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class EventFilterService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean matches(EventMessage event, String filterExpression) {
        if (filterExpression == null || filterExpression.trim().isEmpty() || filterExpression.equals("{}")) {
            return true;
        }

        try {
            JsonNode filterNode = objectMapper.readTree(filterExpression);
            return evaluateFilter(event.getPayload(), filterNode);
        } catch (Exception e) {
            log.error("Error parsing filter expression: {}", filterExpression, e);
            return false;
        }
    }

    private boolean evaluateFilter(Map<String, Object> payload, JsonNode filterNode) {
        var fields = filterNode.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (!evaluateCondition(payload, key, value)) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateCondition(Map<String, Object> payload, String key, JsonNode filterValue) {
        Object actualValue = getNestedValue(payload, key);

        if (filterValue.isObject()) {
            return evaluateOperator(actualValue, filterValue);
        } else {
            return compareValues(actualValue, filterValue);
        }
    }

    private boolean evaluateOperator(Object actualValue, JsonNode filterValue) {
        var fields = filterValue.fields();
        while (fields.hasNext()) {
            var entry = fields.next();
            String operator = entry.getKey();
            JsonNode operand = entry.getValue();

            switch (operator) {
                case "$gte":
                    return compareNumeric(actualValue, operand) >= 0;
                case "$gt":
                    return compareNumeric(actualValue, operand) > 0;
                case "$lte":
                    return compareNumeric(actualValue, operand) <= 0;
                case "$lt":
                    return compareNumeric(actualValue, operand) < 0;
                case "$ne":
                    return !compareValues(actualValue, operand);
                case "$in":
                    return inArray(actualValue, operand);
                case "$exists":
                    return (actualValue != null) == operand.asBoolean();
                case "$regex":
                    return matchesRegex(actualValue, operand.asText());
                default:
                    log.warn("Unknown operator: {}", operator);
                    return false;
            }
        }
        return true;
    }

    private boolean compareValues(Object actual, JsonNode expected) {
        if (actual == null) {
            return expected.isNull();
        }

        if (expected.isTextual()) {
            return actual.toString().equals(expected.asText());
        } else if (expected.isNumber()) {
            return Double.valueOf(actual.toString()).equals(expected.asDouble());
        } else if (expected.isBoolean()) {
            return Boolean.valueOf(actual.toString()).equals(expected.asBoolean());
        }

        return false;
    }

    private int compareNumeric(Object actual, JsonNode expected) {
        if (actual == null) {
            return -1;
        }

        double actualNum = actual instanceof Number ?
            ((Number) actual).doubleValue() :
            Double.parseDouble(actual.toString());
        double expectedNum = expected.asDouble();

        return Double.compare(actualNum, expectedNum);
    }

    private boolean inArray(Object actual, JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return false;
        }

        String actualStr = actual != null ? actual.toString() : null;
        for (JsonNode item : arrayNode) {
            if (item.asText().equals(actualStr)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRegex(Object actual, String regex) {
        if (actual == null) {
            return false;
        }

        try {
            Pattern pattern = Pattern.compile(regex);
            return pattern.matcher(actual.toString()).matches();
        } catch (Exception e) {
            log.error("Error evaluating regex: {}", regex, e);
            return false;
        }
    }

    private Object getNestedValue(Map<String, Object> payload, String key) {
        if (key.contains(".")) {
            String[] parts = key.split("\\.");
            Object current = payload;

            for (String part : parts) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
            }
            return current;
        } else {
            return payload.get(key);
        }
    }
}

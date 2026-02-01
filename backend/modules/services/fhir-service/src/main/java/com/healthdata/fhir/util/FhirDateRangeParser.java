package com.healthdata.fhir.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class FhirDateRangeParser {

    private FhirDateRangeParser() {}

    public static DateRange parseDateRange(List<String> dateParams) {
        if (dateParams == null || dateParams.isEmpty()) {
            return null;
        }

        LocalDateTime start = null;
        LocalDateTime end = null;

        for (String raw : dateParams) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String value = raw.trim();
            String prefix = null;
            if (value.length() > 2) {
                prefix = value.substring(0, 2);
                if (!prefix.matches("ge|le|gt|lt|eq")) {
                    prefix = null;
                }
            }

            String dateValue = prefix == null ? value : value.substring(2);
            LocalDateTime parsed = parseDateTime(dateValue);
            if (parsed == null) {
                continue;
            }

            if (prefix == null || "eq".equals(prefix)) {
                if (start == null || parsed.isBefore(start)) {
                    start = parsed;
                }
                if (end == null || parsed.isAfter(end)) {
                    end = parsed;
                }
            } else if ("ge".equals(prefix) || "gt".equals(prefix)) {
                if (start == null || parsed.isAfter(start)) {
                    start = parsed;
                }
            } else if ("le".equals(prefix) || "lt".equals(prefix)) {
                if (end == null || parsed.isBefore(end)) {
                    end = parsed;
                }
            }
        }

        if (start == null && end == null) {
            return null;
        }

        if (start == null) {
            start = end;
        }
        if (end == null) {
            end = start;
        }

        return new DateRange(start, end);
    }

    private static LocalDateTime parseDateTime(String value) {
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(value).atStartOfDay();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    public record DateRange(LocalDateTime start, LocalDateTime end) {}
}

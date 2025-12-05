package com.healthdata.cql.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.healthdata.common.validation.ValidationResult;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents the versioned CQL library source used across services.
 */
public class CqlLibraryDescriptor {

    private static final Pattern HEADER_PATTERN =
            Pattern.compile("library\\s+(\\w+)\\s+version\\s+'([^']+)'", Pattern.CASE_INSENSITIVE);
    private static final Pattern USING_PATTERN =
            Pattern.compile("using\\s+FHIR\\s+version\\s+'([^']+)'", Pattern.CASE_INSENSITIVE);

    private final String libraryName;
    private final String version;
    private final String content;

    public CqlLibraryDescriptor(String libraryName, String version, String content) {
        this.libraryName = StringUtils.trimToNull(libraryName);
        this.version = StringUtils.trimToNull(version);
        this.content = content;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getVersion() {
        return version;
    }

    public String getContent() {
        return content;
    }

    public CqlLibraryIdentifier toIdentifier() {
        return new CqlLibraryIdentifier(
                Objects.requireNonNull(libraryName, "libraryName must not be null"),
                Objects.requireNonNull(version, "version must not be null"));
    }

    /**
     * Performs lightweight validation without invoking the full CQL translator.
     */
    public ValidationResult validate() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (StringUtils.isBlank(libraryName)) {
            errors.add("libraryName is required");
        }
        if (StringUtils.isBlank(version)) {
            errors.add("version is required");
        }
        if (StringUtils.isBlank(content)) {
            errors.add("content must not be blank");
            return ValidationResult.failure(errors);
        }

        Matcher matcher = HEADER_PATTERN.matcher(content);
        if (!matcher.find()) {
            errors.add("content must include a library header matching the descriptor");
        } else {
            String contentName = matcher.group(1);
            String contentVersion = matcher.group(2);
            if (!contentName.equalsIgnoreCase(libraryName)) {
                errors.add("library header name '%s' does not match descriptor '%s'"
                        .formatted(contentName, libraryName));
            }
            if (!contentVersion.equals(version)) {
                errors.add("library header version '%s' does not match descriptor '%s'"
                        .formatted(contentVersion, version));
            }
        }

        if (!USING_PATTERN.matcher(content).find()) {
            warnings.add("CQL content does not declare using FHIR version – default context will be assumed");
        }

        if (errors.isEmpty()) {
            return ValidationResult.success(warnings);
        }
        return ValidationResult.failure(errors, warnings);
    }
}

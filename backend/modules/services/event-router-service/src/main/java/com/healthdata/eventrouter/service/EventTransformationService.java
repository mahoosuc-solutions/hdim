package com.healthdata.eventrouter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.eventrouter.dto.EventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EventTransformationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScriptEngine scriptEngine;

    public EventTransformationService() {
        ScriptEngineManager manager = new ScriptEngineManager();
        this.scriptEngine = manager.getEngineByName("javascript");
    }

    public EventMessage transform(EventMessage event, String transformationScript) {
        if (transformationScript == null || transformationScript.trim().isEmpty()) {
            return event;
        }

        try {
            EventMessage transformed = new EventMessage();
            transformed.setEventId(event.getEventId());
            transformed.setEventType(event.getEventType());
            transformed.setTenantId(event.getTenantId());
            transformed.setSourceTopic(event.getSourceTopic());
            transformed.setTimestamp(event.getTimestamp());
            transformed.setPayload(new HashMap<>(event.getPayload()));
            transformed.setMetadata(new HashMap<>(event.getMetadata()));

            String[] transformations = transformationScript.split("\\|");
            for (String transformation : transformations) {
                applyTransformation(transformed, transformation.trim());
            }

            return transformed;
        } catch (Exception e) {
            log.error("Error applying transformation: {}", transformationScript, e);
            return event; // Return original on error
        }
    }

    private void applyTransformation(EventMessage event, String transformation) {
        String[] parts = transformation.split(":", 2);
        if (parts.length < 1) {
            return;
        }

        String operation = parts[0];
        String params = parts.length > 1 ? parts[1] : "";

        switch (operation) {
            case "enrichment":
                applyEnrichment(event, params);
                break;
            case "rename":
                applyRename(event, params);
                break;
            case "remove":
                applyRemove(event, params);
                break;
            case "convert":
                applyConversion(event, params);
                break;
            case "mask":
                applyMasking(event, params);
                break;
            case "flatten":
                applyFlatten(event, params);
                break;
            case "js":
                applyJavaScript(event, params);
                break;
            default:
                log.warn("Unknown transformation operation: {}", operation);
        }
    }

    private void applyEnrichment(EventMessage event, String params) {
        String[] enrichments = params.split(",");
        for (String enrichment : enrichments) {
            enrichment = enrichment.trim();
            if ("add-timestamp".equals(enrichment)) {
                event.getPayload().put("enrichedAt", Instant.now().toString());
            } else if ("add-source".equals(enrichment)) {
                event.getPayload().put("source", "event-router-service");
            }
        }
    }

    private void applyRename(EventMessage event, String params) {
        String[] mappings = params.split(",");
        for (String mapping : mappings) {
            String[] parts = mapping.split("->");
            if (parts.length == 2) {
                String oldName = parts[0].trim();
                String newName = parts[1].trim();
                Object value = event.getPayload().remove(oldName);
                if (value != null) {
                    event.getPayload().put(newName, value);
                }
            }
        }
    }

    private void applyRemove(EventMessage event, String params) {
        String[] fields = params.split(",");
        for (String field : fields) {
            event.getPayload().remove(field.trim());
        }
    }

    private void applyConversion(EventMessage event, String params) {
        String[] conversions = params.split(",");
        for (String conversion : conversions) {
            String[] parts = conversion.split("->");
            if (parts.length == 2) {
                String field = parts[0].trim();
                String type = parts[1].trim();
                Object value = event.getPayload().get(field);
                if (value != null) {
                    Object converted = convertType(value, type);
                    if (converted != null) {
                        event.getPayload().put(field, converted);
                    }
                }
            }
        }
    }

    private Object convertType(Object value, String targetType) {
        try {
            String strValue = value.toString();
            switch (targetType.toLowerCase()) {
                case "integer":
                case "int":
                    return Integer.parseInt(strValue);
                case "long":
                    return Long.parseLong(strValue);
                case "double":
                case "float":
                    return Double.parseDouble(strValue);
                case "boolean":
                case "bool":
                    return Boolean.parseBoolean(strValue);
                default:
                    return value;
            }
        } catch (Exception e) {
            log.error("Error converting value to type {}: {}", targetType, value);
            return null;
        }
    }

    private void applyMasking(EventMessage event, String params) {
        String[] fields = params.split(",");
        for (String field : fields) {
            field = field.trim();
            Object value = event.getPayload().get(field);
            if (value != null) {
                String masked = maskValue(value.toString(), field);
                event.getPayload().put(field, masked);
            }
        }
    }

    private String maskValue(String value, String field) {
        if (field.toLowerCase().contains("ssn")) {
            return "***-**-****";
        } else if (field.toLowerCase().contains("email")) {
            int atIndex = value.indexOf('@');
            if (atIndex > 0) {
                return "***" + value.substring(atIndex);
            }
        }
        return "***";
    }

    private void applyFlatten(EventMessage event, String params) {
        String prefix = params.trim();
        Object value = event.getPayload().get(prefix);
        if (value instanceof Map) {
            Map<String, Object> flattened = flattenMap(prefix, (Map<?, ?>) value);
            event.getPayload().putAll(flattened);
        }
    }

    private Map<String, Object> flattenMap(String prefix, Map<?, ?> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                result.putAll(flattenMap(key, (Map<?, ?>) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private void applyJavaScript(EventMessage event, String script) {
        try {
            scriptEngine.put("payload", event.getPayload());
            scriptEngine.eval(script);
        } catch (Exception e) {
            log.error("Error executing JavaScript transformation: {}", script, e);
        }
    }
}

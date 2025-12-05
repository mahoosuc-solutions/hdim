package com.healthdata.quality.service.notification;

import java.util.Map;

/**
 * Template Renderer Interface
 *
 * Renders notification templates with variable substitution.
 * Supports multiple template engines (Thyme leaf, Mustache, etc.)
 */
public interface TemplateRenderer {

    /**
     * Render a template with the given variables
     *
     * @param templateId The template identifier (e.g., "critical-alert", "care-gap")
     * @param variables  Map of variables to substitute in the template
     * @return Rendered template as String
     */
    String render(String templateId, Map<String, Object> variables);

    /**
     * Check if a template exists
     *
     * @param templateId The template identifier
     * @return true if template exists, false otherwise
     */
    boolean templateExists(String templateId);

    /**
     * Get the default template for a notification type
     *
     * @param channel          The notification channel (EMAIL, SMS, etc.)
     * @param notificationType The notification type
     * @return Template ID
     */
    String getDefaultTemplate(String channel, String notificationType);
}

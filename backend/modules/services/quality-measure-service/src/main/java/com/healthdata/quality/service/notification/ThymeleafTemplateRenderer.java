package com.healthdata.quality.service.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.Map;

/**
 * Thymeleaf Implementation of Template Renderer
 *
 * Renders HTML email templates and SMS text templates using Thymeleaf engine.
 * Templates are loaded from src/main/resources/templates/notifications/
 *
 * Example Usage:
 * <pre>
 * Map<String, Object> variables = Map.of(
 *     "patientName", "John Doe",
 *     "alertType", "Critical Lab Result",
 *     "alertMessage", "Blood glucose level critically high"
 * );
 * String html = renderer.render("critical-alert", variables);
 * </pre>
 */
@Slf4j
@Service
public class ThymeleafTemplateRenderer implements TemplateRenderer {

    private TemplateEngine htmlTemplateEngine;
    private TemplateEngine textTemplateEngine;

    @PostConstruct
    public void initialize() {
        this.htmlTemplateEngine = createHtmlTemplateEngine();
        this.textTemplateEngine = createTextTemplateEngine();
        log.info("Initialized Thymeleaf template renderer with HTML and TEXT engines");
    }

    /**
     * Create template engine for HTML email templates
     */
    private TemplateEngine createHtmlTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/notifications/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setCacheTTLMs(3600000L); // 1 hour cache

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    /**
     * Create template engine for SMS text templates
     */
    private TemplateEngine createTextTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/notifications/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);
        resolver.setCacheTTLMs(3600000L); // 1 hour cache

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Override
    public String render(String templateId, Map<String, Object> variables) {
        try {
            // Determine template type (HTML or TEXT) based on channel
            String channel = (String) variables.getOrDefault("channel", "EMAIL");
            TemplateEngine engine = "SMS".equals(channel) ? textTemplateEngine : htmlTemplateEngine;

            Context context = new Context(Locale.getDefault(), variables);
            String result = engine.process(templateId, context);

            log.debug("Rendered template '{}' with {} variables", templateId, variables.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to render template '{}': {}", templateId, e.getMessage(), e);
            throw new TemplateRenderException("Failed to render template: " + templateId, e);
        }
    }

    @Override
    public boolean templateExists(String templateId) {
        try {
            // Try to resolve the template - if it fails, template doesn't exist
            render(templateId, Map.of("channel", "EMAIL"));
            return true;
        } catch (Exception e) {
            log.debug("Template '{}' does not exist or failed to load", templateId);
            return false;
        }
    }

    @Override
    public String getDefaultTemplate(String channel, String notificationType) {
        // Map notification types to template IDs
        return switch (notificationType) {
            case "CLINICAL_ALERT", "CRITICAL_ALERT" -> "critical-alert";
            case "CARE_GAP", "CARE_GAP_IDENTIFIED" -> "care-gap";
            case "HEALTH_SCORE_UPDATE", "HEALTH_SCORE_CHANGE" -> "health-score";
            case "APPOINTMENT_REMINDER" -> "appointment-reminder";
            case "MEDICATION_REMINDER" -> "medication-reminder";
            case "LAB_RESULT", "LAB_RESULT_AVAILABLE" -> "lab-result";
            case "DIGEST", "DAILY_DIGEST" -> "digest";
            default -> {
                log.warn("Unknown notification type '{}', using default template", notificationType);
                yield "default";
            }
        };
    }

    /**
     * Custom exception for template rendering failures
     */
    public static class TemplateRenderException extends RuntimeException {
        public TemplateRenderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

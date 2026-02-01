package com.healthdata.audit.service.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Natural language query service for AI audit events.
 * 
 * Allows users to ask questions like:
 * - "Show me all AI decisions from the last 24 hours"
 * - "What configuration changes were made to the pool size yesterday?"
 * - "Who rejected AI recommendations this week?"
 * - "Show me the audit trail for correlation ID abc-123"
 * - "What was the average confidence score for AI decisions today?"
 * 
 * Uses pattern matching to convert natural language to structured queries.
 * Can be extended with LLM integration for more complex queries.
 */
@Slf4j
@Service
public class NaturalLanguageAuditQuery {

    @Value("${audit.nlq.enable-llm:false}")
    private boolean enableLlm;

    // Time period patterns
    private static final Map<String, Duration> TIME_PERIODS = Map.of(
        "today", Duration.ofDays(0),
        "yesterday", Duration.ofDays(1),
        "this week", Duration.ofDays(7),
        "last week", Duration.ofDays(14),
        "this month", Duration.ofDays(30),
        "last 24 hours", Duration.ofHours(24),
        "last hour", Duration.ofHours(1)
    );

    // Query intent patterns
    private static final List<QueryPattern> QUERY_PATTERNS = Arrays.asList(
        // AI Decisions
        new QueryPattern(
            Pattern.compile("show (me |)(?:all |)ai decisions(?: from | during |)(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.LIST_AI_DECISIONS
        ),
        new QueryPattern(
            Pattern.compile("(?:what|which) ai (?:decisions|recommendations) (.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.LIST_AI_DECISIONS
        ),
        new QueryPattern(
            Pattern.compile("(?:show|get) (?:the |)decision chain for (.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.GET_DECISION_CHAIN
        ),
        
        // Configuration Changes
        new QueryPattern(
            Pattern.compile("(?:show|what) (?:configuration |config |)changes(?: to | for |)(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.LIST_CONFIG_CHANGES
        ),
        new QueryPattern(
            Pattern.compile("(?:show|get) (?:the |)(?:configuration |config |)history (?:for |of )(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.GET_CONFIG_HISTORY
        ),
        new QueryPattern(
            Pattern.compile("(?:show|what) (?:high[- ]risk|critical) changes(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.LIST_HIGH_RISK_CHANGES
        ),
        
        // User Actions
        new QueryPattern(
            Pattern.compile("(?:show|what) (?:user |)actions(?: by | from |)(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.LIST_USER_ACTIONS
        ),
        new QueryPattern(
            Pattern.compile("who (accepted|rejected) (?:ai |)recommendations(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.LIST_AI_FEEDBACK
        ),
        new QueryPattern(
            Pattern.compile("(?:show|get) pending approvals", Pattern.CASE_INSENSITIVE),
            QueryIntent.LIST_PENDING_APPROVALS
        ),
        
        // Analytics
        new QueryPattern(
            Pattern.compile("(?:what is |show )(?:the |)average confidence(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.ANALYTICS_CONFIDENCE
        ),
        new QueryPattern(
            Pattern.compile("(?:show|count) (?:ai |)(?:decision |)outcomes(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.ANALYTICS_OUTCOMES
        ),
        
        // Audit Trail
        new QueryPattern(
            Pattern.compile("(?:show|get) (?:the |)(?:complete |)audit trail (?:for |)(.*)", Pattern.CASE_INSENSITIVE),
            QueryIntent.GET_AUDIT_TRAIL
        )
    );

    /**
     * Parse natural language query and return structured query parameters.
     */
    public ParsedQuery parseQuery(String naturalLanguageQuery) {
        log.debug("Parsing natural language query: {}", naturalLanguageQuery);

        ParsedQuery parsed = new ParsedQuery();
        parsed.setOriginalQuery(naturalLanguageQuery);
        parsed.setTimestamp(Instant.now());

        // Match query intent
        for (QueryPattern pattern : QUERY_PATTERNS) {
            Matcher matcher = pattern.pattern.matcher(naturalLanguageQuery);
            if (matcher.find()) {
                parsed.setIntent(pattern.intent);
                parsed.setContext(matcher.groupCount() > 0 ? matcher.group(1).trim() : "");
                break;
            }
        }

        if (parsed.getIntent() == null) {
            parsed.setIntent(QueryIntent.UNKNOWN);
            log.warn("Could not determine intent for query: {}", naturalLanguageQuery);
        }

        // Extract time period
        TimeRange timeRange = extractTimeRange(naturalLanguageQuery);
        parsed.setStartTime(timeRange.startTime);
        parsed.setEndTime(timeRange.endTime);

        // Extract entities (tenant ID, user ID, service name, etc.)
        parsed.setEntities(extractEntities(naturalLanguageQuery, parsed.getContext()));

        log.debug("Parsed query: intent={}, timeRange={}days, entities={}", 
            parsed.getIntent(), 
            Duration.between(parsed.getStartTime(), parsed.getEndTime()).toDays(),
            parsed.getEntities().size());

        return parsed;
    }

    /**
     * Extract time range from query.
     */
    private TimeRange extractTimeRange(String query) {
        String lowerQuery = query.toLowerCase();
        
        // Check for specific time periods
        for (Map.Entry<String, Duration> entry : TIME_PERIODS.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                if (entry.getKey().equals("today")) {
                    Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
                    return new TimeRange(startOfToday, Instant.now());
                } else if (entry.getKey().equals("yesterday")) {
                    Instant startOfYesterday = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
                    Instant endOfYesterday = startOfYesterday.plus(1, ChronoUnit.DAYS);
                    return new TimeRange(startOfYesterday, endOfYesterday);
                } else {
                    Instant startTime = Instant.now().minus(entry.getValue());
                    return new TimeRange(startTime, Instant.now());
                }
            }
        }

        // Check for "last N hours/days/weeks"
        Pattern lastNPattern = Pattern.compile("last (\\d+) (hour|day|week)s?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = lastNPattern.matcher(query);
        if (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            
            Duration duration = switch (unit) {
                case "hour" -> Duration.ofHours(amount);
                case "day" -> Duration.ofDays(amount);
                case "week" -> Duration.ofDays(amount * 7L);
                default -> Duration.ofDays(7);
            };
            
            Instant startTime = Instant.now().minus(duration);
            return new TimeRange(startTime, Instant.now());
        }

        // Default: last 7 days
        Instant startTime = Instant.now().minus(Duration.ofDays(7));
        return new TimeRange(startTime, Instant.now());
    }

    /**
     * Extract entities (IDs, names) from query.
     */
    private Map<String, String> extractEntities(String query, String context) {
        Map<String, String> entities = new HashMap<>();

        // Extract tenant ID
        Pattern tenantPattern = Pattern.compile("tenant[- ]?id[: ](\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher tenantMatcher = tenantPattern.matcher(query);
        if (tenantMatcher.find()) {
            entities.put("tenantId", tenantMatcher.group(1));
        }

        // Extract user ID/name
        Pattern userPattern = Pattern.compile("(?:user|by) (\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher userMatcher = userPattern.matcher(context);
        if (userMatcher.find()) {
            entities.put("userId", userMatcher.group(1));
        }

        // Extract service name
        Pattern servicePattern = Pattern.compile("(?:service|in) ([\\w-]+)", Pattern.CASE_INSENSITIVE);
        Matcher serviceMatcher = servicePattern.matcher(context);
        if (serviceMatcher.find()) {
            entities.put("serviceName", serviceMatcher.group(1));
        }

        // Extract correlation ID
        Pattern correlationPattern = Pattern.compile("correlation[- ]?id[: ]([\\w-]+)", Pattern.CASE_INSENSITIVE);
        Matcher correlationMatcher = correlationPattern.matcher(query);
        if (correlationMatcher.find()) {
            entities.put("correlationId", correlationMatcher.group(1));
        }

        // Extract config key
        Pattern configPattern = Pattern.compile("(?:config |configuration |)(\\w+\\.\\w+[.\\w]*)", Pattern.CASE_INSENSITIVE);
        Matcher configMatcher = configPattern.matcher(context);
        if (configMatcher.find()) {
            entities.put("configKey", configMatcher.group(1));
        }

        return entities;
    }

    /**
     * Query intent types.
     */
    public enum QueryIntent {
        LIST_AI_DECISIONS,
        GET_DECISION_CHAIN,
        LIST_CONFIG_CHANGES,
        GET_CONFIG_HISTORY,
        LIST_HIGH_RISK_CHANGES,
        LIST_USER_ACTIONS,
        LIST_AI_FEEDBACK,
        LIST_PENDING_APPROVALS,
        ANALYTICS_CONFIDENCE,
        ANALYTICS_OUTCOMES,
        GET_AUDIT_TRAIL,
        UNKNOWN
    }

    /**
     * Parsed query result.
     */
    @Data
    public static class ParsedQuery {
        private String originalQuery;
        private QueryIntent intent;
        private Instant timestamp;
        private Instant startTime;
        private Instant endTime;
        private String context;
        private Map<String, String> entities;
    }

    /**
     * Query pattern for intent matching.
     */
    private record QueryPattern(Pattern pattern, QueryIntent intent) {}

    /**
     * Time range for queries.
     */
    private record TimeRange(Instant startTime, Instant endTime) {}
}

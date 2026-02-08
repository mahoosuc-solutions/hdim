package com.healthdata.sales.client;

import com.healthdata.sales.config.ZohoConfig;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Zoho Analytics API Client
 * Business intelligence and reporting for investor pipeline
 *
 * Features:
 * - Dashboard retrieval
 * - Report generation
 * - Data import/export
 * - KPI tracking
 * - Custom SQL queries
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ZohoAnalyticsClient {

    private final ZohoConfig zohoConfig;
    private final ZohoOAuthService oAuthService;
    private final WebClient.Builder webClientBuilder;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== Data Import ====================

    /**
     * Import data into an Analytics table
     */
    @CircuitBreaker(name = "zohoService", fallbackMethod = "importDataFallback")
    @Retry(name = "zohoService")
    public AnalyticsResult importData(String tableName, List<Map<String, Object>> rows, 
                                       ImportMode mode) {
        if (!isEnabled()) {
            return AnalyticsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> importData = new HashMap<>();
            importData.put("ZOHO_ACTION", "IMPORT");
            importData.put("ZOHO_IMPORT_TYPE", mode.getValue());
            importData.put("ZOHO_IMPORT_FILETYPE", "JSON");
            importData.put("ZOHO_AUTO_IDENTIFY", "TRUE");
            importData.put("ZOHO_DATA", rows);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + zohoConfig.getAnalytics().getOrganizationId())
                    .path("/" + zohoConfig.getAnalytics().getWorkspaceId())
                    .path("/" + tableName)
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(importData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "SUCCESS".equals(response.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                int rowsImported = result != null && result.get("importedRows") != null ? 
                    ((Number) result.get("importedRows")).intValue() : 0;
                
                log.info("Imported {} rows to Analytics table {}", rowsImported, tableName);
                return AnalyticsResult.success("Imported " + rowsImported + " rows");
            }
            
            return AnalyticsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to import data to Analytics: {}", e.getMessage());
            return AnalyticsResult.failed(e.getMessage());
        }
    }

    public AnalyticsResult importDataFallback(String tableName, List<Map<String, Object>> rows,
                                               ImportMode mode, Throwable t) {
        log.warn("Zoho Analytics import fallback triggered: {}", t.getMessage());
        return AnalyticsResult.failed("Circuit breaker open: " + t.getMessage());
    }

    // ==================== Data Export ====================

    /**
     * Export data from an Analytics table or report
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<Map<String, Object>> exportData(String viewName, String criteria, int limit) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path("/" + zohoConfig.getAnalytics().getOrganizationId())
                        .path("/" + zohoConfig.getAnalytics().getWorkspaceId())
                        .path("/" + viewName)
                        .queryParam("ZOHO_ACTION", "EXPORT")
                        .queryParam("ZOHO_OUTPUT_FORMAT", "JSON");
                    if (criteria != null) {
                        builder.queryParam("ZOHO_CRITERIA", criteria);
                    }
                    if (limit > 0) {
                        builder.queryParam("ZOHO_RECORD_LIMIT", limit);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                return data != null ? data : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to export data from Analytics: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== SQL Queries ====================

    /**
     * Execute a custom SQL query
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<Map<String, Object>> executeQuery(String sqlQuery) {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> queryData = new HashMap<>();
            queryData.put("ZOHO_ACTION", "EXECUTE_QUERY");
            queryData.put("ZOHO_SQL_QUERY", sqlQuery);
            queryData.put("ZOHO_OUTPUT_FORMAT", "JSON");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + zohoConfig.getAnalytics().getOrganizationId())
                    .path("/" + zohoConfig.getAnalytics().getWorkspaceId())
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(queryData)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rows = (List<Map<String, Object>>) data.get("rows");
                return rows != null ? rows : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to execute Analytics query: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== Dashboard Access ====================

    /**
     * Get dashboard embed URL for investor pipeline
     */
    public String getInvestorPipelineDashboardUrl() {
        return getDashboardEmbedUrl(
            zohoConfig.getAnalytics().getDashboards().getInvestorPipeline()
        );
    }

    /**
     * Get dashboard embed URL for marketing performance
     */
    public String getMarketingDashboardUrl() {
        return getDashboardEmbedUrl(
            zohoConfig.getAnalytics().getDashboards().getMarketingPerformance()
        );
    }

    /**
     * Get dashboard embed URL for sales performance
     */
    public String getSalesDashboardUrl() {
        return getDashboardEmbedUrl(
            zohoConfig.getAnalytics().getDashboards().getSalesPerformance()
        );
    }

    /**
     * Generate embeddable dashboard URL with auth token
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public String getDashboardEmbedUrl(String dashboardId) {
        if (!isEnabled() || dashboardId == null) {
            return null;
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + zohoConfig.getAnalytics().getOrganizationId())
                    .path("/" + zohoConfig.getAnalytics().getWorkspaceId())
                    .path("/dashboard/" + dashboardId + "/embedurl")
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                return (String) data.get("embedUrl");
            }
        } catch (Exception e) {
            log.error("Failed to get dashboard embed URL: {}", e.getMessage());
        }
        return null;
    }

    // ==================== KPI Queries ====================

    /**
     * Get investor pipeline summary metrics
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public PipelineMetrics getPipelineMetrics() {
        if (!isEnabled()) {
            return null;
        }

        try {
            String query = """
                SELECT 
                    Stage,
                    COUNT(*) as deal_count,
                    SUM(Amount) as total_value,
                    AVG(Amount) as avg_deal_size,
                    AVG(DATEDIFF(CURDATE(), Created_Date)) as avg_days_in_stage
                FROM Deals
                WHERE Stage NOT IN ('Closed Won', 'Closed Lost')
                GROUP BY Stage
                ORDER BY FIELD(Stage, 'Cold', 'Warm', 'Meeting', 'Proposal', 'Due Diligence')
                """;

            List<Map<String, Object>> results = executeQuery(query);
            return mapToPipelineMetrics(results);
        } catch (Exception e) {
            log.error("Failed to get pipeline metrics: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get marketing campaign performance metrics
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public MarketingMetrics getMarketingMetrics(LocalDate startDate, LocalDate endDate) {
        if (!isEnabled()) {
            return null;
        }

        try {
            String query = String.format("""
                SELECT 
                    Campaign_Name,
                    SUM(Emails_Sent) as total_sent,
                    SUM(Opens) as total_opens,
                    SUM(Clicks) as total_clicks,
                    SUM(Leads_Generated) as leads_generated,
                    AVG(Open_Rate) as avg_open_rate,
                    AVG(Click_Rate) as avg_click_rate
                FROM Campaign_Performance
                WHERE Date BETWEEN '%s' AND '%s'
                GROUP BY Campaign_Name
                """, startDate.format(DATE_FORMAT), endDate.format(DATE_FORMAT));

            List<Map<String, Object>> results = executeQuery(query);
            return mapToMarketingMetrics(results);
        } catch (Exception e) {
            log.error("Failed to get marketing metrics: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get sales team performance metrics
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public SalesMetrics getSalesMetrics(LocalDate startDate, LocalDate endDate) {
        if (!isEnabled()) {
            return null;
        }

        try {
            String query = String.format("""
                SELECT 
                    Sales_Rep,
                    COUNT(CASE WHEN Stage = 'Closed Won' THEN 1 END) as deals_closed,
                    SUM(CASE WHEN Stage = 'Closed Won' THEN Amount ELSE 0 END) as revenue,
                    AVG(CASE WHEN Stage = 'Closed Won' THEN Amount END) as avg_deal_size,
                    COUNT(*) as total_deals,
                    COUNT(CASE WHEN Stage = 'Closed Won' THEN 1 END) * 100.0 / COUNT(*) as win_rate
                FROM Deals
                WHERE Close_Date BETWEEN '%s' AND '%s'
                GROUP BY Sales_Rep
                ORDER BY revenue DESC
                """, startDate.format(DATE_FORMAT), endDate.format(DATE_FORMAT));

            List<Map<String, Object>> results = executeQuery(query);
            return mapToSalesMetrics(results);
        } catch (Exception e) {
            log.error("Failed to get sales metrics: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Get lead source ROI analysis
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<LeadSourceROI> getLeadSourceROI() {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String query = """
                SELECT 
                    Lead_Source,
                    COUNT(*) as total_leads,
                    COUNT(CASE WHEN Converted = TRUE THEN 1 END) as converted_leads,
                    SUM(CASE WHEN Converted = TRUE THEN Deal_Amount ELSE 0 END) as revenue,
                    AVG(Cost_Per_Lead) as avg_cost_per_lead,
                    SUM(CASE WHEN Converted = TRUE THEN Deal_Amount ELSE 0 END) / 
                        NULLIF(SUM(Acquisition_Cost), 0) as roi
                FROM Leads_Performance
                GROUP BY Lead_Source
                ORDER BY roi DESC
                """;

            List<Map<String, Object>> results = executeQuery(query);
            return results.stream()
                .map(this::mapToLeadSourceROI)
                .toList();
        } catch (Exception e) {
            log.error("Failed to get lead source ROI: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== Report Management ====================

    /**
     * List available reports in workspace
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public List<ReportInfo> getReports() {
        if (!isEnabled()) {
            return List.of();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + zohoConfig.getAnalytics().getOrganizationId())
                    .path("/" + zohoConfig.getAnalytics().getWorkspaceId())
                    .path("/reports")
                    .build())
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> reports = (List<Map<String, Object>>) data.get("views");
                
                return reports != null ? reports.stream()
                    .map(this::mapToReportInfo)
                    .toList() : List.of();
            }
        } catch (Exception e) {
            log.error("Failed to get reports: {}", e.getMessage());
        }
        return List.of();
    }

    // ==================== Sync From CRM ====================

    /**
     * Sync deals data from Zoho CRM to Analytics for reporting
     */
    @CircuitBreaker(name = "zohoService")
    @Retry(name = "zohoService")
    public AnalyticsResult syncDealsFromCRM() {
        if (!isEnabled()) {
            return AnalyticsResult.disabled();
        }

        try {
            String accessToken = oAuthService.getAccessToken();
            WebClient webClient = buildWebClient(accessToken);

            Map<String, Object> syncConfig = new HashMap<>();
            syncConfig.put("ZOHO_ACTION", "SYNC");
            syncConfig.put("ZOHO_SYNC_TYPE", "FULL");
            syncConfig.put("ZOHO_SOURCE", "ZOHOCRM");
            syncConfig.put("ZOHO_MODULE", "Deals");

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/" + zohoConfig.getAnalytics().getOrganizationId())
                    .path("/" + zohoConfig.getAnalytics().getWorkspaceId())
                    .path("/sync")
                    .build())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(syncConfig)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (response != null && "SUCCESS".equals(response.get("status"))) {
                log.info("Triggered CRM deals sync to Analytics");
                return AnalyticsResult.success("Sync initiated");
            }
            
            return AnalyticsResult.failed(response != null ? 
                (String) response.get("message") : "Unknown error");
        } catch (Exception e) {
            log.error("Failed to sync deals from CRM: {}", e.getMessage());
            return AnalyticsResult.failed(e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private boolean isEnabled() {
        return zohoConfig.getAnalytics().isEnabled() && oAuthService.isConfigured();
    }

    private WebClient buildWebClient(String accessToken) {
        return webClientBuilder
            .baseUrl(zohoConfig.getAnalytics().getBaseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken)
            .build();
    }

    private PipelineMetrics mapToPipelineMetrics(List<Map<String, Object>> results) {
        PipelineMetrics metrics = new PipelineMetrics();
        List<StageMetric> stages = new ArrayList<>();
        
        double totalValue = 0;
        int totalDeals = 0;
        
        for (Map<String, Object> row : results) {
            StageMetric stage = new StageMetric();
            stage.setName((String) row.get("Stage"));
            stage.setDealCount(getIntValue(row, "deal_count"));
            stage.setTotalValue(getDoubleValue(row, "total_value"));
            stage.setAverageDealSize(getDoubleValue(row, "avg_deal_size"));
            stage.setAvgDaysInStage(getIntValue(row, "avg_days_in_stage"));
            stages.add(stage);
            
            totalValue += stage.getTotalValue();
            totalDeals += stage.getDealCount();
        }
        
        metrics.setStages(stages);
        metrics.setTotalPipelineValue(totalValue);
        metrics.setTotalDeals(totalDeals);
        metrics.setAverageDealSize(totalDeals > 0 ? totalValue / totalDeals : 0);
        
        return metrics;
    }

    private MarketingMetrics mapToMarketingMetrics(List<Map<String, Object>> results) {
        MarketingMetrics metrics = new MarketingMetrics();
        List<CampaignMetric> campaigns = new ArrayList<>();
        
        int totalSent = 0;
        int totalOpens = 0;
        int totalClicks = 0;
        int totalLeads = 0;
        
        for (Map<String, Object> row : results) {
            CampaignMetric campaign = new CampaignMetric();
            campaign.setName((String) row.get("Campaign_Name"));
            campaign.setTotalSent(getIntValue(row, "total_sent"));
            campaign.setTotalOpens(getIntValue(row, "total_opens"));
            campaign.setTotalClicks(getIntValue(row, "total_clicks"));
            campaign.setLeadsGenerated(getIntValue(row, "leads_generated"));
            campaign.setOpenRate(getDoubleValue(row, "avg_open_rate"));
            campaign.setClickRate(getDoubleValue(row, "avg_click_rate"));
            campaigns.add(campaign);
            
            totalSent += campaign.getTotalSent();
            totalOpens += campaign.getTotalOpens();
            totalClicks += campaign.getTotalClicks();
            totalLeads += campaign.getLeadsGenerated();
        }
        
        metrics.setCampaigns(campaigns);
        metrics.setTotalEmailsSent(totalSent);
        metrics.setTotalOpens(totalOpens);
        metrics.setTotalClicks(totalClicks);
        metrics.setTotalLeadsGenerated(totalLeads);
        metrics.setOverallOpenRate(totalSent > 0 ? (double) totalOpens / totalSent * 100 : 0);
        metrics.setOverallClickRate(totalSent > 0 ? (double) totalClicks / totalSent * 100 : 0);
        
        return metrics;
    }

    private SalesMetrics mapToSalesMetrics(List<Map<String, Object>> results) {
        SalesMetrics metrics = new SalesMetrics();
        List<SalesRepMetric> reps = new ArrayList<>();
        
        double totalRevenue = 0;
        int totalDeals = 0;
        
        for (Map<String, Object> row : results) {
            SalesRepMetric rep = new SalesRepMetric();
            rep.setName((String) row.get("Sales_Rep"));
            rep.setDealsClosed(getIntValue(row, "deals_closed"));
            rep.setRevenue(getDoubleValue(row, "revenue"));
            rep.setAverageDealSize(getDoubleValue(row, "avg_deal_size"));
            rep.setTotalDeals(getIntValue(row, "total_deals"));
            rep.setWinRate(getDoubleValue(row, "win_rate"));
            reps.add(rep);
            
            totalRevenue += rep.getRevenue();
            totalDeals += rep.getDealsClosed();
        }
        
        metrics.setSalesReps(reps);
        metrics.setTotalRevenue(totalRevenue);
        metrics.setTotalDealsClosed(totalDeals);
        metrics.setAverageDealSize(totalDeals > 0 ? totalRevenue / totalDeals : 0);
        
        return metrics;
    }

    private LeadSourceROI mapToLeadSourceROI(Map<String, Object> row) {
        LeadSourceROI roi = new LeadSourceROI();
        roi.setSource((String) row.get("Lead_Source"));
        roi.setTotalLeads(getIntValue(row, "total_leads"));
        roi.setConvertedLeads(getIntValue(row, "converted_leads"));
        roi.setRevenue(getDoubleValue(row, "revenue"));
        roi.setAvgCostPerLead(getDoubleValue(row, "avg_cost_per_lead"));
        roi.setRoi(getDoubleValue(row, "roi"));
        roi.setConversionRate(roi.getTotalLeads() > 0 ? 
            (double) roi.getConvertedLeads() / roi.getTotalLeads() * 100 : 0);
        return roi;
    }

    private ReportInfo mapToReportInfo(Map<String, Object> data) {
        ReportInfo info = new ReportInfo();
        info.setReportId((String) data.get("viewId"));
        info.setName((String) data.get("viewName"));
        info.setType((String) data.get("viewType"));
        info.setDescription((String) data.get("description"));
        return info;
    }

    private int getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private double getDoubleValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }

    // ==================== Enums ====================

    public enum ImportMode {
        APPEND("APPEND"),
        TRUNCATE_ADD("TRUNCATEADD"),
        UPDATE_ADD("UPDATEADD");

        private final String value;

        ImportMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // ==================== DTOs ====================

    @Data
    public static class AnalyticsResult {
        private boolean success;
        private boolean skipped;
        private String message;

        public static AnalyticsResult success(String message) {
            AnalyticsResult result = new AnalyticsResult();
            result.success = true;
            result.message = message;
            return result;
        }

        public static AnalyticsResult failed(String error) {
            AnalyticsResult result = new AnalyticsResult();
            result.success = false;
            result.message = error;
            return result;
        }

        public static AnalyticsResult disabled() {
            AnalyticsResult result = new AnalyticsResult();
            result.skipped = true;
            result.message = "Zoho Analytics is disabled";
            return result;
        }
    }

    @Data
    public static class PipelineMetrics {
        private List<StageMetric> stages;
        private double totalPipelineValue;
        private int totalDeals;
        private double averageDealSize;
    }

    @Data
    public static class StageMetric {
        private String name;
        private int dealCount;
        private double totalValue;
        private double averageDealSize;
        private int avgDaysInStage;
    }

    @Data
    public static class MarketingMetrics {
        private List<CampaignMetric> campaigns;
        private int totalEmailsSent;
        private int totalOpens;
        private int totalClicks;
        private int totalLeadsGenerated;
        private double overallOpenRate;
        private double overallClickRate;
    }

    @Data
    public static class CampaignMetric {
        private String name;
        private int totalSent;
        private int totalOpens;
        private int totalClicks;
        private int leadsGenerated;
        private double openRate;
        private double clickRate;
    }

    @Data
    public static class SalesMetrics {
        private List<SalesRepMetric> salesReps;
        private double totalRevenue;
        private int totalDealsClosed;
        private double averageDealSize;
    }

    @Data
    public static class SalesRepMetric {
        private String name;
        private int dealsClosed;
        private double revenue;
        private double averageDealSize;
        private int totalDeals;
        private double winRate;
    }

    @Data
    public static class LeadSourceROI {
        private String source;
        private int totalLeads;
        private int convertedLeads;
        private double revenue;
        private double avgCostPerLead;
        private double roi;
        private double conversionRate;
    }

    @Data
    public static class ReportInfo {
        private String reportId;
        private String name;
        private String type;
        private String description;
    }
}

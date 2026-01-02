package com.healthdata.cms.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Query Optimization Analyzer
 * Provides tools for analyzing and optimizing database query performance:
 * - Index usage analysis
 * - Query execution time measurement
 * - Sequential scan detection
 * - Index efficiency metrics
 *
 * Used to identify optimization opportunities and validate index effectiveness
 */
@Slf4j
@Service
public class QueryOptimizationAnalyzer {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Analyze index usage statistics
     */
    public IndexAnalysisReport analyzeIndexUsage() {
        try {
            String sql = """
                SELECT
                    schemaname,
                    tablename,
                    indexname,
                    idx_scan,
                    idx_tup_read,
                    idx_tup_fetch,
                    pg_size_pretty(pg_relation_size(indexrelid)) as index_size
                FROM pg_stat_user_indexes
                WHERE schemaname = 'public'
                ORDER BY idx_scan DESC
                """;

            List<IndexAnalysisReport.IndexStatistic> stats = new ArrayList<>();

            jdbcTemplate.query(sql, rs -> {
                IndexAnalysisReport.IndexStatistic stat = IndexAnalysisReport.IndexStatistic.builder()
                    .indexName(rs.getString("indexname"))
                    .tableName(rs.getString("tablename"))
                    .scanCount(rs.getLong("idx_scan"))
                    .tuplesRead(rs.getLong("idx_tup_read"))
                    .tuplesFetched(rs.getLong("idx_tup_fetch"))
                    .indexSize(rs.getString("index_size"))
                    .build();
                stats.add(stat);
            });

            // Calculate efficiency metrics
            List<IndexAnalysisReport.IndexEfficiency> efficiencies = stats.stream()
                .map(stat -> {
                    double efficiency = stat.getTuplesRead() > 0 ?
                        (double) stat.getTuplesFetched() / stat.getTuplesRead() : 0;
                    return IndexAnalysisReport.IndexEfficiency.builder()
                        .indexName(stat.getIndexName())
                        .scanCount(stat.getScanCount())
                        .efficiency(efficiency)
                        .recommendation(getIndexRecommendation(stat, efficiency))
                        .build();
                })
                .collect(Collectors.toList());

            return IndexAnalysisReport.builder()
                .timestamp(System.currentTimeMillis())
                .totalIndexes(stats.size())
                .indexStatistics(stats)
                .efficiencies(efficiencies)
                .build();

        } catch (Exception e) {
            log.error("Failed to analyze index usage", e);
            return IndexAnalysisReport.builder()
                .timestamp(System.currentTimeMillis())
                .totalIndexes(0)
                .indexStatistics(new ArrayList<>())
                .efficiencies(new ArrayList<>())
                .build();
        }
    }

    /**
     * Detect sequential scans (full table scans)
     */
    public SequentialScanReport detectSequentialScans() {
        try {
            String sql = """
                SELECT
                    schemaname,
                    tablename,
                    seq_scan,
                    seq_tup_read,
                    idx_scan,
                    n_live_tup,
                    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as table_size
                FROM pg_stat_user_tables
                WHERE schemaname = 'public'
                ORDER BY seq_scan DESC
                """;

            List<SequentialScanReport.SeqScanStatistic> stats = new ArrayList<>();

            jdbcTemplate.query(sql, rs -> {
                long seqScans = rs.getLong("seq_scan");
                long idxScans = rs.getLong("idx_scan");
                long totalScans = seqScans + idxScans;

                double seqScanPercent = totalScans > 0 ?
                    (seqScans * 100.0) / totalScans : 0;

                SequentialScanReport.SeqScanStatistic stat = SequentialScanReport.SeqScanStatistic.builder()
                    .tableName(rs.getString("tablename"))
                    .sequentialScans(seqScans)
                    .indexScans(idxScans)
                    .sequentialScanPercent(seqScanPercent)
                    .liveRows(rs.getLong("n_live_tup"))
                    .tableSize(rs.getString("table_size"))
                    .needsIndex(seqScanPercent > 50 && seqScans > 100) // High seq scan rate
                    .build();
                stats.add(stat);
            });

            List<SequentialScanReport.SeqScanStatistic> problematicTables = stats.stream()
                .filter(SequentialScanReport.SeqScanStatistic::isNeedsIndex)
                .collect(Collectors.toList());

            return SequentialScanReport.builder()
                .timestamp(System.currentTimeMillis())
                .totalTables(stats.size())
                .allScans(stats)
                .problematicTables(problematicTables)
                .build();

        } catch (Exception e) {
            log.error("Failed to detect sequential scans", e);
            return SequentialScanReport.builder()
                .timestamp(System.currentTimeMillis())
                .totalTables(0)
                .allScans(new ArrayList<>())
                .problematicTables(new ArrayList<>())
                .build();
        }
    }

    /**
     * Analyze query execution plans for a given query
     */
    public ExecutionPlanAnalysis analyzeExecutionPlan(String query) {
        try {
            String explainQuery = "EXPLAIN ANALYZE " + query;
            List<String> plan = new ArrayList<>();

            jdbcTemplate.query(explainQuery, rs -> {
                plan.add(rs.getString(1));
            });

            boolean hasSeqScan = plan.stream()
                .anyMatch(line -> line.contains("Seq Scan"));
            boolean hasIndex = plan.stream()
                .anyMatch(line -> line.contains("Index"));

            return ExecutionPlanAnalysis.builder()
                .query(query)
                .hasSequentialScan(hasSeqScan)
                .hasIndexUsage(hasIndex)
                .executionPlan(String.join("\n", plan))
                .recommendation(getExecutionPlanRecommendation(hasSeqScan, hasIndex))
                .build();

        } catch (Exception e) {
            log.error("Failed to analyze execution plan", e);
            return ExecutionPlanAnalysis.builder()
                .query(query)
                .recommendation("Error analyzing plan: " + e.getMessage())
                .build();
        }
    }

    /**
     * Measure query execution time
     */
    public QueryPerformanceMeasurement measureQueryPerformance(String query, int iterations) {
        try {
            List<Long> executionTimes = new ArrayList<>();

            for (int i = 0; i < iterations; i++) {
                long startTime = System.nanoTime();
                jdbcTemplate.query(query, rs -> {
                    // Just iterate through results
                });
                long endTime = System.nanoTime();
                executionTimes.add((endTime - startTime) / 1_000_000); // Convert to ms
            }

            Collections.sort(executionTimes);
            double avgTime = executionTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);
            double percentile95 = executionTimes.get((int) (executionTimes.size() * 0.95));
            double percentile99 = executionTimes.get((int) (executionTimes.size() * 0.99));

            return QueryPerformanceMeasurement.builder()
                .query(query)
                .iterations(iterations)
                .minTimeMs(executionTimes.get(0))
                .maxTimeMs(executionTimes.get(executionTimes.size() - 1))
                .avgTimeMs(avgTime)
                .percentile95Ms(percentile95)
                .percentile99Ms(percentile99)
                .build();

        } catch (Exception e) {
            log.error("Failed to measure query performance", e);
            return QueryPerformanceMeasurement.builder()
                .query(query)
                .iterations(iterations)
                .build();
        }
    }

    /**
     * Generate optimization recommendations
     */
    public OptimizationRecommendations generateRecommendations() {
        IndexAnalysisReport indexReport = analyzeIndexUsage();
        SequentialScanReport seqReport = detectSequentialScans();

        List<String> recommendations = new ArrayList<>();

        // Index recommendations
        indexReport.getEfficiencies().stream()
            .filter(e -> e.getEfficiency() < 0.5 && e.getScanCount() > 10)
            .forEach(e -> recommendations.add(
                "Consider dropping unused index: " + e.getIndexName() +
                    " (efficiency: " + String.format("%.2f", e.getEfficiency()) + ")"
            ));

        // Sequential scan recommendations
        seqReport.getProblematicTables().forEach(table ->
            recommendations.add(
                "Add index for table: " + table.getTableName() +
                    " (seq scans: " + table.getSequentialScans() +
                    ", " + String.format("%.1f", table.getSequentialScanPercent()) + "%)"
            )
        );

        return OptimizationRecommendations.builder()
            .timestamp(System.currentTimeMillis())
            .indexReport(indexReport)
            .seqScanReport(seqReport)
            .recommendations(recommendations)
            .build();
    }

    // ========== Helper Methods ==========

    private String getIndexRecommendation(IndexAnalysisReport.IndexStatistic stat, double efficiency) {
        if (stat.getScanCount() == 0) {
            return "UNUSED - Consider dropping";
        }
        if (efficiency < 0.5) {
            return "LOW EFFICIENCY - Review index definition";
        }
        if (efficiency > 0.9) {
            return "EXCELLENT - Index is highly efficient";
        }
        return "GOOD - Index performing well";
    }

    private String getExecutionPlanRecommendation(boolean hasSeqScan, boolean hasIndex) {
        if (hasSeqScan && !hasIndex) {
            return "CRITICAL - Full table scan detected. Consider adding indexes on filter conditions.";
        }
        if (hasSeqScan && hasIndex) {
            return "WARNING - Sequential scan used despite available indexes. Verify join conditions.";
        }
        if (hasIndex) {
            return "GOOD - Query uses index efficiently.";
        }
        return "NEUTRAL - Simple query without complex filter conditions.";
    }

    // ========== DTOs ==========

    @Data
    @Builder
    public static class IndexAnalysisReport {
        private long timestamp;
        private int totalIndexes;
        private List<IndexStatistic> indexStatistics;
        private List<IndexEfficiency> efficiencies;

        @Data
        @Builder
        public static class IndexStatistic {
            private String indexName;
            private String tableName;
            private long scanCount;
            private long tuplesRead;
            private long tuplesFetched;
            private String indexSize;
        }

        @Data
        @Builder
        public static class IndexEfficiency {
            private String indexName;
            private long scanCount;
            private double efficiency;
            private String recommendation;
        }
    }

    @Data
    @Builder
    public static class SequentialScanReport {
        private long timestamp;
        private int totalTables;
        private List<SeqScanStatistic> allScans;
        private List<SeqScanStatistic> problematicTables;

        @Data
        @Builder
        public static class SeqScanStatistic {
            private String tableName;
            private long sequentialScans;
            private long indexScans;
            private double sequentialScanPercent;
            private long liveRows;
            private String tableSize;
            private boolean needsIndex;
        }
    }

    @Data
    @Builder
    public static class ExecutionPlanAnalysis {
        private String query;
        private boolean hasSequentialScan;
        private boolean hasIndexUsage;
        private String executionPlan;
        private String recommendation;
    }

    @Data
    @Builder
    public static class QueryPerformanceMeasurement {
        private String query;
        private int iterations;
        private long minTimeMs;
        private long maxTimeMs;
        private double avgTimeMs;
        private double percentile95Ms;
        private double percentile99Ms;
    }

    @Data
    @Builder
    public static class OptimizationRecommendations {
        private long timestamp;
        private IndexAnalysisReport indexReport;
        private SequentialScanReport seqScanReport;
        private List<String> recommendations;
    }
}

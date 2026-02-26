package com.healthdata.gateway.admin.operations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OperationsSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public OperationsSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureSchema();
    }

    void ensureSchema() {
        log.info("Ensuring operations schema exists");

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS operation_runs (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                operation_type VARCHAR(30) NOT NULL,
                status VARCHAR(20) NOT NULL,
                parameters_json JSONB,
                requested_by VARCHAR(255) NOT NULL,
                requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                started_at TIMESTAMP WITH TIME ZONE,
                completed_at TIMESTAMP WITH TIME ZONE,
                summary TEXT,
                exit_code INT,
                log_output TEXT
            )
            """);

        jdbcTemplate.execute("ALTER TABLE operation_runs ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128)");
        jdbcTemplate.execute("ALTER TABLE operation_runs ADD COLUMN IF NOT EXISTS cancel_requested BOOLEAN NOT NULL DEFAULT FALSE");

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_operation_runs_requested_at
            ON operation_runs(requested_at DESC)
            """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_operation_runs_status
            ON operation_runs(status)
            """);

        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uq_operation_runs_type_idempotency
            ON operation_runs(operation_type, idempotency_key)
            WHERE idempotency_key IS NOT NULL
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS operation_run_steps (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                run_id UUID NOT NULL REFERENCES operation_runs(id) ON DELETE CASCADE,
                step_order INT NOT NULL,
                step_name VARCHAR(120) NOT NULL,
                status VARCHAR(20) NOT NULL,
                command_text TEXT,
                message TEXT,
                output TEXT,
                started_at TIMESTAMP WITH TIME ZONE,
                completed_at TIMESTAMP WITH TIME ZONE,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_operation_run_steps_run
            ON operation_run_steps(run_id, step_order)
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS operation_validation_runs (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                operation_run_id UUID NOT NULL REFERENCES operation_runs(id) ON DELETE CASCADE,
                score INT NOT NULL,
                grade VARCHAR(2) NOT NULL,
                critical_pass BOOLEAN NOT NULL,
                passed BOOLEAN NOT NULL,
                summary_json TEXT,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);

        jdbcTemplate.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uq_operation_validation_runs_run
            ON operation_validation_runs(operation_run_id)
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS operation_validation_gates (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                validation_run_id UUID NOT NULL REFERENCES operation_validation_runs(id) ON DELETE CASCADE,
                gate_key VARCHAR(64) NOT NULL,
                gate_name VARCHAR(120) NOT NULL,
                critical BOOLEAN NOT NULL,
                weight INT NOT NULL,
                status VARCHAR(16) NOT NULL,
                actual_value TEXT,
                expected_value TEXT,
                evidence_text TEXT,
                measured_at TIMESTAMP WITH TIME ZONE NOT NULL,
                created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
            """);

        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS idx_operation_validation_gates_run
            ON operation_validation_gates(validation_run_id, gate_key)
            """);

        log.info("Operations schema migration complete");
    }
}

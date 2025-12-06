package com.healthdata.persistence.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routing DataSource that directs read-only transactions to replica databases
 * and read-write transactions to the primary database.
 *
 * This supports horizontal read scaling by distributing read queries across
 * multiple PostgreSQL replicas while ensuring writes go to the primary.
 *
 * Usage:
 * - Methods annotated with @Transactional(readOnly = true) route to replicas
 * - All other transactions route to the primary
 *
 * HIPAA Compliance Note:
 * This routing is transparent to the application and maintains all audit logging
 * and encryption requirements regardless of which database handles the query.
 */
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    public enum DataSourceType {
        PRIMARY,
        REPLICA
    }

    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

        if (isReadOnly) {
            return DataSourceType.REPLICA;
        }

        return DataSourceType.PRIMARY;
    }
}

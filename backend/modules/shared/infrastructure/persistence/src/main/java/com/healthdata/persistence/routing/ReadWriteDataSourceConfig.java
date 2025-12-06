package com.healthdata.persistence.routing;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for read/write routing between primary and replica databases.
 *
 * When enabled (datasource.routing.enabled=true), this configuration creates:
 * - Primary DataSource: For all write operations
 * - Replica DataSource: For read-only operations
 * - Routing DataSource: Automatically routes based on transaction type
 *
 * Enable in application.yml:
 * <pre>
 * datasource:
 *   routing:
 *     enabled: true
 *   primary:
 *     url: jdbc:postgresql://primary:5432/db
 *     username: user
 *     password: pass
 *   replica:
 *     url: jdbc:postgresql://replica:5432/db
 *     username: user
 *     password: pass
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "datasource.routing.enabled", havingValue = "true")
public class ReadWriteDataSourceConfig {

    /**
     * Primary DataSource for write operations.
     * Connects to the PostgreSQL primary server.
     */
    @Bean
    @ConfigurationProperties(prefix = "datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * Replica DataSource for read operations.
     * Connects to the PostgreSQL replica server(s).
     *
     * Note: For multiple replicas, consider using a load balancer
     * or implementing round-robin selection in a custom DataSource.
     */
    @Bean
    @ConfigurationProperties(prefix = "datasource.replica")
    public DataSource replicaDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    /**
     * Routing DataSource that determines which DataSource to use
     * based on the current transaction's read-only status.
     */
    @Bean
    public DataSource routingDataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource) {

        ReadWriteRoutingDataSource routingDataSource = new ReadWriteRoutingDataSource();

        Map<Object, Object> dataSourceMap = new HashMap<>();
        dataSourceMap.put(ReadWriteRoutingDataSource.DataSourceType.PRIMARY, primaryDataSource);
        dataSourceMap.put(ReadWriteRoutingDataSource.DataSourceType.REPLICA, replicaDataSource);

        routingDataSource.setTargetDataSources(dataSourceMap);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);

        return routingDataSource;
    }

    /**
     * Primary DataSource bean that wraps the routing DataSource.
     * Uses LazyConnectionDataSourceProxy to defer connection acquisition
     * until the transaction type is known.
     */
    @Primary
    @Bean
    public DataSource dataSource(@Qualifier("routingDataSource") DataSource routingDataSource) {
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }
}

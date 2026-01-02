package com.healthdata.messaging;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for Kafka messaging.
 */
@Validated
@ConfigurationProperties(prefix = "healthdata.messaging")
public class MessagingProperties {

    @NotBlank
    private String bootstrapServers = "localhost:9092";

    @NotBlank
    private String clientId = "healthdata-platform";

    @Valid
    private final Producer producer = new Producer();

    @Valid
    private final Consumer consumer = new Consumer();

    @Valid
    private final List<Topic> topics = new ArrayList<>();

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Producer getProducer() {
        return producer;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public static class Producer {

        @NotBlank
        private String acks = "all";

        @Min(0)
        private int retries = 3;

        @Min(1)
        private int batchSize = 16_384;

        private Duration linger = Duration.ofMillis(10);

        private boolean idempotent = true;

        private int maxInFlight = 5;

        @NotBlank
        private String compressionType = "snappy";

        public String getAcks() {
            return acks;
        }

        public void setAcks(String acks) {
            this.acks = acks;
        }

        public int getRetries() {
            return retries;
        }

        public void setRetries(int retries) {
            this.retries = retries;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getLinger() {
            return linger;
        }

        public void setLinger(Duration linger) {
            this.linger = linger;
        }

        public boolean isIdempotent() {
            return idempotent;
        }

        public void setIdempotent(boolean idempotent) {
            this.idempotent = idempotent;
        }

        public int getMaxInFlight() {
            return maxInFlight;
        }

        public void setMaxInFlight(int maxInFlight) {
            this.maxInFlight = maxInFlight;
        }

        public String getCompressionType() {
            return compressionType;
        }

        public void setCompressionType(String compressionType) {
            this.compressionType = compressionType;
        }
    }

    public static class Consumer {

        @NotBlank
        private String groupId = "healthdata-consumer";

        @NotBlank
        private String autoOffsetReset = "earliest";

        private boolean enableAutoCommit = false;

        private Duration pollTimeout = Duration.ofSeconds(1);

        private List<String> trustedPackages = new ArrayList<>(Arrays.asList("com.healthdata", "java.util"));

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getAutoOffsetReset() {
            return autoOffsetReset;
        }

        public void setAutoOffsetReset(String autoOffsetReset) {
            this.autoOffsetReset = autoOffsetReset;
        }

        public boolean isEnableAutoCommit() {
            return enableAutoCommit;
        }

        public void setEnableAutoCommit(boolean enableAutoCommit) {
            this.enableAutoCommit = enableAutoCommit;
        }

        public Duration getPollTimeout() {
            return pollTimeout;
        }

        public void setPollTimeout(Duration pollTimeout) {
            this.pollTimeout = pollTimeout;
        }

        public List<String> getTrustedPackages() {
            return trustedPackages;
        }

        public void setTrustedPackages(List<String> trustedPackages) {
            this.trustedPackages = trustedPackages;
        }
    }

    public static class Topic {

        @NotBlank
        private String name;

        @Min(1)
        private int partitions = 1;

        @Min(1)
        private int replicas = 1;

        private boolean compacted = false;

        private Duration retention = Duration.ofDays(7);

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPartitions() {
            return partitions;
        }

        public void setPartitions(int partitions) {
            this.partitions = partitions;
        }

        public int getReplicas() {
            return replicas;
        }

        public void setReplicas(int replicas) {
            this.replicas = replicas;
        }

        public boolean isCompacted() {
            return compacted;
        }

        public void setCompacted(boolean compacted) {
            this.compacted = compacted;
        }

        public Duration getRetention() {
            return retention;
        }

        public void setRetention(Duration retention) {
            this.retention = retention;
        }
    }
}

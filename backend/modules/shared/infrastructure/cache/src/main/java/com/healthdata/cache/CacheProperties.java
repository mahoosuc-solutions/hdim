package com.healthdata.cache;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for Redis caching.
 */
@Validated
@ConfigurationProperties(prefix = "healthdata.cache")
public class CacheProperties {

    private final Redis redis = new Redis();
    private final Map<String, CacheSpec> caches = new LinkedHashMap<>();

    public Redis getRedis() {
        return redis;
    }

    public Map<String, CacheSpec> getCaches() {
        return caches;
    }

    public static class Redis {

        @NotBlank
        private String host = "${REDIS_HOST:redis}";

        @Min(1)
        private int port = 6379;

        private String password;

        private boolean ssl = false;

        private Duration timeout = Duration.ofSeconds(2);

        private Duration defaultTtl = Duration.ofMinutes(5);

        private boolean cacheNulls = false;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isSsl() {
            return ssl;
        }

        public void setSsl(boolean ssl) {
            this.ssl = ssl;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public boolean isCacheNulls() {
            return cacheNulls;
        }

        public void setCacheNulls(boolean cacheNulls) {
            this.cacheNulls = cacheNulls;
        }
    }

    public static class CacheSpec {
        private Duration ttl;
        private Boolean cacheNulls;

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public Boolean getCacheNulls() {
            return cacheNulls;
        }

        public void setCacheNulls(Boolean cacheNulls) {
            this.cacheNulls = cacheNulls;
        }
    }
}

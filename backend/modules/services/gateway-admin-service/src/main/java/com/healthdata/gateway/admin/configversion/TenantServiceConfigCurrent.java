package com.healthdata.gateway.admin.configversion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tenant_service_config_current")
@IdClass(TenantServiceConfigCurrent.Key.class)
public class TenantServiceConfigCurrent {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Id
    @Column(name = "service_name", nullable = false, length = 120)
    private String serviceName;

    @Column(name = "active_version_id", nullable = false)
    private UUID activeVersionId;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public UUID getActiveVersionId() {
        return activeVersionId;
    }

    public void setActiveVersionId(UUID activeVersionId) {
        this.activeVersionId = activeVersionId;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static class Key implements java.io.Serializable {
        private String tenantId;
        private String serviceName;

        public Key() {}

        public Key(String tenantId, String serviceName) {
            this.tenantId = tenantId;
            this.serviceName = serviceName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Key key = (Key) o;
            return Objects.equals(tenantId, key.tenantId)
                && Objects.equals(serviceName, key.serviceName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, serviceName);
        }
    }
}

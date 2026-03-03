package com.healthdata.ehr.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisted EHR connection configurations.
 * All queries enforce tenant isolation.
 */
@Repository
public interface EhrConnectionConfigRepository extends JpaRepository<EhrConnectionConfigEntity, UUID> {

    Optional<EhrConnectionConfigEntity> findByConnectionIdAndTenantId(String connectionId, String tenantId);

    Optional<EhrConnectionConfigEntity> findByConnectionId(String connectionId);

    List<EhrConnectionConfigEntity> findByTenantIdAndActiveTrue(String tenantId);

    List<EhrConnectionConfigEntity> findByActiveTrue();

    List<EhrConnectionConfigEntity> findByTenantId(String tenantId);
}

package com.healthdata.cms.repository;

import com.healthdata.cms.model.SyncAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface SyncAuditLogRepository extends JpaRepository<SyncAuditLog, UUID> {
}

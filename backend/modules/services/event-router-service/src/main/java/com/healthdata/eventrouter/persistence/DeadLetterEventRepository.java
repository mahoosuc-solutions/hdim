package com.healthdata.eventrouter.persistence;

import com.healthdata.eventrouter.entity.DeadLetterEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEventEntity, Long> {

    @Query("SELECT COUNT(e) FROM DeadLetterEventEntity e WHERE e.tenantId = :tenantId")
    long countByTenantId(String tenantId);

    @Query("SELECT COUNT(e) FROM DeadLetterEventEntity e")
    long countAll();
}

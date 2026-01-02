package com.healthdata.eventrouter.persistence;

import com.healthdata.eventrouter.entity.RoutingRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoutingRuleRepository extends JpaRepository<RoutingRuleEntity, Long> {

    List<RoutingRuleEntity> findByTenantIdAndSourceTopicAndEnabledTrue(String tenantId, String sourceTopic);

    List<RoutingRuleEntity> findByTenantIdAndEnabledTrue(String tenantId);

    List<RoutingRuleEntity> findBySourceTopicAndEnabledTrue(String sourceTopic);
}

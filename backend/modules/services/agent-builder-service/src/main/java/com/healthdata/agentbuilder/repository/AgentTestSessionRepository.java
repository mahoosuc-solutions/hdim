package com.healthdata.agentbuilder.repository;

import com.healthdata.agentbuilder.domain.entity.AgentTestSession;
import com.healthdata.agentbuilder.domain.entity.AgentTestSession.TestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentTestSessionRepository extends JpaRepository<AgentTestSession, UUID> {

    Page<AgentTestSession> findByAgentConfigurationId(UUID agentConfigurationId, Pageable pageable);

    Page<AgentTestSession> findByTenantId(String tenantId, Pageable pageable);

    Optional<AgentTestSession> findByIdAndTenantId(UUID id, String tenantId);

    List<AgentTestSession> findByAgentConfigurationIdAndStatus(UUID agentConfigurationId, TestStatus status);

    long countByAgentConfigurationId(UUID agentConfigurationId);

    long countByAgentConfigurationIdAndStatus(UUID agentConfigurationId, TestStatus status);
}

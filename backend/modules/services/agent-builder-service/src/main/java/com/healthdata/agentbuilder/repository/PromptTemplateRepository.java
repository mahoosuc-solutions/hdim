package com.healthdata.agentbuilder.repository;

import com.healthdata.agentbuilder.domain.entity.PromptTemplate;
import com.healthdata.agentbuilder.domain.entity.PromptTemplate.TemplateCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    Page<PromptTemplate> findByTenantId(String tenantId, Pageable pageable);

    Page<PromptTemplate> findByTenantIdAndCategory(String tenantId, TemplateCategory category, Pageable pageable);

    Optional<PromptTemplate> findByTenantIdAndId(String tenantId, UUID id);

    Optional<PromptTemplate> findByTenantIdAndName(String tenantId, String name);

    boolean existsByTenantIdAndName(String tenantId, String name);

    @Query("SELECT t FROM PromptTemplate t WHERE t.tenantId = :tenantId OR t.isSystem = true ORDER BY t.category, t.name")
    List<PromptTemplate> findAvailableTemplates(@Param("tenantId") String tenantId);

    @Query("SELECT t FROM PromptTemplate t WHERE (t.tenantId = :tenantId OR t.isSystem = true) AND t.category = :category ORDER BY t.name")
    List<PromptTemplate> findAvailableTemplatesByCategory(@Param("tenantId") String tenantId, @Param("category") TemplateCategory category);

    List<PromptTemplate> findByIsSystemTrue();
}

package com.healthdata.ecr.persistence;

import com.healthdata.ecr.persistence.RctcTriggerCodeEntity.RctcTriggerCodeId;
import com.healthdata.ecr.persistence.RctcTriggerCodeEntity.TriggerType;
import com.healthdata.ecr.persistence.RctcTriggerCodeEntity.Urgency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RctcTriggerCodeRepository extends JpaRepository<RctcTriggerCodeEntity, RctcTriggerCodeId> {

    /**
     * Find trigger code by code and system
     */
    Optional<RctcTriggerCodeEntity> findByCodeAndCodeSystem(String code, String codeSystem);

    /**
     * Find all active triggers for a specific code system
     */
    List<RctcTriggerCodeEntity> findByCodeSystemAndIsActiveTrue(String codeSystem);

    /**
     * Find all triggers for a specific condition
     */
    List<RctcTriggerCodeEntity> findByConditionNameAndIsActiveTrue(String conditionName);

    /**
     * Find triggers by type (DIAGNOSIS, LAB_RESULT, etc.)
     */
    List<RctcTriggerCodeEntity> findByTriggerTypeAndIsActiveTrue(TriggerType triggerType);

    /**
     * Check if a code is a reportable trigger
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM RctcTriggerCodeEntity t
        WHERE t.code = :code
        AND t.codeSystem = :codeSystem
        AND t.isActive = true
        """)
    boolean isReportableTrigger(
        @Param("code") String code,
        @Param("codeSystem") String codeSystem);

    /**
     * Find matching triggers for a list of codes (bulk check)
     */
    @Query("""
        SELECT t FROM RctcTriggerCodeEntity t
        WHERE t.code IN :codes
        AND t.codeSystem = :codeSystem
        AND t.isActive = true
        """)
    List<RctcTriggerCodeEntity> findMatchingTriggers(
        @Param("codes") List<String> codes,
        @Param("codeSystem") String codeSystem);

    /**
     * Find all immediate urgency triggers
     */
    List<RctcTriggerCodeEntity> findByUrgencyAndIsActiveTrue(Urgency urgency);

    /**
     * Get all active trigger codes (for caching)
     */
    @Query("""
        SELECT t FROM RctcTriggerCodeEntity t
        WHERE t.isActive = true
        ORDER BY t.conditionName, t.code
        """)
    List<RctcTriggerCodeEntity> findAllActiveTriggers();

    /**
     * Count triggers by condition name
     */
    @Query("""
        SELECT t.conditionName, COUNT(t) FROM RctcTriggerCodeEntity t
        WHERE t.isActive = true
        GROUP BY t.conditionName
        ORDER BY COUNT(t) DESC
        """)
    List<Object[]> countByCondition();

    /**
     * Find by value set OID
     */
    List<RctcTriggerCodeEntity> findByValueSetOidAndIsActiveTrue(String valueSetOid);
}

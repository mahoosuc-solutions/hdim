package com.healthdata.hcc.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiagnosisHccMapRepository extends JpaRepository<DiagnosisHccMapEntity, String> {

    Optional<DiagnosisHccMapEntity> findByIcd10Code(String icd10Code);

    List<DiagnosisHccMapEntity> findByHccCodeV24(String hccCodeV24);

    List<DiagnosisHccMapEntity> findByHccCodeV28(String hccCodeV28);

    @Query("SELECT d FROM DiagnosisHccMapEntity d WHERE d.icd10Code IN :codes")
    List<DiagnosisHccMapEntity> findByIcd10Codes(@Param("codes") List<String> codes);

    @Query("SELECT d FROM DiagnosisHccMapEntity d WHERE d.changedInV28 = true")
    List<DiagnosisHccMapEntity> findCodesChangedInV28();

    @Query("SELECT d FROM DiagnosisHccMapEntity d WHERE d.requiresSpecificity = true")
    List<DiagnosisHccMapEntity> findCodesRequiringSpecificity();
}

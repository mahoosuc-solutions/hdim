package com.healthdata.patient.repository;

import com.healthdata.patient.entity.PatientInsuranceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientInsuranceRepository extends JpaRepository<PatientInsuranceEntity, UUID> {
}

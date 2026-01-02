package com.healthdata.patient.repository;

import com.healthdata.patient.entity.PatientDemographicsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PatientDemographicsRepository extends JpaRepository<PatientDemographicsEntity, UUID> {
}

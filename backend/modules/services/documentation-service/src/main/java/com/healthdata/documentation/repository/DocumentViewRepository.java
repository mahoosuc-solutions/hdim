package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.DocumentViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentViewRepository extends JpaRepository<DocumentViewEntity, Long> {
}

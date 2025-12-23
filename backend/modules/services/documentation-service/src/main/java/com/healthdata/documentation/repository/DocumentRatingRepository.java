package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.DocumentRatingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRatingRepository extends JpaRepository<DocumentRatingEntity, Long> {
}

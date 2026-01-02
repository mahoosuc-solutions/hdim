package com.healthdata.documentation.repository;

import com.healthdata.documentation.persistence.SearchQueryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SearchQueryRepository extends JpaRepository<SearchQueryEntity, Long> {
}

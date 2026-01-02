package com.healthdata.consent.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConsentPolicyRepository extends JpaRepository<ConsentPolicyEntity, UUID> {
}

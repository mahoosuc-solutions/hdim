package com.healthdata.events.repository;

import com.healthdata.events.entity.EventSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscriptionEntity, UUID> {
}

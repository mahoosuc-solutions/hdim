package com.healthdata.sdoh.repository;

import com.healthdata.sdoh.entity.CommunityResourceEntity;
import com.healthdata.sdoh.model.ResourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityResourceRepository extends JpaRepository<CommunityResourceEntity, String> {

    List<CommunityResourceEntity> findByCategory(ResourceCategory category);

    List<CommunityResourceEntity> findByCityAndState(String city, String state);

    List<CommunityResourceEntity> findByZipCode(String zipCode);

    List<CommunityResourceEntity> findByAcceptsWalkIns(boolean acceptsWalkIns);

    List<CommunityResourceEntity> findByRequiresReferral(boolean requiresReferral);

    @Query("SELECT r FROM CommunityResourceEntity r WHERE " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(r.latitude)) * " +
           "cos(radians(r.longitude) - radians(:lon)) + sin(radians(:lat)) * " +
           "sin(radians(r.latitude)))) < :radiusKm")
    List<CommunityResourceEntity> findWithinRadius(
            @Param("lat") double latitude,
            @Param("lon") double longitude,
            @Param("radiusKm") double radiusKm);
}

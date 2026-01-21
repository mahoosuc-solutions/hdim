package com.healthdata.caregap.config;

import com.healthdata.caregap.eventhandler.CareGapEventHandler;
import com.healthdata.caregap.eventhandler.CareGapEventHandler.EventStore;
import com.healthdata.caregap.eventhandler.CareGapEventHandler.CareGapProjectionStore;
import com.healthdata.caregap.persistence.CareGapProjectionRepository;
import com.healthdata.caregap.persistence.PopulationHealthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Event Handler Configuration for Care Gap Service
 */
@Configuration
@RequiredArgsConstructor
public class EventHandlerConfig {

    private final CareGapProjectionRepository gapRepository;
    private final PopulationHealthRepository populationRepository;

    @Bean
    public CareGapEventHandler careGapEventHandler() {
        EventStore eventStore = new InMemoryEventStore();
        CareGapProjectionStore projectionStore = new RepositoryBackedProjectionStore(gapRepository, populationRepository);
        return new CareGapEventHandler(projectionStore, eventStore);
    }

    private static class InMemoryEventStore implements EventStore {
        @Override
        public void storeEvent(Object event) {
            // Event persistence
        }
    }

    private static class RepositoryBackedProjectionStore implements CareGapProjectionStore {
        private final CareGapProjectionRepository gapRepository;
        private final PopulationHealthRepository populationRepository;

        public RepositoryBackedProjectionStore(CareGapProjectionRepository gapRepository,
                                              PopulationHealthRepository populationRepository) {
            this.gapRepository = gapRepository;
            this.populationRepository = populationRepository;
        }

        @Override
        public void saveCareGapProjection(com.healthdata.caregap.projection.CareGapProjection projection) {
            gapRepository.save(projection);
        }

        @Override
        public com.healthdata.caregap.projection.CareGapProjection getCareGapProjection(String patientId, String tenantId, String gapCode) {
            String id = patientId + "_" + tenantId + "_" + gapCode;
            return gapRepository.findById(id).orElse(null);
        }

        @Override
        public void savePopulationHealth(com.healthdata.caregap.projection.PopulationHealthProjection projection) {
            populationRepository.save(projection);
        }

        @Override
        public com.healthdata.caregap.projection.PopulationHealthProjection getPopulationHealth(String tenantId) {
            return populationRepository.findByTenantId(tenantId).orElse(null);
        }
    }
}

package com.healthdata.caregap.config;

import com.healthdata.caregap.eventhandler.CareGapEventHandler;
import com.healthdata.caregap.eventhandler.CareGapEventHandler.EventStore;
import com.healthdata.caregap.eventhandler.CareGapEventHandler.GapProjectionStore;
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
        GapProjectionStore projectionStore = new RepositoryBackedProjectionStore(gapRepository, populationRepository);
        return new CareGapEventHandler(projectionStore, eventStore);
    }

    private static class InMemoryEventStore implements EventStore {
        @Override
        public void storeEvent(Object event) {
            // Event persistence
        }
    }

    private static class RepositoryBackedProjectionStore implements GapProjectionStore {
        private final CareGapProjectionRepository gapRepository;
        private final PopulationHealthRepository populationRepository;

        public RepositoryBackedProjectionStore(CareGapProjectionRepository gapRepository,
                                              PopulationHealthRepository populationRepository) {
            this.gapRepository = gapRepository;
            this.populationRepository = populationRepository;
        }

        @Override
        public void saveGapProjection(Object projection) {
            gapRepository.save((com.healthdata.caregap.projection.CareGapProjection) projection);
        }

        @Override
        public void savePopulationProjection(Object projection) {
            populationRepository.save((com.healthdata.caregap.projection.PopulationHealthProjection) projection);
        }

        @Override
        public Object getGapProjection(String gapId) {
            return gapRepository.findById(gapId).orElse(null);
        }

        @Override
        public Object getPopulationProjection(String tenantId) {
            return populationRepository.findByTenantId(tenantId).orElse(null);
        }
    }
}

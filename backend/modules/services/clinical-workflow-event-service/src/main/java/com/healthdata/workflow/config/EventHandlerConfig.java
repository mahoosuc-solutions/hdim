package com.healthdata.workflow.config;

import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler;
import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler.EventStore;
import com.healthdata.workflow.eventhandler.ClinicalWorkflowEventHandler.WorkflowProjectionStore;
import com.healthdata.workflow.persistence.WorkflowProjectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Event Handler Configuration for Workflow Service
 */
@Configuration
@RequiredArgsConstructor
public class EventHandlerConfig {

    private final WorkflowProjectionRepository workflowRepository;

    @Bean
    public ClinicalWorkflowEventHandler clinicalWorkflowEventHandler() {
        EventStore eventStore = new InMemoryEventStore();
        WorkflowProjectionStore projectionStore = new RepositoryBackedProjectionStore(workflowRepository);
        return new ClinicalWorkflowEventHandler(projectionStore, eventStore);
    }

    private static class InMemoryEventStore implements EventStore {
        @Override
        public void storeEvent(Object event) {
            // Event persistence
        }
    }

    private static class RepositoryBackedProjectionStore implements WorkflowProjectionStore {
        private final WorkflowProjectionRepository repository;

        public RepositoryBackedProjectionStore(WorkflowProjectionRepository repository) {
            this.repository = repository;
        }

        @Override
        public void saveWorkflowProjection(com.healthdata.workflow.projection.WorkflowProjection projection) {
            repository.save(projection);
        }

        @Override
        public com.healthdata.workflow.projection.WorkflowProjection getWorkflowProjection(String patientId, String tenantId, String workflowType) {
            String id = patientId + "_" + tenantId + "_" + workflowType;
            return repository.findById(id).orElse(null);
        }
    }
}

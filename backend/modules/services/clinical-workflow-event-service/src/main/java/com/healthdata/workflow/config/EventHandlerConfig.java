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
        public void saveWorkflowProjection(Object projection) {
            repository.save((com.healthdata.workflow.projection.WorkflowProjection) projection);
        }

        @Override
        public Object getWorkflowProjection(String workflowId) {
            return repository.findById(workflowId).orElse(null);
        }
    }
}

package com.healthdata.demo.application;

import com.healthdata.demo.domain.model.DemoScenario;
import com.healthdata.demo.domain.model.DemoSession;
import com.healthdata.demo.domain.model.DemoSessionProgress;
import com.healthdata.demo.domain.repository.DemoSessionProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for tracking demo session progress.
 */
@Service
@Transactional
public class DemoProgressService {

    public enum Stage {
        INITIALIZING,
        RESETTING,
        GENERATING_PATIENTS,
        PERSISTING_FHIR,
        CREATING_CARE_GAPS,
        SEEDING_MEASURES,
        COMPLETE,
        CANCELLED,
        FAILED
    }

    private final DemoSessionProgressRepository progressRepository;

    public DemoProgressService(DemoSessionProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    public DemoSessionProgress createForSession(DemoSession session, DemoScenario scenario) {
        DemoSessionProgress progress = new DemoSessionProgress();
        progress.setSessionId(session.getId());
        progress.setScenarioName(scenario.getName());
        progress.setTenantId(scenario.getTenantId());
        progress.setStage(Stage.INITIALIZING.name());
        progress.setProgressPercent(5);
        progress.setCancelRequested(false);
        return progressRepository.save(progress);
    }

    public Optional<DemoSessionProgress> getBySessionId(UUID sessionId) {
        return progressRepository.findBySessionId(sessionId);
    }

    public void updateStage(UUID sessionId, Stage stage, int percent, String message) {
        progressRepository.findBySessionId(sessionId).ifPresent(progress -> {
            progress.setStage(stage.name());
            progress.setProgressPercent(percent);
            progress.setMessage(message);
            progressRepository.save(progress);
        });
    }

    public void updateCounts(UUID sessionId,
                             Integer patientsGenerated,
                             Integer patientsPersisted,
                             Integer careGapsCreated,
                             Integer measuresSeeded) {
        progressRepository.findBySessionId(sessionId).ifPresent(progress -> {
            if (patientsGenerated != null) {
                progress.setPatientsGenerated(patientsGenerated);
            }
            if (patientsPersisted != null) {
                progress.setPatientsPersisted(patientsPersisted);
            }
            if (careGapsCreated != null) {
                progress.setCareGapsCreated(careGapsCreated);
            }
            if (measuresSeeded != null) {
                progress.setMeasuresSeeded(measuresSeeded);
            }
            progressRepository.save(progress);
        });
    }

    public void markFailed(UUID sessionId, String message) {
        updateStage(sessionId, Stage.FAILED, 100, message);
    }

    public void markCancelled(UUID sessionId, String message) {
        progressRepository.findBySessionId(sessionId).ifPresent(progress -> {
            progress.setCancelRequested(true);
            progress.setStage(Stage.CANCELLED.name());
            progress.setProgressPercent(100);
            progress.setMessage(message);
            progressRepository.save(progress);
        });
    }

    public void requestCancel(UUID sessionId, String message) {
        progressRepository.findBySessionId(sessionId).ifPresent(progress -> {
            progress.setCancelRequested(true);
            if (message != null && !message.isBlank()) {
                progress.setMessage(message);
            }
            progressRepository.save(progress);
        });
    }

    public boolean isCancelRequested(UUID sessionId) {
        return progressRepository.findBySessionId(sessionId)
            .map(DemoSessionProgress::isCancelRequested)
            .orElse(false);
    }
}

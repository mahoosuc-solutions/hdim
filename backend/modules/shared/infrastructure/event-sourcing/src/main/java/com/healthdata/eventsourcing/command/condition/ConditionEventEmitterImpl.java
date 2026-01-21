package com.healthdata.eventsourcing.command.condition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ConditionEventEmitterImpl implements ConditionEventEmitter {
    @Override
    public Mono<Void> emitConditionDiagnosed(ConditionDiagnosedEvent event) {
        return Mono.fromRunnable(() ->
            log.info("Emitting ConditionDiagnosedEvent for patient: {}, ICD: {}",
                event.getPatientId(), event.getIcdCode())
        );
    }
}

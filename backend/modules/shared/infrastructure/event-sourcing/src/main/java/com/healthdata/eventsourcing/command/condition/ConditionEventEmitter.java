package com.healthdata.eventsourcing.command.condition;

import reactor.core.publisher.Mono;

public interface ConditionEventEmitter {
    Mono<Void> emitConditionDiagnosed(ConditionDiagnosedEvent event);
}

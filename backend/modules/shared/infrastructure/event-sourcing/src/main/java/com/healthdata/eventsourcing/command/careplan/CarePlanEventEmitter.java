package com.healthdata.eventsourcing.command.careplan;

import reactor.core.publisher.Mono;

public interface CarePlanEventEmitter {
    Mono<Void> emitCarePlanCreated(CarePlanCreatedEvent event);
}

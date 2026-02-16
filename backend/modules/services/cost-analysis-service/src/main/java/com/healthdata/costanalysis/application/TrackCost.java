package com.healthdata.costanalysis.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TrackCost {
    String serviceId();
    String metricType() default "execution-time-ms";
    String featureKey() default "";
}
